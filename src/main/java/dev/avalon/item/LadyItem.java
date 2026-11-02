package dev.avalon.item;

import dev.avalon.game.GameManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Lady of the Lake: right click to choose a player whose faction you inspect. */
public class LadyItem extends Item {
    public LadyItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            GameManager gm = GameManager.get();
            if (gm != null) gm.onLadyUse(sp);
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
}
