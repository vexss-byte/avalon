package dev.avalon.client.az;

import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.impl.AzItemAnimator;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class BatteryAnimator extends AzItemAnimator {
    private static final Identifier ANIMATIONS =
            Identifier.of("avalon", "animations/item/battery.animation.json");

    /** Same idle glow for every battery, so GOOD and EVIL batteries can never be told apart. */
    public static final AzCommand GLOW = AzCommand.create("base_controller", "glow", AzPlayBehaviors.LOOP);

    @Override
    public void registerControllers(AzAnimationControllerContainer<ItemStack> container) {
        container.add(AzAnimationController.builder(this, "base_controller").build());
    }

    @Override
    public Identifier getAnimationLocation(ItemStack animatable) {
        return ANIMATIONS;
    }
}
