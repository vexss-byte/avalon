package dev.avalon.client;

import dev.avalon.net.OpenSelectPayload;
import dev.avalon.net.SelectResultPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Player picker used for team selection, ejecting a suspect, Lady of the Lake and the Assassin. */
public class SelectScreen extends AvalonScreen {
    private final OpenSelectPayload data;
    private final Set<UUID> selected = new LinkedHashSet<>();
    private final List<AvalonButton> playerButtons = new ArrayList<>();
    private AvalonButton confirm;
    private boolean sent;

    public SelectScreen(OpenSelectPayload data) {
        super(Text.literal(data.title), data.seconds);
        this.data = data;
    }

    private int rows() { return (data.ids.size() + 1) / 2; }
    private int panelW() { return 320; }
    private int panelH() { return 112 + rows() * 26 + (data.allowSkip ? 30 : 0); }

    @Override
    protected void init() {
        playerButtons.clear();
        int pw = panelW();
        int ph = panelH();
        int x = (this.width - pw) / 2;
        int y = (this.height - ph) / 2;
        for (int i = 0; i < data.ids.size(); i++) {
            final int idx = i;
            int bx = x + 15 + (i % 2) * 150;
            int by = y + 62 + (i / 2) * 26;
            AvalonButton b = new AvalonButton(bx, by, 140, 22, Text.literal(data.names.get(i)), 0xFF16404A,
                    btn -> toggle(idx));
            playerButtons.add(b);
            addDrawableChild(b);
        }
        int cy = y + ph - 36 - (data.allowSkip ? 30 : 0);
        confirm = new AvalonButton(x + 60, cy, 200, 24, Text.literal("KONFIRMASI"), 0xFF157A5A, b -> send(new ArrayList<>(selected)));
        confirm.active = false;
        addDrawableChild(confirm);
        if (data.allowSkip) {
            addDrawableChild(new AvalonButton(x + 60, cy + 30, 200, 22, Text.literal("SKIP / TIDAK ADA"),
                    0xFF4A4A55, b -> send(new ArrayList<>())));
        }
    }

    private void toggle(int idx) {
        UUID id = data.ids.get(idx);
        if (selected.contains(id)) {
            selected.remove(id);
        } else if (data.limit == 1) {
            selected.clear();
            selected.add(id);
        } else if (selected.size() < data.limit) {
            selected.add(id);
        }
        for (int i = 0; i < playerButtons.size(); i++) {
            playerButtons.get(i).setSelected(selected.contains(data.ids.get(i)));
        }
        confirm.active = selected.size() == data.limit;
    }

    private void send(List<UUID> chosen) {
        if (sent) return;
        sent = true;
        ClientPlayNetworking.send(new SelectResultPayload(data.mode, chosen));
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int pw = panelW();
        int ph = panelH();
        int x = (this.width - pw) / 2;
        int y = (this.height - ph) / 2;
        drawPanel(ctx, x, y, pw, ph);
        var tr = this.textRenderer;
        ctx.drawCenteredTextWithShadow(tr, Text.literal(data.title.toUpperCase()), x + pw / 2, y + 7, GOLD);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(data.subtitle), x + pw / 2, y + 30, TEXT);
        ctx.drawCenteredTextWithShadow(tr,
                Text.literal("Dipilih " + selected.size() + "/" + data.limit + "   |   Sisa waktu " + timeText()),
                x + pw / 2, y + 46, MUTED);
        super.render(ctx, mouseX, mouseY, delta);
    }
}
