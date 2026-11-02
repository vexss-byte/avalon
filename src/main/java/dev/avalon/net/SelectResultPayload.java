package dev.avalon.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Client -> server: the player's choice. The server re-validates everything. */
public final class SelectResultPayload implements CustomPayload {
    public static final Id<SelectResultPayload> ID = new Id<>(Identifier.of("avalon", "select_result"));
    public static final PacketCodec<RegistryByteBuf, SelectResultPayload> CODEC =
            PacketCodec.of(SelectResultPayload::write, SelectResultPayload::new);

    public final int mode;
    public final List<UUID> chosen;

    public SelectResultPayload(int mode, List<UUID> chosen) {
        this.mode = mode;
        this.chosen = chosen;
    }

    private SelectResultPayload(RegistryByteBuf buf) {
        this.mode = buf.readVarInt();
        int n = Math.min(buf.readVarInt(), 16);
        this.chosen = new ArrayList<>(n);
        for (int i = 0; i < n; i++) chosen.add(buf.readUuid());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(mode);
        buf.writeVarInt(chosen.size());
        for (UUID id : chosen) buf.writeUuid(id);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
