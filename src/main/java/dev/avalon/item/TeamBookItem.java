package dev.avalon.item;

import dev.avalon.game.GameManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** The King right-clicks this book to open the team selection screen. */
public class TeamBookItem extends Item {
    public TeamBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            GameManager gm = GameManager.get();
            if (gm != null) gm.onTeamBookUse(sp);
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
}
