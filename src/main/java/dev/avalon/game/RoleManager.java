package dev.avalon.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Role assignment and the (private) role introduction. Roles never leave the server except as private text. */
public final class RoleManager {
    private static final Role[] GOOD_ORDER = {
            Role.MERLIN, Role.PERCIVAL, Role.KINGS_DAUGHTER,
            Role.LOYAL_SERVANT, Role.LOYAL_SERVANT, Role.LOYAL_SERVANT
    };
    private static final Role[] EVIL_ORDER = {Role.ASSASSIN, Role.MORGANA, Role.MORDRED, Role.MINION};

    public static int evilCount(int n) {
        if (n >= 10) return 4;
        if (n >= 7) return 3;
        if (n >= 5) return 2;
        return 1;
    }

    public void assign(GameManager g, List<UUID> players) {
        g.roles.clear();
        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, g.random);
        int n = shuffled.size();
        int evil = Math.min(evilCount(n), n - 1);
        int good = n - evil;
        int idx = 0;
        for (int i = 0; i < good; i++) {
            g.roles.put(shuffled.get(idx++), GOOD_ORDER[Math.min(i, GOOD_ORDER.length - 1)]);
        }
        for (int i = 0; i < evil; i++) {
            g.roles.put(shuffled.get(idx++), EVIL_ORDER[Math.min(i, EVIL_ORDER.length - 1)]);
        }
    }

    public int evilTotal(GameManager g) {
        int c = 0;
        for (Role r : g.roles.values()) if (r.evil()) c++;
        return c;
    }

    public UUID merlin(GameManager g) {
        for (var e : g.roles.entrySet()) if (e.getValue() == Role.MERLIN) return e.getKey();
        return null;
    }

    /** The Assassin, or (if ejected) any other living EVIL player. */
    public UUID assassin(GameManager g) {
        for (var e : g.roles.entrySet()) {
            if (e.getValue() == Role.ASSASSIN && !g.ejected.contains(e.getKey())) return e.getKey();
        }
        for (var e : g.roles.entrySet()) {
            if (e.getValue().evil() && !g.ejected.contains(e.getKey())) return e.getKey();
        }
        return null;
    }

    public boolean allEvilEjected(GameManager g) {
        for (var e : g.roles.entrySet()) {
            if (e.getValue().evil() && !g.ejected.contains(e.getKey())) return false;
        }
        return true;
    }

    /** Sends each player ONLY their own secret information (chat + title). */
    public void sendIntro(GameManager g) {
        for (UUID id : g.order) {
            ServerPlayerEntity p = g.player(id);
            Role role = g.roles.get(id);
            if (p == null || role == null) continue;
            Formatting color = role.good ? Formatting.AQUA : Formatting.RED;
            Msg.title(p, role.display.toUpperCase(), color,
                    role.good ? "Kamu berada di KUBU BAIK" : "Kamu berada di KUBU JAHAT",
                    Formatting.GRAY, 10, 80, 20);
            Msg.chat(p, "Peranmu: " + role.display + (role.good ? " (Kubu Baik)" : " (Kubu Jahat)"), color);
            switch (role) {
                case MERLIN -> {
                    List<String> names = new ArrayList<>();
                    for (UUID o : g.order) {
                        Role r = g.roles.get(o);
                        if (r != null && r.evil() && r != Role.MORDRED) names.add(g.nameOf(o));
                    }
                    Collections.shuffle(names, g.random);
                    Msg.chat(p, "Kamu melihat kubu jahat (kecuali Mordred): " + String.join(", ", names), Formatting.RED);
                    Msg.chat(p, "Jangan ketahuan Assassin!", Formatting.GRAY);
                }
                case PERCIVAL -> {
                    List<String> names = new ArrayList<>();
                    for (UUID o : g.order) {
                        Role r = g.roles.get(o);
                        if (r == Role.MERLIN || r == Role.MORGANA) names.add(g.nameOf(o));
                    }
                    Collections.shuffle(names, g.random);
                    Msg.chat(p, "Salah satu dari mereka adalah Merlin, yang lain Morgana: "
                            + String.join(", ", names), Formatting.YELLOW);
                }
                case KINGS_DAUGHTER, LOYAL_SERVANT ->
                        Msg.chat(p, "Kamu tidak punya informasi rahasia. Percayalah pada diskusi!", Formatting.GRAY);
                default -> {
                    List<String> mates = new ArrayList<>();
                    for (UUID o : g.order) {
                        Role r = g.roles.get(o);
                        if (!o.equals(id) && r != null && r.evil()) mates.add(g.nameOf(o));
                    }
                    Msg.chat(p, "Rekan kubu jahatmu: " + (mates.isEmpty() ? "(tidak ada)" : String.join(", ", mates)),
                            Formatting.RED);
                    Msg.chat(p, "Saat jadi anggota tim misi: ambil baterai di gudang lalu KLIK KANAN untuk menyiapkan sabotase.",
                            Formatting.DARK_RED);
                    if (role == Role.ASSASSIN) {
                        Msg.chat(p, "Di akhir permainan kamu bisa menebak siapa Merlin.", Formatting.DARK_RED);
                    }
                }
            }
            if (g.ladyHolder != null && g.ladyHolder.equals(id)) {
                Msg.chat(p, Msg.line("Kamu memegang Lady of the Lake (dipakai setelah misi).", Formatting.AQUA));
            }
        }
    }
}
