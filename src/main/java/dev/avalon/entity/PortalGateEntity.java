package dev.avalon.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * The 3D intro gate. Purely visual: the model (Blockbench/Bedrock geo) and its spinning rings are
 * rendered on the client by AzureLib (see client/az/PortalGateRenderer). The server only spawns it.
 */
public class PortalGateEntity extends Entity {
    /** Set by the client initializer; keeps AzureLib client classes out of the common code. */
    public static Consumer<PortalGateEntity> CLIENT_TICK;

    public PortalGateEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            if (CLIENT_TICK != null) CLIENT_TICK.accept(this);
        } else if (age > 20 * 60) {
            discard(); // safety net: never leave a gate behind
        }
    }

    @Override
    public boolean shouldRender(double distance) {
        return true;
    }
}
