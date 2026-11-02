package dev.avalon.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Shared look for all Avalon screens: dark teal panel, gold border, pulsing cyan accent. No blur. */
public abstract class AvalonScreen extends Screen {
    protected static final int BG = 0xE60B1A22;
    protected static final int PANEL = 0xF0102830;
    protected static final int GOLD = 0xFFF0C440;
    protected static final int CYAN = 0xFF24C4E0;
    protected static final int TEXT = 0xFFE8FBFF;
    protected static final int MUTED = 0xFF8FB4BD;

    private final long openedAt = System.currentTimeMillis();
    protected final long endAtMs;

    protected AvalonScreen(Text title, int seconds) {
        super(title);
        this.endAtMs = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
    }

    protected int secondsLeft() {
        return (int) Math.max(0, (endAtMs - System.currentTimeMillis() + 999) / 1000);
    }

    protected String timeText() {
        int s = secondsLeft();
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Plain translucent dim (the default vanilla background blurs the world; we do not want that). */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x99000010);
    }

    protected void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        double pulse = 0.5 + 0.5 * Math.sin((System.currentTimeMillis() - openedAt) / 350.0);
        int a = (int) (110 + 120 * pulse);
        int glow = (a << 24) | 0x0024C4E0;
        ctx.fill(x - 3, y - 3, x + w + 3, y + h + 3, glow);
        ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, GOLD);
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 22, BG);
        ctx.fill(x, y + 22, x + w, y + 23, CYAN);
    }

    protected static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }
}
