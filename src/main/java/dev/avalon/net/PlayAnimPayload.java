package dev.avalon.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Server -> client: play (or stop, when animation is empty) a Player Animation Library animation on a player. */
public final class PlayAnimPayload implements CustomPayload {
    public static final Id<PlayAnimPayload> ID = new Id<>(Identifier.of("avalon", "play_anim"));
    public static final PacketCodec<RegistryByteBuf, PlayAnimPayload> CODEC =
            PacketCodec.of(PlayAnimPayload::write, PlayAnimPayload::new);

    public final UUID player;
    public final String animation;

    public PlayAnimPayload(UUID player, String animation) {
        this.player = player;
        this.animation = animation;
    }

    private PlayAnimPayload(RegistryByteBuf buf) {
        this.player = buf.readUuid();
        this.animation = buf.readString();
    }

    private void write(RegistryByteBuf buf) {
        buf.writeUuid(player);
        buf.writeString(animation);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
