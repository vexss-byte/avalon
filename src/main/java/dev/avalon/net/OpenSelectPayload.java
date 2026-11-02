package dev.avalon.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server -> client: open the player-selection screen. Carries only public data (names). */
public final class OpenSelectPayload implements CustomPayload {
    public static final Id<OpenSelectPayload> ID = new Id<>(Identifier.of("avalon", "open_select"));
    public static final PacketCodec<RegistryByteBuf, OpenSelectPayload> CODEC =
            PacketCodec.of(OpenSelectPayload::write, OpenSelectPayload::new);

    public static final int MODE_TEAM = 0;
    public static final int MODE_EJECT = 1;
    public static final int MODE_LADY = 2;
    public static final int MODE_ASSASSIN = 3;

    public final int mode;
    public final String title;
    public final String subtitle;
    public final int limit;
    public final boolean allowSkip;
    public final int seconds;
    public final List<UUID> ids;
    public final List<String> names;

    public OpenSelectPayload(int mode, String title, String subtitle, int limit, boolean allowSkip, int seconds,
                             List<UUID> ids, List<String> names) {
        this.mode = mode;
        this.title = title;
        this.subtitle = subtitle;
        this.limit = limit;
        this.allowSkip = allowSkip;
        this.seconds = seconds;
        this.ids = ids;
        this.names = names;
    }

    private OpenSelectPayload(RegistryByteBuf buf) {
        this.mode = buf.readVarInt();
        this.title = buf.readString();
        this.subtitle = buf.readString();
        this.limit = buf.readVarInt();
        this.allowSkip = buf.readBoolean();
        this.seconds = buf.readVarInt();
        int n = buf.readVarInt();
        this.ids = new ArrayList<>(n);
        this.names = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ids.add(buf.readUuid());
            names.add(buf.readString());
        }
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(mode);
        buf.writeString(title);
        buf.writeString(subtitle);
        buf.writeVarInt(limit);
        buf.writeBoolean(allowSkip);
        buf.writeVarInt(seconds);
        buf.writeVarInt(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            buf.writeUuid(ids.get(i));
            buf.writeString(names.get(i));
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
