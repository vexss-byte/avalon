package dev.avalon.client;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.enums.PlayState;
import dev.avalon.net.PlayAnimPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/** Player Animation Library glue: one animation layer that plays server-triggered animations. */
public final class PalHooks {
    /** Layer id. Animations live in assets/avalon/player_animations/*.json (e.g. "pull_gate"). */
    public static final Identifier LAYER = Identifier.of("avalon", "avalon_layer");

    private PalHooks() {}

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 42,
                player -> new PlayerAnimationController(player, (controller, data, setter) -> PlayState.STOP));
    }

    public static void play(MinecraftClient client, PlayAnimPayload payload) {
        if (client.world == null) return;
        PlayerEntity target = client.world.getPlayerByUuid(payload.player);
        if (!(target instanceof AbstractClientPlayerEntity player)) return;
        IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER);
        if (layer instanceof PlayerAnimationController controller) {
            if (payload.animation.isEmpty()) {
                controller.stopTriggeredAnimation();
            } else {
                controller.triggerAnimation(Identifier.of("avalon", payload.animation));
            }
        }
    }
}
