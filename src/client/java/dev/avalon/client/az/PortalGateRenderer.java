package dev.avalon.client.az;

import dev.avalon.entity.PortalGateEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class PortalGateRenderer extends AzEntityRenderer<PortalGateEntity> {
    private static final Identifier GEO = Identifier.of("avalon", "geo/entity/portal_gate.geo.json");
    private static final Identifier TEX = Identifier.of("avalon", "textures/entity/portal_gate.png");

    public PortalGateRenderer(EntityRendererFactory.Context context) {
        super(AzEntityRendererConfig.<PortalGateEntity>builder(GEO, TEX)
                .setAnimatorProvider(PortalGateAnimator::new)
                .setShadowRadius(0.0f)
                .build(), context);
    }
}
