package dev.avalon.client;

import dev.avalon.net.OpenVotePayload;
import dev.avalon.net.VoteCastPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Private team voting screen: big Setuju / Tolak buttons, proposed team and a countdown. */
public class VoteScreen extends AvalonScreen {
    private final OpenVotePayload data;
    private boolean sent;

    public VoteScreen(OpenVotePayload data) {
        super(Text.literal("Voting Tim"), data.seconds);
        this.data = data;
    }

    private int panelW() { return 320; }
    private int panelH() { return 120 + data.members.size() * 14; }

    @Override
    protected void init() {
        int pw = panelW();
        int ph = panelH();
        int x = (this.width - pw) / 2;
        int y = (this.height - ph) / 2;
        int by = y + ph - 40;
        addDrawableChild(new AvalonButton(x + 20, by, 130, 26, Text.literal("SETUJU"), 0xFF157A5A, b -> cast(true)));
        addDrawableChild(new AvalonButton(x + pw - 150, by, 130, 26, Text.literal("TOLAK"), 0xFF8A2A3A, b -> cast(false)));
    }

    private void cast(boolean approve) {
        if (sent) return;
        sent = true;
        ClientPlayNetworking.send(new VoteCastPayload(approve));
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
        ctx.drawCenteredTextWithShadow(tr, Text.literal("VOTING TIM - PILAR " + data.pillar), x + pw / 2, y + 7, GOLD);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("Raja: " + data.kingName), x + pw / 2, y + 32, TEXT);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("Anggota tim yang diusulkan:"), x + pw / 2, y + 48, MUTED);
        int ly = y + 62;
        for (String m : data.members) {
            ctx.drawCenteredTextWithShadow(tr, Text.literal("\u25C6 " + m), x + pw / 2, ly, CYAN);
            ly += 14;
        }
        ctx.drawCenteredTextWithShadow(tr, Text.literal("Sisa waktu " + timeText() + "  |  suaramu rahasia"),
                x + pw / 2, y + ph - 60, MUTED);
        super.render(ctx, mouseX, mouseY, delta);
    }
}
