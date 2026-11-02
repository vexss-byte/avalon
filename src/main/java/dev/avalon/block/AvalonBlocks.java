package dev.avalon.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class AvalonBlocks {
    public static final BatterySocketBlock BATTERY_SOCKET = register("battery_socket",
            new BatterySocketBlock(AbstractBlock.Settings.create()
                    .strength(-1.0f, 3600000.0f)
                    .dropsNothing()
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                    .luminance(s -> s.get(BatterySocketBlock.INSTALLED) ? 11 : 4)));

    public static final DepotBlock DEPOT = register("depot",
            new DepotBlock(AbstractBlock.Settings.create()
                    .strength(-1.0f, 3600000.0f)
                    .dropsNothing()
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(s -> 8)));

    private AvalonBlocks() {}

    private static <T extends Block> T register(String name, T block) {
        Identifier id = Identifier.of("avalon", name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void init() {
        // class loading registers the blocks
    }
}
