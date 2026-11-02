package dev.avalon.game;

import dev.avalon.AvalonConfig;
import dev.avalon.item.AvalonItems;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side secret sabotage state. Nothing here is ever sent to other players.
 * Rules: only EVIL players on the current mission team, only during the MISSION phase,
 * at most once per pillar and at most sabotageUsesPerPlayer times per game.
 */
public final class SabotageManager {
    private final Set<UUID> armed = new HashSet<>();
    private final Set<UUID> sabotagedThisPillar = new HashSet<>();
    private final Map<UUID, Integer> usedTotal = new HashMap<>();
    private int sabotageCount;

    public void resetAll() {
        armed.clear();
        sabotagedThisPillar.clear();
        usedTotal.clear();
        sabotageCount = 0;
    }

    public void resetPillar() {
        armed.clear();
        sabotagedThisPillar.clear();
        sabotageCount = 0;
    }

    public int count() {
        return sabotageCount;
    }

    private boolean eligible(GameManager g, ServerPlayerEntity p) {
        if (g.phase != Phase.MISSION) return false;
        Role r = g.roles.get(p.getUuid());
        if (r == null || !r.evil()) return false;
        if (!g.team.contains(p.getUuid())) return false;
        if (g.ejected.contains(p.getUuid())) return false;
        if (sabotagedThisPillar.contains(p.getUuid())) return false;
        return usedTotal.getOrDefault(p.getUuid(), 0) < AvalonConfig.get().sabotageUsesPerPlayer;
    }

    /** Right click with a battery. Toggles the secret flag; only the acting EVIL player ever sees feedback. */
    public void toggleArm(GameManager g, ServerPlayerEntity p) {
        if (!eligible(g, p)) return;
        if (!p.getMainHandStack().isOf(AvalonItems.BATTERY)) return;
        if (armed.remove(p.getUuid())) {
            Msg.action(p, "Sabotase dibatalkan.", Formatting.GRAY);
        } else {
            armed.add(p.getUuid());
            Msg.action(p, "Sabotase siap pada baterai ini.", Formatting.DARK_PURPLE);
        }
    }

    /** Called when a battery is inserted. Returns true if THIS install was a sabotage (kept secret). */
    public boolean onInstall(GameManager g, ServerPlayerEntity p) {
        if (!armed.remove(p.getUuid())) return false;
        if (!eligible(g, p)) return false;
        sabotagedThisPillar.add(p.getUuid());
        usedTotal.merge(p.getUuid(), 1, Integer::sum);
        sabotageCount++;
        return true;
    }

    /** Private hotbar hint shown only to EVIL team members holding a battery. */
    public void tickHints(GameManager g) {
        if (g.phase != Phase.MISSION) return;
        for (UUID id : g.team) {
            ServerPlayerEntity p = g.player(id);
            if (p == null) continue;
            if (!eligible(g, p)) continue;
            if (!p.getMainHandStack().isOf(AvalonItems.BATTERY)) continue;
            if (armed.contains(id)) {
                Msg.action(p, "Sabotase SIAP - pasang baterai ini di pilar", Formatting.DARK_PURPLE);
            } else {
                Msg.action(p, "Klik kanan untuk sabotase", Formatting.RED);
            }
        }
    }
}
