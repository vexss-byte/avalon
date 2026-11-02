package dev.avalon.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server -> client: close any open Avalon screen (phase ended). */
public final class CloseScreenPayload implements CustomPayload {
    public static final Id<CloseScreenPayload> ID = new Id<>(Identifier.of("avalon", "close_screen"));
    public static final CloseScreenPayload INSTANCE = new CloseScreenPayload();
    public static final PacketCodec<RegistryByteBuf, CloseScreenPayload> CODEC = PacketCodec.unit(INSTANCE);

    private CloseScreenPayload() {}

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
