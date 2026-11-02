package dev.avalon.game;

import dev.avalon.AvalonConfig;
import dev.avalon.world.Arena;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Plays the activation sequence of a pillar. The first 100 ticks are IDENTICAL whatever happened;
 * only afterwards does the server check the sabotage count and branch to success or corruption.
 */
public final class PillarController {
    public enum Result { RUNNING, SUCCESS, FAILED }

    private static final int ACTIVATION_TICKS = 100;
    private static final int SUCCESS_TICKS = 50;
    private static final int FAIL_TICKS = 140;

    private int pillar;
    private int t;
    private boolean checked;
    private boolean unstable;

    public void begin(int pillar) {
        this.pillar = pillar;
        this.t = 0;
        this.checked = false;
        this.unstable = false;
    }

    public Result tick(GameManager g) {
        ServerWorld w = Arena.world(g.server);
        Vec3d top = Arena.pillarTop(pillar);
        Vec3d base = Vec3d.ofCenter(Arena.pillarCenter(pillar));
        t++;

        if (t <= ACTIVATION_TICKS) {
            // identical for everyone: rising beam + spiral + hum
            for (int i = 0; i < 6; i++) {
                double y = base.y + 1 + g.random.nextDouble() * 14;
                w.spawnParticles(ParticleTypes.END_ROD, base.x, y, base.z, 1, 0.15, 0.1, 0.15, 0.01);
            }
            double a = t * 0.35;
            double r = 2.6 - 1.6 * (t / (double) ACTIVATION_TICKS);
            for (int k = 0; k < 3; k++) {
                double aa = a + k * 2.094;
                double y = base.y + 1 + (t / (double) ACTIVATION_TICKS) * 13 ;
                w.spawnParticles(ParticleTypes.ENCHANT, base.x + Math.cos(aa) * r, y, base.z + Math.sin(aa) * r,
                        2, 0.05, 0.05, 0.05, 0.0);
            }
            if (t % 20 == 1) {
                w.playSound(null, base.x, base.y + 2, base.z, SoundEvents.BLOCK_BEACON_AMBIENT,
                        SoundCategory.BLOCKS, 2.0f, 0.8f + t * 0.004f);
            }
            if (t == ACTIVATION_TICKS) {
                w.playSound(null, base.x, base.y + 2, base.z, SoundEvents.BLOCK_BEACON_ACTIVATE,
                        SoundCategory.BLOCKS, 2.0f, 1.0f);
            }
            return Result.RUNNING;
        }

        if (!checked) {
            // Server checks the (secret) sabotage count only now, AFTER the activation was played.
            checked = true;
            unstable = g.sabotage.count() >= Math.max(1, AvalonConfig.get().sabotageThreshold);
        }
        int u = t - ACTIVATION_TICKS;

        if (!unstable) {
            w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, top.x, top.y, top.z, 12, 1.2, 1.0, 1.2, 0.3);
            if (u == 1) {
                w.playSound(null, base.x, base.y + 4, base.z, SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.BLOCKS, 2.0f, 1.0f);
            }
            return u >= SUCCESS_TICKS ? Result.SUCCESS : Result.RUNNING;
        }

        // ---- unstable energy ----
        if (u <= 40) {                       // sparks
            w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, base.x, base.y + 7, base.z, 14, 2.5, 6.5, 2.5, 0.5);
            if (u % 8 == 1) {
                w.playSound(null, base.x, base.y + 4, base.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,
                        SoundCategory.BLOCKS, 0.8f, 1.6f);
            }
        } else if (u <= 90) {                // energy cracks + red/purple corruption
            double ang = u * 0.5;
            for (int k = 0; k < 4; k++) {
                double aa = ang + k * Math.PI / 2;
                for (int h = 0; h < 14; h += 2) {
                    w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            base.x + Math.cos(aa) * 1.7, base.y + 1 + h, base.z + Math.sin(aa) * 1.7,
                            1, 0.05, 0.1, 0.05, 0.0);
                }
            }
            w.spawnParticles(ParticleTypes.CRIMSON_SPORE, base.x, base.y + 6, base.z, 20, 3.0, 6.0, 3.0, 0.1);
            w.spawnParticles(ParticleTypes.WARPED_SPORE, base.x, base.y + 6, base.z, 20, 3.0, 6.0, 3.0, 0.1);
            w.spawnParticles(ParticleTypes.DRAGON_BREATH, base.x, base.y + 3, base.z, 6, 2.0, 2.0, 2.0, 0.02);
            if (u % 10 == 1) {
                w.playSound(null, base.x, base.y + 4, base.z, SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                        SoundCategory.BLOCKS, 2.0f, 0.7f);
            }
        } else if (u <= 125) {               // build-up
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, base.x, base.y + 8, base.z, 25, 2.0, 7.0, 2.0, 0.05);
            w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, base.x, base.y + 7, base.z, 30, 3.0, 7.0, 3.0, 0.8);
        } else if (u == 126) {               // controlled explosion: visuals only, no block damage
            w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, base.x, base.y + 8, base.z, 3, 1.5, 3.0, 1.5, 0.0);
            w.spawnParticles(ParticleTypes.FLASH, base.x, base.y + 8, base.z, 2, 0.5, 0.5, 0.5, 0.0);
            w.playSound(null, base.x, base.y + 4, base.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                    SoundCategory.BLOCKS, 4.0f, 0.8f);
        }
        return u >= FAIL_TICKS ? Result.FAILED : Result.RUNNING;
    }
}
