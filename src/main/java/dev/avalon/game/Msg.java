package dev.avalon.game;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

/** Small helpers for chat / title / action bar messages. */
public final class Msg {
    private Msg() {}

    public static Text prefix() {
        return Text.literal("[Avalon] ").formatted(Formatting.GOLD);
    }

    public static Text line(String s, Formatting color) {
        return prefix().copy().append(Text.literal(s).formatted(color));
    }

    public static void chat(ServerPlayerEntity p, String s, Formatting color) {
        p.sendMessage(line(s, color), false);
    }

    public static void chat(ServerPlayerEntity p, Text t) {
        p.sendMessage(t, false);
    }

    public static void chatAll(Collection<ServerPlayerEntity> ps, String s, Formatting color) {
        Text t = line(s, color);
        for (ServerPlayerEntity p : ps) p.sendMessage(t, false);
    }

    public static void action(ServerPlayerEntity p, String s, Formatting color) {
        p.sendMessage(Text.literal(s).formatted(color), true);
    }

    public static void title(ServerPlayerEntity p, String title, Formatting tc, String sub, Formatting sc,
                             int fadeIn, int stay, int fadeOut) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sub).formatted(sc)));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(tc, Formatting.BOLD)));
    }

    public static void titleAll(Collection<ServerPlayerEntity> ps, String title, Formatting tc, String sub,
                                Formatting sc, int fadeIn, int stay, int fadeOut) {
        for (ServerPlayerEntity p : ps) title(p, title, tc, sub, sc, fadeIn, stay, fadeOut);
    }
}
