package dev.avalon.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Flat, themed button with hover glow and an optional "selected" look. */
public class AvalonButton extends ButtonWidget {
    private final int baseColor;
    private boolean selected;

    public AvalonButton(int x, int y, int w, int h, Text label, int baseColor, PressAction action) {
        super(x, y, w, h, label, action, DEFAULT_NARRATION_SUPPLIER);
        this.baseColor = baseColor;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelectedState() {
        return selected;
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int fill = baseColor;
        if (!this.active) fill = 0xFF2A3A40;
        else if (selected) fill = 0xFF1F9BB5;
        else if (isHovered()) fill = brighten(baseColor);
        int border = selected ? 0xFFF0C440 : (isHovered() && this.active ? 0xFF8FEAF8 : 0xFF3C7480);
        ctx.fill(x, y, x + w, y + h, border);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 3, 0x30FFFFFF);
        int color = this.active ? 0xFFFFFFFF : 0xFF7A8C90;
        var tr = mc().textRenderer;
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2, color);
    }

    private static int brighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((c >> 8) & 0xFF) + 40);
        int b = Math.min(255, (c & 0xFF) + 40);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static net.minecraft.client.MinecraftClient mc() {
        return net.minecraft.client.MinecraftClient.getInstance();
    }
}
