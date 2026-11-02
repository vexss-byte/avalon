package dev.avalon.item;

import dev.avalon.game.GameManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.function.BiConsumer;

/**
 * Every team member gets the SAME battery item from the depot. Right click toggles the secret
 * sabotage flag (EVIL only, validated on the server). The result is always PASS, so GOOD and EVIL
 * look identical to everybody else (no swing animation, no sound, no particle).
 */
public class BatteryItem extends Item {
    /** Set by the client initializer (AzureLib idle animation). Null on a dedicated server. */
    public static BiConsumer<Entity, ItemStack> CLIENT_HOOK;

    public BatteryItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient && selected && CLIENT_HOOK != null && entity.age % 60 == 1) {
            CLIENT_HOOK.accept(entity, stack);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            GameManager gm = GameManager.get();
            if (gm != null) gm.sabotage.toggleArm(gm, sp);
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
}
