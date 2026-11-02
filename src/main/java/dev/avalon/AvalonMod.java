package dev.avalon;

import dev.avalon.block.AvalonBlocks;
import dev.avalon.entity.AvalonEntities;
import dev.avalon.game.GameManager;
import dev.avalon.item.AvalonItems;
import dev.avalon.net.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvalonMod implements ModInitializer {
    public static final String MOD_ID = "avalon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AvalonConfig.load();
        AvalonBlocks.init();
        AvalonEntities.init();
        AvalonItems.init();
        NetworkHandler.register();
        AvalonCommands.register();

        ServerLifecycleEvents.SERVER_STARTED.register(GameManager::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GameManager.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GameManager g = GameManager.get();
            if (g != null) g.tick();
        });

        // Depot + socket interaction. Everything is validated server-side in MissionManager.
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            GameManager g = GameManager.get();
            if (g == null) return ActionResult.PASS;
            return g.mission.handleUse(g, sp, hand, hit.getBlockPos());
        });

        LOGGER.info("AVALON: The Forgotten Kingdom loaded.");
    }
}
