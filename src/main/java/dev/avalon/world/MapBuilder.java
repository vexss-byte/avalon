package dev.avalon.world;

import dev.avalon.AvalonConfig;
import dev.avalon.block.AvalonBlocks;
import dev.avalon.block.BatterySocketBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Builds the single unified map: ONE Central Discussion Hall + EXACTLY THREE pillars
 * connected by roads. Called by /avalon build (no external world file needed).
 */
public final class MapBuilder {
    private MapBuilder() {}

    private static final int FLAGS = Block.NOTIFY_LISTENERS;

    private static void set(ServerWorld w, int x, int y, int z, BlockState s) {
        w.setBlockState(new BlockPos(x, y, z), s, FLAGS);
    }

    private static void loadArea(ServerWorld w, int minX, int minZ, int maxX, int maxZ) {
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                w.getChunk(cx, cz);
            }
        }
    }

    public static void build(ServerWorld w) {
        AvalonConfig c = AvalonConfig.get();
        int ext = c.pillarDistance + 12;
        loadArea(w, c.arenaX - ext, c.arenaZ - ext, c.arenaX + ext, c.arenaZ + ext);
        buildHall(w);
        for (int p = 0; p < 3; p++) {
            buildRoad(w, p);
            buildPillar(w, p);
        }
    }

    private static void buildHall(ServerWorld w) {
        AvalonConfig c = AvalonConfig.get();
        int R = c.hallRadius;
        int Y = c.arenaY;
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R) continue;
                int x = c.arenaX + dx;
                int z = c.arenaZ + dz;
                BlockState floor = ((dx + dz) & 1) == 0 ? Blocks.DEEPSLATE_TILES.getDefaultState()
                        : Blocks.POLISHED_DEEPSLATE.getDefaultState();
                if (d <= 5.5) floor = d <= 3.5 ? Blocks.AMETHYST_BLOCK.getDefaultState()
                        : Blocks.CRYING_OBSIDIAN.getDefaultState();
                set(w, x, Y, z, floor);
                // clear above
                for (int h = 1; h <= 6; h++) set(w, x, Y + h, z, Blocks.AIR.getDefaultState());

                // outer wall with three exits
                if (d > R - 1.5) {
                    boolean exitN = dz < 0 && Math.abs(dx) <= 2;
                    boolean exitW = dx < 0 && Math.abs(dz) <= 2;
                    boolean exitE = dx > 0 && Math.abs(dz) <= 2;
                    if (!(exitN || exitW || exitE)) {
                        set(w, x, Y + 1, z, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
                        set(w, x, Y + 2, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
                    }
                }
            }
        }
        // centre altar
        set(w, c.arenaX, Y + 1, c.arenaZ, Blocks.AMETHYST_CLUSTER.getDefaultState());
        // seats (stairs facing the centre)
        int seats = 24;
        for (int i = 0; i < seats; i++) {
            Vec3 v = polar(c.arenaX, c.arenaZ, R - 6, i, seats);
            int dx = v.x - c.arenaX;
            int dz = v.z - c.arenaZ;
            Direction facing = Math.abs(dx) > Math.abs(dz)
                    ? (dx > 0 ? Direction.WEST : Direction.EAST)
                    : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
            set(w, v.x, Y + 1, v.z,
                    Blocks.DEEPSLATE_TILE_STAIRS.getDefaultState().with(StairsBlock.FACING, facing));
        }
        // light posts around the rim
        for (int i = 0; i < 16; i++) {
            Vec3 v = polar(c.arenaX, c.arenaZ, R - 3, i, 16);
            set(w, v.x, Y + 1, v.z, Blocks.CRYING_OBSIDIAN.getDefaultState());
            set(w, v.x, Y + 2, v.z, Blocks.CRYING_OBSIDIAN.getDefaultState());
            set(w, v.x, Y + 3, v.z, Blocks.END_ROD.getDefaultState());
        }
    }

    private record Vec3(int x, int z) {}

    private static Vec3 polar(int cx, int cz, double r, int i, int n) {
        double a = Math.PI * 2.0 * i / n;
        return new Vec3((int) Math.round(cx + Math.cos(a) * r), (int) Math.round(cz + Math.sin(a) * r));
    }

    private static void buildRoad(ServerWorld w, int p) {
        AvalonConfig c = AvalonConfig.get();
        BlockPos pc = Arena.pillarCenter(p);
        int[] t = Arena.towardHall(p);          // direction pillar -> hall
        int px = -t[1];
        int pz = t[0];
        int from = 6;                            // just outside pillar platform
        int to = c.pillarDistance - c.hallRadius + 1;
        for (int s = from; s <= to; s++) {
            for (int wdt = -1; wdt <= 1; wdt++) {
                int x = pc.getX() + t[0] * s + px * wdt;
                int z = pc.getZ() + t[1] * s + pz * wdt;
                BlockState b = ((s + wdt) % 5 == 0) ? Blocks.MOSSY_STONE_BRICKS.getDefaultState()
                        : (((s * 7 + wdt) % 11 == 0) ? Blocks.CRACKED_STONE_BRICKS.getDefaultState()
                        : Blocks.STONE_BRICKS.getDefaultState());
                set(w, x, c.arenaY, z, b);
                for (int h = 1; h <= 4; h++) set(w, x, c.arenaY + h, z, Blocks.AIR.getDefaultState());
            }
            if (s % 6 == 0) {
                for (int side = -2; side <= 2; side += 4) {
                    int x = pc.getX() + t[0] * s + px * side;
                    int z = pc.getZ() + t[1] * s + pz * side;
                    set(w, x, c.arenaY, z, Blocks.STONE_BRICKS.getDefaultState());
                    set(w, x, c.arenaY + 1, z, Blocks.CRYING_OBSIDIAN.getDefaultState());
                    set(w, x, c.arenaY + 2, z, Blocks.END_ROD.getDefaultState());
                }
            }
        }
    }

    private static void buildPillar(ServerWorld w, int p) {
        BlockPos c = Arena.pillarCenter(p);
        int Y = c.getY();
        // platform 13 x 13
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                BlockState floor = (Math.abs(dx) == 6 || Math.abs(dz) == 6)
                        ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()
                        : (((dx + dz) & 1) == 0 ? Blocks.DEEPSLATE_BRICKS.getDefaultState()
                        : Blocks.DEEPSLATE_TILES.getDefaultState());
                set(w, c.getX() + dx, Y, c.getZ() + dz, floor);
                for (int h = 1; h <= 16; h++) set(w, c.getX() + dx, Y + h, c.getZ() + dz, Blocks.AIR.getDefaultState());
            }
        }
        // tower core 3x3 x 13
        for (int h = 1; h <= 13; h++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockState b;
                    if (dx == 0 && dz == 0) b = Blocks.AMETHYST_BLOCK.getDefaultState();
                    else if (h % 4 == 0) b = Blocks.CRYING_OBSIDIAN.getDefaultState();
                    else if (h % 2 == 0 && (dx == 0 || dz == 0)) b = Blocks.PURPLE_STAINED_GLASS.getDefaultState();
                    else b = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                    set(w, c.getX() + dx, Y + h, c.getZ() + dz, b);
                }
            }
        }
        set(w, c.getX(), Y + 14, c.getZ(), Blocks.SEA_LANTERN.getDefaultState());
        set(w, c.getX(), Y + 15, c.getZ(), Blocks.END_ROD.getDefaultState());
        // corner pylons
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int x = c.getX() + sx * 5;
                int z = c.getZ() + sz * 5;
                for (int h = 1; h <= 3; h++) set(w, x, Y + h, z, Blocks.CRYING_OBSIDIAN.getDefaultState());
                set(w, x, Y + 4, z, Blocks.END_ROD.getDefaultState());
            }
        }
        // sockets (4) + depot
        for (BlockPos s : Arena.sockets(p)) {
            w.setBlockState(s, AvalonBlocks.BATTERY_SOCKET.getDefaultState()
                    .with(BatterySocketBlock.INSTALLED, false), FLAGS);
        }
        w.setBlockState(Arena.depot(p), AvalonBlocks.DEPOT.getDefaultState(), FLAGS);
    }

    /** Resets sockets of a pillar to empty. */
    public static void resetSockets(ServerWorld w, int p) {
        for (BlockPos s : Arena.sockets(p)) {
            w.setBlockState(s, AvalonBlocks.BATTERY_SOCKET.getDefaultState()
                    .with(BatterySocketBlock.INSTALLED, false), FLAGS);
        }
    }
}
