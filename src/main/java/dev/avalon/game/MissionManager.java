package dev.avalon.game;

import dev.avalon.AvalonConfig;
import dev.avalon.block.AvalonBlocks;
import dev.avalon.block.BatterySocketBlock;
import dev.avalon.item.AvalonItems;
import dev.avalon.world.Arena;
import dev.avalon.world.MapBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative pillar mission: depot -> socket interactions with full validation. */
public final class MissionManager {
    private final Set<BlockPos> installed = new HashSet<>();

    public int installedCount() {
        return installed.size();
    }

    public void start(GameManager g) {
        ServerWorld arena = Arena.world(g.server);
        installed.clear();
        g.sabotage.resetPillar();
        MapBuilder.resetSockets(arena, g.round);
        int slot = 0;
        for (UUID id : g.team) {
            ServerPlayerEntity p = g.player(id);
            if (p == null) continue;
            Vec3d s = Arena.pillarStand(g.round, slot++);
            Vec3d c = Vec3d.ofCenter(Arena.pillarCenter(g.round));
            float yaw = Arena.yawTo(s.x, s.z, c.x, c.z);
            p.teleport(arena, s.x, s.y, s.z, yaw, 0f);
            arena.spawnParticles(ParticleTypes.REVERSE_PORTAL, s.x, s.y + 1, s.z, 30, 0.4, 0.8, 0.4, 0.05);
            Msg.title(p, "MISI PILAR " + (g.round + 1), Formatting.LIGHT_PURPLE,
                    "Ambil baterai di gudang, pasang di 4 soket", Formatting.GRAY, 5, 50, 10);
        }
        for (UUID id : g.order) {
            if (g.team.contains(id)) continue;
            ServerPlayerEntity p = g.player(id);
            if (p == null || g.ejected.contains(id)) continue;
            Msg.chat(p, "Kamu tidak terpilih dalam misi ini. Mode menonton.", Formatting.GRAY);
        }
    }

    /** Handles a right click on a block. Returns PASS when the block is none of ours. */
    public ActionResult handleUse(GameManager g, ServerPlayerEntity p, Hand hand, BlockPos pos) {
        ServerWorld w = p.getServerWorld();
        BlockState state = w.getBlockState(pos);
        boolean socket = state.isOf(AvalonBlocks.BATTERY_SOCKET);
        boolean depot = state.isOf(AvalonBlocks.DEPOT);
        if (!socket && !depot) return ActionResult.PASS;

        // ---- common validation (public information only, so GOOD/EVIL behave identically) ----
        if (g.phase != Phase.MISSION) return ActionResult.FAIL;
        if (w.getRegistryKey() != Arena.ARENA_KEY) return ActionResult.FAIL;
        if (!g.team.contains(p.getUuid()) || g.ejected.contains(p.getUuid())) {
            Msg.action(p, "Kamu bukan anggota tim misi ini.", Formatting.GRAY);
            return ActionResult.FAIL;
        }
        double max = AvalonConfig.get().interactDistance;
        if (p.getPos().squaredDistanceTo(Vec3d.ofCenter(pos)) > max * max) return ActionResult.FAIL;

        if (depot) {
            if (!pos.equals(Arena.depot(g.round))) return ActionResult.FAIL;
            if (p.getInventory().count(AvalonItems.BATTERY) > 0) {
                Msg.action(p, "Kamu sudah membawa baterai.", Formatting.GRAY);
                return ActionResult.FAIL;
            }
            if (installed.size() >= 4) return ActionResult.FAIL;
            p.giveItemStack(new ItemStack(AvalonItems.BATTERY));
            w.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.8f, 1.2f);
            return ActionResult.SUCCESS;
        }

        // ---- socket ----
        if (!Arena.isPillarSocket(g.round, pos)) return ActionResult.FAIL;
        ItemStack stack = p.getStackInHand(hand);
        if (!stack.isOf(AvalonItems.BATTERY)) {
            Msg.action(p, "Pegang baterai untuk memasangnya.", Formatting.GRAY);
            return ActionResult.FAIL;
        }
        if (state.get(BatterySocketBlock.INSTALLED) || installed.contains(pos)) return ActionResult.FAIL;

        stack.decrement(1);
        w.setBlockState(pos, state.with(BatterySocketBlock.INSTALLED, true), 3);
        installed.add(pos.toImmutable());
        // Secret: recorded server-side only. The visible effect below is IDENTICAL for GOOD and EVIL.
        g.sabotage.onInstall(g, p);

        w.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.0f);
        w.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 0.6f, 1.4f);
        w.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.4, 0.4, 0.4, 0.02);
        Msg.chatAll(g.alivePlayers(), "Baterai terpasang: " + installed.size() + "/4", Formatting.AQUA);

        if (installed.size() >= 4) g.beginActivation();
        return ActionResult.SUCCESS;
    }

    /** Remove loose batteries and put every living player back to adventure mode. */
    public void clearBatteries(GameManager g) {
        for (UUID id : g.order) {
            ServerPlayerEntity p = g.player(id);
            if (p == null) continue;
            for (int i = 0; i < p.getInventory().size(); i++) {
                if (p.getInventory().getStack(i).isOf(AvalonItems.BATTERY)) {
                    p.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }
        }
    }

    public void returnAllToHall(GameManager g) {
        ServerWorld arena = Arena.world(g.server);
        List<UUID> list = g.order;
        int n = list.size();
        Vec3d c = Arena.hallCenterVec();
        for (int i = 0; i < n; i++) {
            ServerPlayerEntity p = g.player(list.get(i));
            if (p == null) continue;
            Vec3d s = Arena.hallSeat(i, n);
            float yaw = Arena.yawTo(s.x, s.z, c.x, c.z);
            if (g.ejected.contains(p.getUuid())) {
                p.changeGameMode(GameMode.SPECTATOR);
            } else {
                p.changeGameMode(GameMode.ADVENTURE);
            }
            p.setNoGravity(false);
            p.teleport(arena, s.x, s.y, s.z, yaw, 0f);
            arena.spawnParticles(ParticleTypes.REVERSE_PORTAL, s.x, s.y + 1, s.z, 20, 0.4, 0.8, 0.4, 0.05);
        }
    }
}
