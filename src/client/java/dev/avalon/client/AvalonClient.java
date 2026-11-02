package dev.avalon.client;

import dev.avalon.client.az.BatteryAnimator;
import dev.avalon.client.az.BatteryRenderer;
import dev.avalon.client.az.PortalGateAnimator;
import dev.avalon.client.az.PortalGateRenderer;
import dev.avalon.entity.AvalonEntities;
import dev.avalon.entity.PortalGateEntity;
import dev.avalon.item.AvalonItems;
import dev.avalon.item.BatteryItem;
import dev.avalon.net.CloseScreenPayload;
import dev.avalon.net.OpenSelectPayload;
import dev.avalon.net.OpenVotePayload;
import dev.avalon.net.PlayAnimPayload;
import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class AvalonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // ---- screens ----
        ClientPlayNetworking.registerGlobalReceiver(OpenSelectPayload.ID,
                (payload, ctx) -> ctx.client().setScreen(new SelectScreen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(OpenVotePayload.ID,
                (payload, ctx) -> ctx.client().setScreen(new VoteScreen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(CloseScreenPayload.ID, (payload, ctx) -> {
            if (ctx.client().currentScreen instanceof AvalonScreen) ctx.client().setScreen(null);
        });

        // ---- AzureLib: 3D portal gate entity + animated battery item ----
        EntityRendererRegistry.register(AvalonEntities.PORTAL_GATE, PortalGateRenderer::new);
        AzItemRendererRegistry.register(BatteryRenderer::new, AvalonItems.BATTERY);
        PortalGateEntity.CLIENT_TICK = gate -> {
            if (gate.age % 10 == 1) PortalGateAnimator.SPIN.sendForEntity(gate);
        };
        BatteryItem.CLIENT_HOOK = (entity, stack) -> BatteryAnimator.GLOW.sendForItem(entity, stack);

        // ---- Player Animation Library: pull-into-portal animation ----
        PalHooks.register();
        ClientPlayNetworking.registerGlobalReceiver(PlayAnimPayload.ID,
                (payload, ctx) -> PalHooks.play(ctx.client(), payload));
    }
}
