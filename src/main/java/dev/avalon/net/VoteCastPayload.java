package dev.avalon.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: approve / reject. */
public final class VoteCastPayload implements CustomPayload {
    public static final Id<VoteCastPayload> ID = new Id<>(Identifier.of("avalon", "vote_cast"));
    public static final PacketCodec<RegistryByteBuf, VoteCastPayload> CODEC =
            PacketCodec.of(VoteCastPayload::write, VoteCastPayload::new);

    public final boolean approve;

    public VoteCastPayload(boolean approve) {
        this.approve = approve;
    }

    private VoteCastPayload(RegistryByteBuf buf) {
        this.approve = buf.readBoolean();
    }

    private void write(RegistryByteBuf buf) {
        buf.writeBoolean(approve);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
