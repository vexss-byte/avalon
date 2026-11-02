package dev.avalon.net;

import dev.avalon.game.GameManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class NetworkHandler {
    private NetworkHandler() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenSelectPayload.ID, OpenSelectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenVotePayload.ID, OpenVotePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CloseScreenPayload.ID, CloseScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayAnimPayload.ID, PlayAnimPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectResultPayload.ID, SelectResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VoteCastPayload.ID, VoteCastPayload.CODEC);

        // Handlers run on the server thread. The server re-validates phase, membership and counts.
        ServerPlayNetworking.registerGlobalReceiver(SelectResultPayload.ID, (payload, ctx) -> {
            GameManager g = GameManager.get();
            if (g != null) g.handleSelect(ctx.player(), payload);
        });
        ServerPlayNetworking.registerGlobalReceiver(VoteCastPayload.ID, (payload, ctx) -> {
            GameManager g = GameManager.get();
            if (g != null) g.handleVote(ctx.player(), payload.approve);
        });
    }
}
