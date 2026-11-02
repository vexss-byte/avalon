package dev.avalon.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class AvalonEntities {
    /** Large box so the gate is never frustum-culled (its rings are ~13 blocks wide). */
    public static final EntityType<PortalGateEntity> PORTAL_GATE = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of("avalon", "portal_gate"),
            FabricEntityTypeBuilder.<PortalGateEntity>create(SpawnGroup.MISC, PortalGateEntity::new)
                    .dimensions(EntityDimensions.fixed(13.0f, 13.0f))
                    .trackRangeBlocks(160)
                    .trackedUpdateRate(20)
                    .build());

    private AvalonEntities() {}

    public static void init() {
        // class loading registers the entity type
    }
}
