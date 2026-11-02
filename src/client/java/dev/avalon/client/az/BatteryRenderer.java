package dev.avalon.client.az;

import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import net.minecraft.util.Identifier;

public class BatteryRenderer extends AzItemRenderer {
    private static final Identifier GEO = Identifier.of("avalon", "geo/item/battery.geo.json");
    private static final Identifier TEX = Identifier.of("avalon", "textures/item/battery.png");

    public BatteryRenderer() {
        super(AzItemRendererConfig.builder(GEO, TEX)
                .setAnimatorProvider(BatteryAnimator::new)
                .build());
    }
}
