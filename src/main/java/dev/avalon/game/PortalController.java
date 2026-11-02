package dev.avalon.game;

import dev.avalon.entity.AvalonEntities;
import dev.avalon.entity.PortalGateEntity;
import dev.avalon.net.PlayAnimPayload;
import dev.avalon.world.Arena;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cinematic intro portal. It is made of real world objects (block_display entities for the gate
 * and rune rings, particles for the magic circle and vortex), NOT a screen overlay.
 * No blur, no shaders, no Vulkan: plain vanilla entities + particles, fine on OpenGL 3.3.
 */
public final class PortalController {
    public static final String FX_TAG = "avalon_fx";
    private static final int COUNTDOWN = 100;     // 5 s
    private static final int PULL = 100;          // 5 s
    private static final int TOTAL = COUNTDOWN + PULL;

    private int t;
    private boolean active;
    private ServerWorld world;
    private Vec3d center;       // lobby gathering centre
    private Vec3d gate;         // centre of the gate
    private final List<UUID> pulled = new ArrayList<>();
    private final List<Vec3d> starts = new ArrayList<>();

    public boolean active() {
        return active;
    }

    public void cancel() {
        active = false;
    }

    public void begin(GameManager g, ServerWorld lobby, Vec3d lobbyPos) {
        this.world = lobby;
        this.center = lobbyPos;
        this.gate = lobbyPos.add(0, 7, 9);
        this.t = 0;
        this.active = true;
        pulled.clear();
        starts.clear();

        // gather players on a ring in front of the gate
        int n = g.order.size();
        for (int i = 0; i < n; i++) {
            ServerPlayerEntity p = g.player(g.order.get(i));
            if (p == null) continue;
            double a = Math.PI * 2 * i / Math.max(1, n);
            Vec3d s = new Vec3d(center.x + Math.cos(a) * 4, center.y, center.z + Math.sin(a) * 4);
            float yaw = Arena.yawTo(s.x, s.z, gate.x, gate.z);
            p.teleport(world, s.x, s.y, s.z, yaw, -25f);
            pulled.add(p.getUuid());
            starts.add(s);
        }
        buildGate();
        world.playSound(null, gate.x, gate.y, gate.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.AMBIENT,
                6.0f, 0.8f);
    }

    /** Spawns the AzureLib portal gate entity (Blockbench model, spinning rune rings). */
    private void buildGate() {
        PortalGateEntity gateEntity = AvalonEntities.PORTAL_GATE.create(world);
        if (gateEntity == null) return;
        gateEntity.refreshPositionAndAngles(gate.x, gate.y, gate.z, 0.0f, 0.0f);
        world.spawnEntity(gateEntity);
    }

    private void sendAnim(GameManager g, UUID target, String animation) {
        PlayAnimPayload payload = new PlayAnimPayload(target, animation);
        for (ServerPlayerEntity viewer : g.server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    /** Stops the PAL pull animation for everybody who was pulled. */
    public void stopAnimations(GameManager g) {
        for (UUID id : pulled) sendAnim(g, id, "");
    }

    public void removeFx(MinecraftServer server, ServerWorld w) {
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource().withWorld(w).withSilent(),
                "kill @e[type=avalon:portal_gate]");
    }

    /** @return true when the intro finished this tick (caller should teleport everybody to the Hall). */
    public boolean tick(GameManager g) {
        if (!active) return false;
        t++;
        spawnAmbient();

        if (t <= COUNTDOWN) {
            if (t % 20 == 1) {
                int sec = 5 - (t / 20);
                for (UUID id : pulled) {
                    ServerPlayerEntity p = g.player(id);
                    if (p != null) {
                        Msg.title(p, "Game dimulai dalam...", Formatting.GOLD, String.valueOf(sec), Formatting.AQUA, 0, 25, 5);
                    }
                }
                world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                        SoundCategory.AMBIENT, 2.0f, 0.6f + (5 - sec) * 0.15f);
            }
            return false;
        }

        int u = t - COUNTDOWN;
        if (u == 1) {
            for (UUID id : pulled) sendAnim(g, id, "pull_gate");
        }
        double s = Math.min(1.0, u / (double) PULL);
        double ease = s * s * (3 - 2 * s);
        for (int i = 0; i < pulled.size(); i++) {
            ServerPlayerEntity p = g.player(pulled.get(i));
            if (p == null) continue;
            Vec3d st = starts.get(i);
            double swirl = (1 - ease) * 2.2;
            double ang = u * 0.18 + i;
            double x = st.x + (gate.x - st.x) * ease + Math.cos(ang) * swirl * (1 - ease);
            double y = st.y + (gate.y - 1 - st.y) * ease + Math.sin(s * Math.PI) * 2.5;
            double z = st.z + (gate.z - st.z) * ease + Math.sin(ang) * swirl * (1 - ease);
            p.setNoGravity(true);
            p.fallDistance = 0;
            p.setVelocity(Vec3d.ZERO);
            p.requestTeleport(x, y, z);
        }
        if (u % 12 == 1) {
            world.playSound(null, gate.x, gate.y, gate.z, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT,
                    4.0f, 0.7f + (float) s * 0.6f);
        }
        if (u == PULL - 5) {
            world.spawnParticles(ParticleTypes.FLASH, gate.x, gate.y, gate.z, 3, 0.5, 0.5, 0.5, 0.0);
            world.playSound(null, gate.x, gate.y, gate.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.AMBIENT,
                    4.0f, 0.5f);
        }
        if (t >= TOTAL) {
            active = false;
            stopAnimations(g);
            for (UUID id : pulled) {
                ServerPlayerEntity p = g.player(id);
                if (p != null) {
                    p.setNoGravity(false);
                    p.fallDistance = 0;
                }
            }
            removeFx(g.server, world);
            return true;
        }
        return false;
    }

    /** Magic circle on the ground, rune rings and a swirling vortex/black hole in the gate. */
    private void spawnAmbient() {
        double power = Math.min(1.0, t / 80.0);
        // ground magic circle (two counter-rotating rings + hexagram points)
        for (int k = 0; k < 3; k++) {
            double a = t * 0.07 + k * 2.094;
            world.spawnParticles(ParticleTypes.END_ROD, center.x + Math.cos(a) * 6.5, center.y + 0.1,
                    center.z + Math.sin(a) * 6.5, 1, 0, 0, 0, 0);
            double b = -t * 0.05 + k * 2.094;
            world.spawnParticles(ParticleTypes.WITCH, center.x + Math.cos(b) * 4.5, center.y + 0.1,
                    center.z + Math.sin(b) * 4.5, 1, 0, 0, 0, 0);
        }
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 3 * i + t * 0.02;
            double a2 = Math.PI / 3 * (i + 2) + t * 0.02;
            for (int j = 0; j <= 4; j++) {
                double f = j / 4.0;
                double x = center.x + Math.cos(a) * 6.5 * (1 - f) + Math.cos(a2) * 6.5 * f;
                double z = center.z + Math.sin(a) * 6.5 * (1 - f) + Math.sin(a2) * 6.5 * f;
                if ((t + j + i) % 6 == 0) world.spawnParticles(ParticleTypes.ENCHANT, x, center.y + 0.2, z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
        // vortex / black hole in the gate plane
        int arms = (int) (6 + 8 * power);
        for (int k = 0; k < arms; k++) {
            double r = 0.4 + (k / (double) arms) * 4.2;
            double a = t * 0.35 - r * 1.4 + k;
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, gate.x + Math.cos(a) * r, gate.y + Math.sin(a) * r,
                    gate.z, 1, 0, 0, 0, 0.02);
            if (k % 3 == 0) {
                world.spawnParticles(ParticleTypes.PORTAL, gate.x + Math.cos(a) * r, gate.y + Math.sin(a) * r,
                        gate.z, 1, 0.1, 0.1, 0.1, 0.1);
            }
        }
        world.spawnParticles(ParticleTypes.SQUID_INK, gate.x, gate.y, gate.z, 2, 0.35, 0.35, 0.1, 0.0);
        // rune sparks orbiting
        for (int k = 0; k < 4; k++) {
            double a = -t * 0.12 + k * Math.PI / 2;
            world.spawnParticles(ParticleTypes.ENCHANT, gate.x + Math.cos(a) * 4.2, gate.y + Math.sin(a) * 4.2,
                    gate.z + 0.4, 2, 0.1, 0.1, 0.1, 0.0);
        }
    }
}
