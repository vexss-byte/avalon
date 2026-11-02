package dev.avalon.world;

import dev.avalon.AvalonConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** All coordinates of the single unified map. Everything is derived from the config origin. */
public final class Arena {
    public static final RegistryKey<World> ARENA_KEY =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("avalon", "arena"));

    private Arena() {}

    public static ServerWorld world(MinecraftServer server) {
        return server.getWorld(ARENA_KEY);
    }

    public static ServerWorld lobbyWorld(MinecraftServer server) {
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD,
                Identifier.of(AvalonConfig.get().lobbyDimension));
        ServerWorld w = server.getWorld(key);
        return w != null ? w : server.getOverworld();
    }

    public static Vec3d lobbyPos() {
        AvalonConfig c = AvalonConfig.get();
        return new Vec3d(c.lobbyX, c.lobbyY, c.lobbyZ);
    }

    /** Y of the floor blocks. Players stand on floorY + 1. */
    public static int floorY() {
        return AvalonConfig.get().arenaY;
    }

    public static BlockPos hallCenter() {
        AvalonConfig c = AvalonConfig.get();
        return new BlockPos(c.arenaX, c.arenaY, c.arenaZ);
    }

    /** Position of a seat in the Discussion Hall (index i of n), y is the standing position. */
    public static Vec3d hallSeat(int i, int n) {
        AvalonConfig c = AvalonConfig.get();
        double r = c.hallRadius - 6;
        double a = (Math.PI * 2.0 * i) / Math.max(1, n);
        double x = c.arenaX + 0.5 + Math.cos(a) * r;
        double z = c.arenaZ + 0.5 + Math.sin(a) * r;
        return new Vec3d(x, c.arenaY + 1.0, z);
    }

    /** Yaw (degrees) so that an entity at (x,z) looks at (tx,tz). */
    public static float yawTo(double x, double z, double tx, double tz) {
        return (float) (Math.toDegrees(Math.atan2(-(tx - x), tz - z)));
    }

    public static Vec3d hallCenterVec() {
        BlockPos p = hallCenter();
        return new Vec3d(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5);
    }

    /** Pillar 0 = north, pillar 1 = west, pillar 2 = east (matches the reference diagram). */
    public static BlockPos pillarCenter(int p) {
        AvalonConfig c = AvalonConfig.get();
        int d = c.pillarDistance;
        return switch (p) {
            case 0 -> new BlockPos(c.arenaX, c.arenaY, c.arenaZ - d);
            case 1 -> new BlockPos(c.arenaX - d, c.arenaY, c.arenaZ);
            default -> new BlockPos(c.arenaX + d, c.arenaY, c.arenaZ);
        };
    }

    /** Unit axis from the pillar toward the hall. */
    public static int[] towardHall(int p) {
        return switch (p) {
            case 0 -> new int[]{0, 1};
            case 1 -> new int[]{1, 0};
            default -> new int[]{-1, 0};
        };
    }

    /** The four battery sockets of a pillar. */
    public static BlockPos[] sockets(int p) {
        BlockPos c = pillarCenter(p);
        int y = c.getY() + 2;
        return new BlockPos[]{
                new BlockPos(c.getX() + 2, y, c.getZ()),
                new BlockPos(c.getX() - 2, y, c.getZ()),
                new BlockPos(c.getX(), y, c.getZ() + 2),
                new BlockPos(c.getX(), y, c.getZ() - 2)
        };
    }

    public static BlockPos depot(int p) {
        BlockPos c = pillarCenter(p);
        int[] t = towardHall(p);
        int px = -t[1]; // perpendicular axis
        int pz = t[0];
        return new BlockPos(c.getX() + t[0] * 4 + px * 3, c.getY() + 1, c.getZ() + t[1] * 4 + pz * 3);
    }

    /** Where mission team members stand when a mission begins. */
    public static Vec3d pillarStand(int p, int slot) {
        BlockPos c = pillarCenter(p);
        int[] t = towardHall(p);
        int px = -t[1];
        int pz = t[0];
        double off = (slot - 1.5) * 1.5;
        return new Vec3d(c.getX() + 0.5 + t[0] * 5 + px * off, c.getY() + 1.0, c.getZ() + 0.5 + t[1] * 5 + pz * off);
    }

    public static Vec3d pillarTop(int p) {
        BlockPos c = pillarCenter(p);
        return new Vec3d(c.getX() + 0.5, c.getY() + 14.0, c.getZ() + 0.5);
    }

    public static boolean isPillarSocket(int p, BlockPos pos) {
        for (BlockPos s : sockets(p)) if (s.equals(pos)) return true;
        return false;
    }
}
