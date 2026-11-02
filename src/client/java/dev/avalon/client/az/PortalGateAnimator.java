package dev.avalon.client.az;

import dev.avalon.entity.PortalGateEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.util.Identifier;

public class PortalGateAnimator extends AzEntityAnimator<PortalGateEntity> {
    private static final Identifier ANIMATIONS =
            Identifier.of("avalon", "animations/entity/portal_gate.animation.json");

    /** Looping spin of the three rune rings (defined in portal_gate.animation.json, editable in Blockbench). */
    public static final AzCommand SPIN = AzCommand.create("base_controller", "spin", AzPlayBehaviors.LOOP);

    @Override
    public void registerControllers(AzAnimationControllerContainer<PortalGateEntity> container) {
        container.add(AzAnimationController.builder(this, "base_controller").build());
    }

    @Override
    public Identifier getAnimationLocation(PortalGateEntity animatable) {
        return ANIMATIONS;
    }
}
