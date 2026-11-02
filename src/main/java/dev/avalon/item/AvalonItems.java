package dev.avalon.item;

import mod.azure.azurelib.common.animation.cache.AzIdentityRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class AvalonItems {
    public static final Item BATTERY = register("battery", new BatteryItem(new Item.Settings().maxCount(1)));
    public static final Item TEAM_BOOK = register("team_book", new TeamBookItem(new Item.Settings().maxCount(1)));
    public static final Item LADY_ORB = register("lady_orb", new LadyItem(new Item.Settings().maxCount(1)));

    private AvalonItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of("avalon", name), item);
    }

    public static void init() {
        // AzureLib needs an identity for animated items so trigger animations reach the right stack.
        AzIdentityRegistry.register(BATTERY);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(BATTERY);
            entries.add(TEAM_BOOK);
            entries.add(LADY_ORB);
        });
    }
}
