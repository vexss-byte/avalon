package dev.avalon.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Server -> client: open the private voting screen for a proposed team. */
public final class OpenVotePayload implements CustomPayload {
    public static final Id<OpenVotePayload> ID = new Id<>(Identifier.of("avalon", "open_vote"));
    public static final PacketCodec<RegistryByteBuf, OpenVotePayload> CODEC =
            PacketCodec.of(OpenVotePayload::write, OpenVotePayload::new);

    public final String kingName;
    public final int pillar;
    public final int seconds;
    public final List<String> members;

    public OpenVotePayload(String kingName, int pillar, int seconds, List<String> members) {
        this.kingName = kingName;
        this.pillar = pillar;
        this.seconds = seconds;
        this.members = members;
    }

    private OpenVotePayload(RegistryByteBuf buf) {
        this.kingName = buf.readString();
        this.pillar = buf.readVarInt();
        this.seconds = buf.readVarInt();
        int n = Math.min(buf.readVarInt(), 16);
        this.members = new ArrayList<>(n);
        for (int i = 0; i < n; i++) members.add(buf.readString());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeString(kingName);
        buf.writeVarInt(pillar);
        buf.writeVarInt(seconds);
        buf.writeVarInt(members.size());
        for (String m : members) buf.writeString(m);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
