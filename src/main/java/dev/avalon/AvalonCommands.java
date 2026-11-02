package dev.avalon;

import com.mojang.brigadier.CommandDispatcher;
import dev.avalon.game.GameManager;
import dev.avalon.world.Arena;
import dev.avalon.world.MapBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AvalonCommands {
    private AvalonCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
    }

    private static void build(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("avalon")
                .then(CommandManager.literal("start")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            GameManager g = GameManager.get();
                            String err = g == null ? "Game manager belum siap." : g.startGame();
                            if (err != null) {
                                ctx.getSource().sendError(Text.literal(err));
                                return 0;
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal("Avalon dimulai!").formatted(Formatting.GOLD), true);
                            return 1;
                        }))
                .then(CommandManager.literal("stop")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            GameManager g = GameManager.get();
                            if (g != null) g.forceStop();
                            ctx.getSource().sendFeedback(() -> Text.literal("Avalon dihentikan."), true);
                            return 1;
                        }))
                .then(CommandManager.literal("build")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerWorld w = Arena.world(ctx.getSource().getServer());
                            if (w == null) {
                                ctx.getSource().sendError(Text.literal("Dimensi avalon:arena tidak ditemukan."));
                                return 0;
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal("Membangun map Avalon..."), true);
                            MapBuilder.build(w);
                            ctx.getSource().sendFeedback(() -> Text.literal("Map selesai: 1 Discussion Hall + 3 Pilar."), true);
                            return 1;
                        }))
                .then(CommandManager.literal("setlobby")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            AvalonConfig c = AvalonConfig.get();
                            c.lobbyDimension = p.getServerWorld().getRegistryKey().getValue().toString();
                            c.lobbyX = p.getX();
                            c.lobbyY = p.getY();
                            c.lobbyZ = p.getZ();
                            AvalonConfig.save();
                            ctx.getSource().sendFeedback(() -> Text.literal("Lobby disimpan di posisimu."), true);
                            return 1;
                        }))
                .then(CommandManager.literal("reload")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            AvalonConfig.load();
                            ctx.getSource().sendFeedback(() -> Text.literal("Config dimuat ulang."), true);
                            return 1;
                        }))
                .then(CommandManager.literal("status")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            GameManager g = GameManager.get();
                            String st = g == null ? "tidak aktif" : g.status();
                            ctx.getSource().sendFeedback(() -> Text.literal(st), false);
                            return 1;
                        }))
                .then(CommandManager.literal("skip")
                        .executes(ctx -> {
                            GameManager g = GameManager.get();
                            if (g != null) g.handleSkip(ctx.getSource().getPlayerOrThrow());
                            return 1;
                        }))
                .then(CommandManager.literal("menu")
                        .executes(ctx -> {
                            GameManager g = GameManager.get();
                            if (g != null) g.reopenFor(ctx.getSource().getPlayerOrThrow());
                            return 1;
                        })));
    }
}
