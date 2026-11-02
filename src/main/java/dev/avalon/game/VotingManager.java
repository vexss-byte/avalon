package dev.avalon.game;

import dev.avalon.net.OpenSelectPayload;
import dev.avalon.net.OpenVotePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Private voting. Votes are stored only on the server and revealed (like the reference video)
 * only AFTER the vote closes. Also handles the "eject a suspect" vote.
 */
public final class VotingManager {
    /** Eligible voters for the current vote. */
    private final Set<UUID> voters = new LinkedHashSet<>();
    private final Map<UUID, Boolean> teamVotes = new LinkedHashMap<>();
    private final Map<UUID, UUID> ejectVotes = new HashMap<>();
    private static final UUID SKIP = new UUID(0L, 0L);

    // ------------------------------------------------------------------ team vote
    public void startTeamVote(GameManager g) {
        voters.clear();
        teamVotes.clear();
        for (ServerPlayerEntity p : g.alivePlayers()) voters.add(p.getUuid());
        List<String> names = new ArrayList<>();
        for (UUID id : g.team) names.add(g.nameOf(id));
        OpenVotePayload payload = new OpenVotePayload(g.nameOf(g.king), g.round + 1,
                g.cfg().voteSeconds, names);
        for (ServerPlayerEntity p : g.alivePlayers()) {
            ServerPlayNetworking.send(p, payload);
            Msg.chat(p, Msg.line("Tim usulan Raja " + g.nameOf(g.king) + ": " + String.join(", ", names), Formatting.YELLOW));
            Msg.chat(p, Text.literal("   ").append(Text.literal("[Buka layar voting]").setStyle(
                    Style.EMPTY.withColor(Formatting.GREEN).withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/avalon menu")))));
        }
    }

    /** @return true if every voter has voted. */
    public boolean castTeamVote(GameManager g, ServerPlayerEntity p, boolean approve) {
        if (g.phase != Phase.VOTING) return false;
        if (!voters.contains(p.getUuid()) || teamVotes.containsKey(p.getUuid())) return false;
        teamVotes.put(p.getUuid(), approve);
        Msg.action(p, "Suaramu tercatat (rahasia sampai voting selesai).", Formatting.GRAY);
        Msg.chatAll(g.alivePlayers(), p.getName().getString() + " telah memberikan suara. ("
                + teamVotes.size() + "/" + voters.size() + ")", Formatting.DARK_GRAY);
        return teamVotes.size() >= voters.size();
    }

    public boolean hasVoted(UUID id) {
        return teamVotes.containsKey(id);
    }

    public int votesCast() {
        return teamVotes.size();
    }

    public int voterCount() {
        return voters.size();
    }

    /** Closes the vote, reveals the result and returns true when the team is approved. */
    public boolean resolveTeamVote(GameManager g) {
        boolean missingReject = g.cfg().missingVoteIsReject;
        int approve = 0;
        int reject = 0;
        List<String> lines = new ArrayList<>();
        for (UUID id : voters) {
            Boolean v = teamVotes.get(id);
            boolean a = v != null ? v : !missingReject;
            if (a) approve++; else reject++;
            lines.add((a ? "Setuju" : "Tolak") + (v == null ? " (tidak memilih)" : "") + " - " + g.nameOf(id));
        }
        for (ServerPlayerEntity p : g.alivePlayers()) {
            Msg.chat(p, Msg.line("HASIL VOTING  Setuju " + approve + " : Tolak " + reject, Formatting.GOLD));
            for (String l : lines) {
                p.sendMessage(Text.literal("  " + l).formatted(l.startsWith("Setuju") ? Formatting.GREEN : Formatting.RED), false);
            }
        }
        return approve > reject;
    }

    // ------------------------------------------------------------------ eject vote
    public void startEjectVote(GameManager g) {
        voters.clear();
        ejectVotes.clear();
        for (ServerPlayerEntity p : g.alivePlayers()) voters.add(p.getUuid());
        List<UUID> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (ServerPlayerEntity p : g.alivePlayers()) {
            ids.add(p.getUuid());
            names.add(p.getName().getString());
        }
        for (ServerPlayerEntity p : g.alivePlayers()) {
            ServerPlayNetworking.send(p, new OpenSelectPayload(OpenSelectPayload.MODE_EJECT,
                    "Tebak Kubu Jahat", "Pilih 1 pemain yang dicurigai - sisa kesempatan: " + g.guessesLeft,
                    1, true, g.cfg().ejectVoteSeconds, ids, names));
        }
    }

    /** @return true when all voters have answered. */
    public boolean castEjectVote(GameManager g, ServerPlayerEntity p, List<UUID> chosen) {
        if (g.phase != Phase.EJECT_VOTE) return false;
        if (!voters.contains(p.getUuid()) || ejectVotes.containsKey(p.getUuid())) return false;
        UUID target = SKIP;
        if (!chosen.isEmpty()) {
            UUID c = chosen.get(0);
            if (g.roles.containsKey(c) && !g.ejected.contains(c)) target = c;
        }
        ejectVotes.put(p.getUuid(), target);
        Msg.chatAll(g.alivePlayers(), p.getName().getString() + " telah memberikan suara. ("
                + ejectVotes.size() + "/" + voters.size() + ")", Formatting.DARK_GRAY);
        return ejectVotes.size() >= voters.size();
    }

    /** Plurality winner, or null on a tie / all skipped. */
    public UUID resolveEjectVote(GameManager g) {
        Map<UUID, Integer> tally = new HashMap<>();
        for (UUID target : ejectVotes.values()) {
            if (!SKIP.equals(target)) tally.merge(target, 1, Integer::sum);
        }
        UUID best = null;
        int bestCount = 0;
        boolean tie = false;
        for (var e : tally.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
                tie = false;
            } else if (e.getValue() == bestCount) {
                tie = true;
            }
        }
        if (tie || best == null) return null;
        // must be at least a strict plurality of ALL voters' non-skip votes; ties handled above
        return best;
    }
}
