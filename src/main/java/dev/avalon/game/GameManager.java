package dev.avalon.game;

import dev.avalon.AvalonConfig;
import dev.avalon.AvalonMod;
import dev.avalon.block.AvalonBlocks;
import dev.avalon.item.AvalonItems;
import dev.avalon.net.CloseScreenPayload;
import dev.avalon.net.OpenSelectPayload;
import dev.avalon.net.OpenVotePayload;
import dev.avalon.net.SelectResultPayload;
import dev.avalon.world.Arena;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The server-side state machine. The SERVER is the single source of truth: roles, factions,
 * mission results, sabotage state and the winner never come from a client.
 */
public final class GameManager {
    private static GameManager current;

    public static GameManager get() {
        return current;
    }

    public static void init(MinecraftServer server) {
        current = new GameManager(server);
    }

    public static void shutdown() {
        if (current != null) current.forceStop();
        current = null;
    }

    public final MinecraftServer server;
    public final Random random = new Random();
    public final RoleManager roleManager = new RoleManager();
    public final VotingManager voting = new VotingManager();
    public final MissionManager mission = new MissionManager();
    public final SabotageManager sabotage = new SabotageManager();
    public final PillarController pillars = new PillarController();
    public final PortalController portal = new PortalController();

    public Phase phase = Phase.LOBBY;
    public int ticksLeft;
    public final List<UUID> order = new ArrayList<>();
    public final Map<UUID, Role> roles = new HashMap<>();
    public final Set<UUID> ejected = new HashSet<>();
    private final Map<UUID, String> names = new HashMap<>();

    public int round;
    private int kingIdx = -1;
    public UUID king;
    public final List<UUID> team = new ArrayList<>();
    private int rejections;
    private boolean anySabotage;
    private boolean lastSabotaged;
    public int guessesLeft;
    public UUID ladyHolder;
    private final Set<UUID> ladyChecked = new HashSet<>();
    private final Set<UUID> skipVotes = new HashSet<>();
    private final boolean[] results = new boolean[3];
    private ServerBossBar bar;
    private long tickCounter;
    private boolean portalTestOnly;

    private GameManager(MinecraftServer server) {
        this.server = server;
    }

    // ------------------------------------------------------------------ helpers
    public AvalonConfig cfg() {
        return AvalonConfig.get();
    }

    public ServerPlayerEntity player(UUID id) {
        return id == null ? null : server.getPlayerManager().getPlayer(id);
    }

    public String nameOf(UUID id) {
        return names.getOrDefault(id, "?");
    }

    public List<ServerPlayerEntity> alivePlayers() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        for (UUID id : order) {
            if (ejected.contains(id)) continue;
            ServerPlayerEntity p = player(id);
            if (p != null) list.add(p);
        }
        return list;
    }

    private List<ServerPlayerEntity> allPlayers() {
        List<ServerPlayerEntity> list = new ArrayList<>();
        for (UUID id : order) {
            ServerPlayerEntity p = player(id);
            if (p != null) list.add(p);
        }
        return list;
    }

    private boolean isAlive(UUID id) {
        return id != null && roles.containsKey(id) && !ejected.contains(id) && player(id) != null;
    }

    private void closeScreens() {
        for (ServerPlayerEntity p : allPlayers()) ServerPlayNetworking.send(p, CloseScreenPayload.INSTANCE);
    }

    private void removeItems(net.minecraft.item.Item item) {
        for (ServerPlayerEntity p : allPlayers()) {
            for (int i = 0; i < p.getInventory().size(); i++) {
                if (p.getInventory().getStack(i).isOf(item)) p.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private int teamSize() {
        int[] sizes = cfg().teamSizes;
        int s = sizes[Math.min(round, sizes.length - 1)];
        return Math.max(1, Math.min(s, alivePlayers().size()));
    }

    // ------------------------------------------------------------------ start / stop
    /** @return an error message, or null when the game started. */
    public String startGame() {
        if (phase != Phase.LOBBY) return "Game sedang berjalan.";
        List<ServerPlayerEntity> ps = new ArrayList<>(server.getPlayerManager().getPlayerList());
        int minNeeded = cfg().soloTest ? 1 : Math.max(2, cfg().minPlayers);
        if (ps.size() < minNeeded)
            return "Pemain kurang. Minimal " + minNeeded + " (atur minPlayers / soloTest di config/avalon.json).";
        if (ps.size() > cfg().maxPlayers) return "Pemain terlalu banyak. Maksimal " + cfg().maxPlayers + ".";
        ServerWorld arena = Arena.world(server);
        if (arena == null) return "Dimensi avalon:arena tidak ditemukan (apakah mod terpasang di server?).";
        if (!arena.getBlockState(Arena.sockets(0)[0]).isOf(AvalonBlocks.BATTERY_SOCKET))
            return "Map belum dibangun. Jalankan /avalon build dulu.";

        resetState();
        Collections.shuffle(ps, random);
        for (ServerPlayerEntity p : ps) {
            order.add(p.getUuid());
            names.put(p.getUuid(), p.getName().getString());
        }
        roleManager.assign(this, order);
        guessesLeft = roleManager.evilTotal(this);
        if (cfg().ladyOfTheLakeEnabled) {
            ladyHolder = order.get(random.nextInt(order.size()));
            ladyChecked.add(ladyHolder);
        }
        for (ServerPlayerEntity p : ps) {
            p.getInventory().clear();
            p.changeGameMode(GameMode.ADVENTURE);
            p.setHealth(p.getMaxHealth());
            p.getHungerManager().setFoodLevel(20);
        }
        bar = new ServerBossBar(Text.literal("Avalon"), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        phase = Phase.PORTAL_INTRO;
        portal.begin(this, Arena.lobbyWorld(server), Arena.lobbyPos());
        AvalonMod.LOGGER.info("Avalon game started with {} players", order.size());
        return null;
    }

    private void resetState() {
        portalTestOnly = false;
        order.clear();
        roles.clear();
        ejected.clear();
        names.clear();
        team.clear();
        ladyChecked.clear();
        skipVotes.clear();
        sabotage.resetAll();
        round = 0;
        kingIdx = -1;
        king = null;
        rejections = 0;
        anySabotage = false;
        lastSabotaged = false;
        ladyHolder = null;
        ticksLeft = 0;
        for (int i = 0; i < results.length; i++) results[i] = false;
    }

    /** Abort/cleanup (command or server stopping). Returns everybody to the lobby. */
    public void forceStop() {
        if (phase == Phase.LOBBY) return;
        cleanup();
    }

    private void cleanup() {
        ServerWorld lobby = Arena.lobbyWorld(server);
        Vec3d lp = Arena.lobbyPos();
        portal.stopAnimations(this);
        portal.cancel();
        portal.removeFx(server, lobby);
        removeItems(AvalonItems.BATTERY);
        removeItems(AvalonItems.TEAM_BOOK);
        removeItems(AvalonItems.LADY_ORB);
        closeScreens();
        for (ServerPlayerEntity p : allPlayers()) {
            p.setNoGravity(false);
            p.changeGameMode(GameMode.SURVIVAL);
            p.teleport(lobby, lp.x, lp.y, lp.z, 0f, 0f);
        }
        if (bar != null) {
            bar.clearPlayers();
            bar = null;
        }
        phase = Phase.LOBBY;
        ticksLeft = 0;
        resetState();
    }

    // ------------------------------------------------------------------ tick
    public void tick() {
        if (phase == Phase.LOBBY) return;
        tickCounter++;

        switch (phase) {
            case PORTAL_INTRO -> {
                if (portal.tick(this)) onPortalDone();
            }
            case ACTIVATION -> {
                PillarController.Result r = pillars.tick(this);
                if (r != PillarController.Result.RUNNING) onActivationDone(r == PillarController.Result.SUCCESS);
            }
            default -> {
                if (ticksLeft > 0 && --ticksLeft == 0) onTimeout();
            }
        }
        if (phase == Phase.LOBBY) return;

        if (tickCounter % 20 == 0) {
            if (phase == Phase.MISSION) sabotage.tickHints(this);
            voidSafety();
        }
        if (tickCounter % 10 == 0) updateBar();
    }

    private void voidSafety() {
        if (phase == Phase.PORTAL_INTRO || phase == Phase.ENDED) return;
        ServerWorld arena = Arena.world(server);
        List<ServerPlayerEntity> list = allPlayers();
        int n = order.size();
        for (ServerPlayerEntity p : list) {
            if (p.getServerWorld() == arena && p.getY() < Arena.floorY() - 15 && !ejected.contains(p.getUuid())) {
                int i = Math.max(0, order.indexOf(p.getUuid()));
                Vec3d s = Arena.hallSeat(i, n);
                p.teleport(arena, s.x, s.y, s.z, p.getYaw(), 0f);
            }
        }
    }

    private void updateBar() {
        if (bar == null) return;
        for (ServerPlayerEntity p : allPlayers()) bar.addPlayer(p);
        String label;
        int total;
        switch (phase) {
            case PORTAL_INTRO -> {
                label = "Gerbang terbuka...";
                total = 0;
            }
            case ROLE_INTRO -> {
                label = "Fase perkenalan";
                total = cfg().roleIntroSeconds * 20;
            }
            case TEAM_SELECT -> {
                label = "Raja " + nameOf(king) + " memilih tim Pilar " + (round + 1);
                total = cfg().teamSelectSeconds * 20;
            }
            case VOTING -> {
                label = "Voting tim (" + voting.votesCast() + "/" + voting.voterCount() + ")";
                total = cfg().voteSeconds * 20;
            }
            case MISSION -> {
                label = "Misi Pilar " + (round + 1) + "  Baterai " + mission.installedCount() + "/4";
                total = cfg().missionSeconds * 20;
            }
            case ACTIVATION -> {
                label = "Pilar " + (round + 1) + " sedang diaktifkan...";
                total = 0;
            }
            case DISCUSSION -> {
                label = "Diskusi  Skip " + skipVotes.size() + "/" + alivePlayers().size();
                total = cfg().discussionSeconds * 20;
            }
            case EJECT_VOTE -> {
                label = "Tebak kubu jahat  Kesempatan " + guessesLeft;
                total = cfg().ejectVoteSeconds * 20;
            }
            case ASSASSIN -> {
                label = "Kubu jahat berdiskusi - Assassin mencari Merlin";
                total = cfg().assassinSeconds * 20;
            }
            default -> {
                label = "Permainan selesai";
                total = cfg().endScreenSeconds * 20;
            }
        }
        int secs = Math.max(0, ticksLeft / 20);
        String time = total > 0 ? String.format("  %d:%02d", secs / 60, secs % 60) : "";
        bar.setName(Text.literal(label + time).formatted(Formatting.LIGHT_PURPLE));
        bar.setPercent(total > 0 ? Math.max(0f, Math.min(1f, ticksLeft / (float) total)) : 1f);
    }

    private void onTimeout() {
        switch (phase) {
            case ROLE_INTRO -> enterKingSelect();
            case TEAM_SELECT -> {
                autoTeam();
                startVote();
            }
            case VOTING -> resolveVote();
            case MISSION -> missionTimeout();
            case DISCUSSION -> afterDiscussion();
            case EJECT_VOTE -> resolveEject();
            case ASSASSIN -> endGame(true, "Assassin kehabisan waktu - Merlin selamat.");
            case ENDED -> cleanup();
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ phases
    private void onPortalDone() {
        mission.returnAllToHall(this);
        if (portalTestOnly) {
            // Portal test: show the Hall for 5 seconds, then everything is cleaned up.
            phase = Phase.ENDED;
            ticksLeft = 100;
            return;
        }
        enterRoleIntro();
    }

    /** Plays only the intro portal (gate + pull animation) for one player. No roles, no game. */
    public String startPortalTest(ServerPlayerEntity p) {
        if (phase != Phase.LOBBY) return "Game sedang berjalan.";
        ServerWorld arena = Arena.world(server);
        if (arena == null) return "Dimensi avalon:arena tidak ditemukan.";
        resetState();
        order.add(p.getUuid());
        names.put(p.getUuid(), p.getName().getString());
        portalTestOnly = true;
        p.getInventory().clear();
        p.changeGameMode(GameMode.ADVENTURE);
        phase = Phase.PORTAL_INTRO;
        portal.begin(this, Arena.lobbyWorld(server), Arena.lobbyPos());
        return null;
    }

    private void enterRoleIntro() {
        phase = Phase.ROLE_INTRO;
        ticksLeft = cfg().roleIntroSeconds * 20;
        roleManager.sendIntro(this);
    }

    private void advanceKing() {
        int n = order.size();
        if (kingIdx < 0) kingIdx = random.nextInt(n) - 1;
        for (int i = 0; i < n; i++) {
            kingIdx = (kingIdx + 1) % n;
            if (isAlive(order.get(kingIdx))) {
                king = order.get(kingIdx);
                return;
            }
        }
    }

    private void enterKingSelect() {
        advanceKing();
        team.clear();
        removeItems(AvalonItems.TEAM_BOOK);
        phase = Phase.TEAM_SELECT;
        ticksLeft = cfg().teamSelectSeconds * 20;
        int size = teamSize();
        ServerPlayerEntity k = player(king);
        for (ServerPlayerEntity p : alivePlayers()) {
            Msg.title(p, "RAJA TELAH DIPILIH", Formatting.GOLD,
                    nameOf(king) + " adalah Raja Misi " + (round + 1), Formatting.YELLOW, 10, 60, 15);
            Msg.chat(p, "Raja Misi " + (round + 1) + ": " + nameOf(king) + " - tim terdiri dari " + size + " orang.", Formatting.GOLD);
        }
        if (k != null) {
            k.getInventory().setStack(4, new ItemStack(AvalonItems.TEAM_BOOK));
            k.getInventory().selectedSlot = 4;
            Msg.title(k, "KAMU ADALAH RAJA", Formatting.GOLD,
                    "Klik kanan Buku Pemilihan Tim untuk memilih " + size + " anggota", Formatting.YELLOW, 10, 80, 15);
        }
    }

    public void onTeamBookUse(ServerPlayerEntity p) {
        if (phase != Phase.TEAM_SELECT || !p.getUuid().equals(king)) return;
        List<UUID> ids = new ArrayList<>();
        List<String> ns = new ArrayList<>();
        for (ServerPlayerEntity a : alivePlayers()) {
            ids.add(a.getUuid());
            ns.add(a.getName().getString());
        }
        ServerPlayNetworking.send(p, new OpenSelectPayload(OpenSelectPayload.MODE_TEAM,
                "Pilih Tim Misi Pilar " + (round + 1), "Pilih tepat " + teamSize() + " anggota",
                teamSize(), false, Math.max(1, ticksLeft / 20), ids, ns));
    }

    private void autoTeam() {
        List<UUID> pool = new ArrayList<>();
        for (ServerPlayerEntity p : alivePlayers()) pool.add(p.getUuid());
        Collections.shuffle(pool, random);
        team.clear();
        team.addAll(pool.subList(0, Math.min(teamSize(), pool.size())));
        Msg.chatAll(alivePlayers(), "Waktu habis - tim dipilih otomatis.", Formatting.GRAY);
    }

    private void startVote() {
        removeItems(AvalonItems.TEAM_BOOK);
        closeScreens();
        phase = Phase.VOTING;
        ticksLeft = cfg().voteSeconds * 20;
        voting.startTeamVote(this);
    }

    private void resolveVote() {
        boolean ok = voting.resolveTeamVote(this);
        closeScreens();
        if (ok) {
            rejections = 0;
            Msg.chatAll(alivePlayers(), "Tim disetujui! Memulai fase misi.", Formatting.GREEN);
            startMission();
        } else {
            rejections++;
            Msg.chatAll(alivePlayers(), "Tim ditolak (" + rejections + "/" + cfg().maxRejections + ").", Formatting.RED);
            if (rejections >= cfg().maxRejections) {
                endGame(false, "Terlalu banyak proposal ditolak - kekacauan menguasai Avalon.");
            } else {
                enterKingSelect();
            }
        }
    }

    private void startMission() {
        phase = Phase.MISSION;
        ticksLeft = cfg().missionSeconds * 20;
        mission.start(this);
    }

    /** Called by MissionManager when the 4th battery is inserted. */
    public void beginActivation() {
        if (phase != Phase.MISSION) return;
        phase = Phase.ACTIVATION;
        ticksLeft = 0;
        pillars.begin(round);
        Msg.titleAll(alivePlayers(), "AKTIVASI PILAR " + (round + 1), Formatting.LIGHT_PURPLE,
                "Energi mengalir...", Formatting.GRAY, 10, 60, 10);
    }

    private void onActivationDone(boolean success) {
        results[round] = success;
        boolean anySab = sabotage.count() > 0;
        if (anySab) anySabotage = true;
        lastSabotaged = anySab;
        if (success) {
            Msg.titleAll(allPlayers(), "PILAR " + (round + 1) + " BERHASIL", Formatting.AQUA,
                    "Energi stabil", Formatting.GRAY, 10, 60, 20);
            Msg.chatAll(allPlayers(), "Pilar " + (round + 1) + " berhasil diaktifkan.", Formatting.AQUA);
        } else {
            Msg.titleAll(allPlayers(), "PILAR " + (round + 1) + " GAGAL", Formatting.RED,
                    "Energi tidak stabil!", Formatting.DARK_RED, 10, 60, 20);
            Msg.chatAll(allPlayers(), "Pilar " + (round + 1) + " meledak - ada sabotase!", Formatting.RED);
        }
        finishMission();
    }

    private void missionTimeout() {
        results[round] = false;
        lastSabotaged = false;
        Msg.chatAll(allPlayers(), "Waktu misi habis - Pilar " + (round + 1) + " gagal.", Formatting.RED);
        finishMission();
    }

    private void finishMission() {
        mission.clearBatteries(this);
        mission.returnAllToHall(this);
        enterDiscussion();
    }

    private void enterDiscussion() {
        phase = Phase.DISCUSSION;
        ticksLeft = cfg().discussionSeconds * 20;
        skipVotes.clear();
        removeItems(AvalonItems.TEAM_BOOK);
        Msg.titleAll(alivePlayers(), "DISKUSI", Formatting.YELLOW,
                "Duduk dan berdiskusi", Formatting.GRAY, 10, 50, 15);
        Text skip = Text.literal("[Skip diskusi]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/avalon skip")));
        for (ServerPlayerEntity p : alivePlayers()) {
            Msg.chat(p, Text.literal("Diskusi dimulai. ").formatted(Formatting.YELLOW).append(skip));
        }
        if (cfg().ladyOfTheLakeEnabled && round < 2 && isAlive(ladyHolder)) {
            ServerPlayerEntity lady = player(ladyHolder);
            lady.giveItemStack(new ItemStack(AvalonItems.LADY_ORB));
            Msg.title(lady, "LADY OF THE LAKE", Formatting.AQUA, "Klik kanan untuk memeriksa satu pemain",
                    Formatting.GRAY, 10, 60, 15);
        }
    }

    public void handleSkip(ServerPlayerEntity p) {
        if (phase != Phase.DISCUSSION || !isAlive(p.getUuid())) return;
        if (!skipVotes.add(p.getUuid())) return;
        int alive = alivePlayers().size();
        Msg.chatAll(alivePlayers(), p.getName().getString() + " ingin skip diskusi (" + skipVotes.size() + "/" + alive + ")",
                Formatting.DARK_GRAY);
        if (skipVotes.size() > alive / 2) afterDiscussion();
    }

    public void onLadyUse(ServerPlayerEntity p) {
        if (phase != Phase.DISCUSSION || !p.getUuid().equals(ladyHolder)) return;
        List<UUID> ids = new ArrayList<>();
        List<String> ns = new ArrayList<>();
        for (ServerPlayerEntity a : alivePlayers()) {
            if (ladyChecked.contains(a.getUuid())) continue;
            ids.add(a.getUuid());
            ns.add(a.getName().getString());
        }
        if (ids.isEmpty()) {
            Msg.chat(p, "Tidak ada pemain yang bisa diperiksa.", Formatting.GRAY);
            return;
        }
        ServerPlayNetworking.send(p, new OpenSelectPayload(OpenSelectPayload.MODE_LADY, "Lady of the Lake",
                "Pilih pemain yang kamu periksa (hanya kubunya yang terlihat)", 1, false,
                Math.max(1, ticksLeft / 20), ids, ns));
    }

    private void afterDiscussion() {
        removeItems(AvalonItems.LADY_ORB);
        closeScreens();
        boolean canGuess = guessesLeft > 0 && (cfg().ejectAfterEveryMission || lastSabotaged);
        if (canGuess && alivePlayers().size() > 2) {
            phase = Phase.EJECT_VOTE;
            ticksLeft = cfg().ejectVoteSeconds * 20;
            voting.startEjectVote(this);
        } else {
            afterRound();
        }
    }

    private void resolveEject() {
        UUID target = voting.resolveEjectVote(this);
        closeScreens();
        if (target == null) {
            Msg.chatAll(allPlayers(), "Tidak ada yang dikeluarkan (seri / semua skip).", Formatting.GRAY);
        } else {
            guessesLeft--;
            Role r = roles.get(target);
            ejected.add(target);
            ServerPlayerEntity ep = player(target);
            if (ep != null) ep.changeGameMode(GameMode.SPECTATOR);
            Msg.titleAll(allPlayers(), nameOf(target) + " DIKELUARKAN", r.good ? Formatting.AQUA : Formatting.RED,
                    r.good ? "Dia ternyata KUBU BAIK!" : "Dia memang KUBU JAHAT!", Formatting.GRAY, 10, 70, 20);
            Msg.chatAll(allPlayers(), nameOf(target) + " dikeluarkan - " + (r.good ? "KUBU BAIK" : "KUBU JAHAT")
                    + ". Sisa kesempatan menebak: " + guessesLeft, r.good ? Formatting.AQUA : Formatting.RED);
            if (roleManager.allEvilEjected(this)) {
                endGame(true, "Seluruh kubu jahat berhasil dikeluarkan!");
                return;
            }
        }
        afterRound();
    }

    private void afterRound() {
        if (round >= 2) {
            finalizeGame();
        } else {
            round++;
            enterKingSelect();
        }
    }

    private void finalizeGame() {
        if (!anySabotage) {
            endGame(true, "Ketiga pilar aktif tanpa sabotase.");
        } else if (roleManager.allEvilEjected(this)) {
            endGame(true, "Seluruh kubu jahat berhasil dikeluarkan!");
        } else if (cfg().assassinEnabled && roleManager.assassin(this) != null) {
            enterAssassin();
        } else {
            endGame(false, "Sabotase terjadi dan kubu jahat tidak pernah ditemukan.");
        }
    }

    private void enterAssassin() {
        phase = Phase.ASSASSIN;
        ticksLeft = cfg().assassinSeconds * 20;
        Msg.titleAll(allPlayers(), "KUBU JAHAT BERDISKUSI", Formatting.DARK_RED,
                "Assassin sedang mencari Merlin...", Formatting.GRAY, 10, 80, 20);
        UUID a = roleManager.assassin(this);
        ServerPlayerEntity ap = player(a);
        if (ap == null) return;
        List<UUID> ids = new ArrayList<>();
        List<String> ns = new ArrayList<>();
        for (ServerPlayerEntity p : alivePlayers()) {
            Role r = roles.get(p.getUuid());
            if (r != null && r.evil()) continue;      // the assassin already knows his team
            ids.add(p.getUuid());
            ns.add(p.getName().getString());
        }
        if (ids.isEmpty()) {
            endGame(true, "(Tes solo) tidak ada kandidat Merlin untuk ditebak.");
            return;
        }
        ServerPlayNetworking.send(ap, new OpenSelectPayload(OpenSelectPayload.MODE_ASSASSIN, "Tebak Merlin",
                "Satu kesempatan. Jika salah, kubu baik menang.", 1, false, cfg().assassinSeconds, ids, ns));
        Msg.chat(ap, "Kamu adalah Assassin sekarang. Diskusikan dengan timmu, lalu pilih Merlin.", Formatting.DARK_RED);
    }

    // ------------------------------------------------------------------ end
    private void endGame(boolean goodWins, String reason) {
        closeScreens();
        mission.clearBatteries(this);
        removeItems(AvalonItems.TEAM_BOOK);
        removeItems(AvalonItems.LADY_ORB);
        phase = Phase.ENDED;
        ticksLeft = cfg().endScreenSeconds * 20;
        Msg.titleAll(allPlayers(), goodWins ? "KUBU BAIK MENANG" : "KUBU JAHAT MENANG",
                goodWins ? Formatting.AQUA : Formatting.RED, reason, Formatting.GRAY, 10, 120, 30);
        List<ServerPlayerEntity> ps = allPlayers();
        Msg.chatAll(ps, reason, goodWins ? Formatting.AQUA : Formatting.RED);
        Msg.chatAll(ps, "=== PERAN SEMUA PEMAIN ===", Formatting.GOLD);
        for (UUID id : order) {
            Role r = roles.get(id);
            for (ServerPlayerEntity p : ps) {
                p.sendMessage(Text.literal("  " + nameOf(id) + " - " + r.display)
                        .formatted(r.good ? Formatting.AQUA : Formatting.RED), false);
            }
        }
        ServerWorld arena = Arena.world(server);
        Vec3d c = Arena.hallCenterVec();
        for (ServerPlayerEntity p : ps) {
            p.changeGameMode(ejected.contains(p.getUuid()) ? GameMode.SPECTATOR : GameMode.ADVENTURE);
            if (p.getServerWorld() != arena) p.teleport(arena, c.x, c.y + 1, c.z, 0f, 0f);
        }
    }

    // ------------------------------------------------------------------ network handlers
    public void handleVote(ServerPlayerEntity p, boolean approve) {
        if (phase != Phase.VOTING) return;
        if (voting.castTeamVote(this, p, approve)) resolveVote();
    }

    public void handleSelect(ServerPlayerEntity p, SelectResultPayload payload) {
        if (phase == Phase.LOBBY) return;
        switch (payload.mode) {
            case OpenSelectPayload.MODE_TEAM -> handleTeamChoice(p, payload.chosen);
            case OpenSelectPayload.MODE_EJECT -> {
                if (voting.castEjectVote(this, p, payload.chosen)) resolveEject();
            }
            case OpenSelectPayload.MODE_LADY -> handleLady(p, payload.chosen);
            case OpenSelectPayload.MODE_ASSASSIN -> handleAssassin(p, payload.chosen);
            default -> {
            }
        }
    }

    private void handleTeamChoice(ServerPlayerEntity p, List<UUID> chosen) {
        if (phase != Phase.TEAM_SELECT || !p.getUuid().equals(king)) return;
        Set<UUID> unique = new HashSet<>(chosen);
        if (unique.size() != chosen.size() || chosen.size() != teamSize()) {
            Msg.chat(p, "Jumlah anggota harus tepat " + teamSize() + ".", Formatting.RED);
            return;
        }
        for (UUID id : chosen) {
            if (!isAlive(id)) {
                Msg.chat(p, "Ada pemain yang tidak valid.", Formatting.RED);
                return;
            }
        }
        team.clear();
        team.addAll(chosen);
        startVote();
    }

    private void handleLady(ServerPlayerEntity p, List<UUID> chosen) {
        if (phase != Phase.DISCUSSION || !p.getUuid().equals(ladyHolder) || chosen.size() != 1) return;
        UUID target = chosen.get(0);
        if (!isAlive(target) || ladyChecked.contains(target)) return;
        boolean hasItem = false;
        for (int i = 0; i < p.getInventory().size(); i++) {
            if (p.getInventory().getStack(i).isOf(AvalonItems.LADY_ORB)) hasItem = true;
        }
        if (!hasItem) return;
        Role r = roles.get(target);
        removeItems(AvalonItems.LADY_ORB);
        ladyChecked.add(target);
        ladyHolder = target;
        closeScreens();
        // PRIVATE: only the Lady learns the faction. Nothing is broadcast, no particles, no sound.
        Msg.chat(p, "Lady of the Lake: " + nameOf(target) + " adalah " + (r.good ? "KUBU BAIK" : "KUBU JAHAT") + ".",
                r.good ? Formatting.AQUA : Formatting.RED);
        Msg.title(p, r.good ? "KUBU BAIK" : "KUBU JAHAT", r.good ? Formatting.AQUA : Formatting.RED,
                nameOf(target), Formatting.GRAY, 5, 50, 15);
        Msg.chatAll(alivePlayers(), "Lady of the Lake telah memeriksa seorang pemain.", Formatting.DARK_GRAY);
    }

    private void handleAssassin(ServerPlayerEntity p, List<UUID> chosen) {
        if (phase != Phase.ASSASSIN || !p.getUuid().equals(roleManager.assassin(this)) || chosen.size() != 1) return;
        UUID target = chosen.get(0);
        if (!isAlive(target)) return;
        Role tr = roles.get(target);
        if (tr != null && tr.evil()) return;
        UUID merlin = roleManager.merlin(this);
        closeScreens();
        if (target.equals(merlin)) {
            endGame(false, "Assassin menemukan Merlin: " + nameOf(target) + "!");
        } else {
            endGame(true, "Assassin salah menebak (" + nameOf(target) + ") - Merlin selamat!");
        }
    }

    /** Re-send the screen that is currently pending for this player (/avalon menu). */
    public void reopenFor(ServerPlayerEntity p) {
        switch (phase) {
            case VOTING -> {
                if (!isAlive(p.getUuid()) || voting.hasVoted(p.getUuid())) return;
                List<String> ns = new ArrayList<>();
                for (UUID id : team) ns.add(nameOf(id));
                ServerPlayNetworking.send(p, new OpenVotePayload(nameOf(king), round + 1,
                        Math.max(1, ticksLeft / 20), ns));
            }
            case TEAM_SELECT -> onTeamBookUse(p);
            case DISCUSSION -> onLadyUse(p);
            case EJECT_VOTE -> {
                if (!isAlive(p.getUuid())) return;
                List<UUID> ids = new ArrayList<>();
                List<String> ns = new ArrayList<>();
                for (ServerPlayerEntity a : alivePlayers()) {
                    ids.add(a.getUuid());
                    ns.add(a.getName().getString());
                }
                ServerPlayNetworking.send(p, new OpenSelectPayload(OpenSelectPayload.MODE_EJECT, "Tebak Kubu Jahat",
                        "Pilih 1 pemain yang dicurigai - sisa kesempatan: " + guessesLeft, 1, true,
                        Math.max(1, ticksLeft / 20), ids, ns));
            }
            default -> {
            }
        }
    }

    public String status() {
        return "Phase=" + phase + " round=" + (round + 1) + " players=" + order.size() + " alive=" + alivePlayers().size()
                + " ticksLeft=" + ticksLeft + " guessesLeft=" + guessesLeft;
    }
}
