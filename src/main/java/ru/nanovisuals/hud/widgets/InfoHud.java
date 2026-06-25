package ru.nanovisuals.hud.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import ru.nanovisuals.hud.HudWidget;
import ru.nanovisuals.utils.ColorUtil;
import ru.nanovisuals.utils.GlassRender;
import ru.nanovisuals.utils.MathUtil;
import ru.nanovisuals.utils.Spring;

public class InfoHud extends HudWidget {

    private final Spring speedSpring = new Spring(120f, 18f);
    private double prevX, prevZ;
    private long prevNs;
    private boolean primed;

    public InfoHud() {
        super("InfoHud", "Info Panel", 14f, 0f);
        speedSpring.snap(0f);
    }

    @Override
    public float width()  { return 140f; }
    @Override
    public float height() { return 54f; }

    @Override
    public void renderHud(DrawContext ctx, float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;

        if (anchorY <= 0f) anchorY = mc.getWindow().getScaledHeight() - height() - 14f;

        long now = System.nanoTime();
        if (p != null) {
            double dx = p.getX() - prevX;
            double dz = p.getZ() - prevZ;
            float dtSec = primed ? Math.max(1e-3f, (now - prevNs) / 1_000_000_000f) : 1f / 20f;
            double inst = Math.hypot(dx, dz) / dtSec;
            speedSpring.setTarget((float) inst);
            speedSpring.update(dtSec);
            prevX = p.getX();
            prevZ = p.getZ();
            primed = true;
        }
        prevNs = now;

        float x = drawX();
        float y = drawY();
        float w = width();
        float h = height();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + w / 2f, y + h / 2f, 0);
        ctx.getMatrices().multiply(
                new org.joml.Quaternionf().rotateZ((float) Math.toRadians(tiltDeg() * 0.5f)));
        ctx.getMatrices().translate(-(x + w / 2f), -(y + h / 2f), 0);

        GlassRender.glassPanel(ctx, x, y, w, h, 12f);

        int fps = mc.getCurrentFps();
        int ping = pingOf(mc, p);
        float speed = speedSpring.position;

        int labelCol = ColorUtil.rgba(170, 175, 195, 200);
        int valCol = ColorUtil.rgba(235, 240, 250, 235);
        int accent = fpsColor(fps);

        ctx.drawText(mc.textRenderer, "FPS",   (int) (x + 12f), (int) (y + 8f),  labelCol, false);
        ctx.drawText(mc.textRenderer, "Ping",  (int) (x + 12f), (int) (y + 22f), labelCol, false);
        ctx.drawText(mc.textRenderer, "Speed", (int) (x + 12f), (int) (y + 36f), labelCol, false);

        String fpsTxt = String.valueOf(fps);
        String pingTxt = ping >= 0 ? ping + "ms" : "--";
        String spdTxt = String.format("%.1f m/s", speed);

        int fpsW = mc.textRenderer.getWidth(fpsTxt);
        int pingW = mc.textRenderer.getWidth(pingTxt);
        int spdW = mc.textRenderer.getWidth(spdTxt);

        ctx.drawText(mc.textRenderer, fpsTxt,
                (int) (x + w - 12f - fpsW), (int) (y + 8f), accent, false);
        ctx.drawText(mc.textRenderer, pingTxt,
                (int) (x + w - 12f - pingW), (int) (y + 22f), pingColor(ping), false);
        ctx.drawText(mc.textRenderer, spdTxt,
                (int) (x + w - 12f - spdW), (int) (y + 36f), valCol, false);

        float u = MathUtil.clamp(speed / 8f, 0f, 1f);
        float trackY = y + h - 6f;
        int back = ColorUtil.rgba(45, 50, 65, 200);
        GlassRender.fillRounded(ctx, x + 12f, trackY, w - 24f, 2f, 1f, back, back);
        int fill = ColorUtil.rgba(120, 195, 255, 230);
        int fillEnd = ColorUtil.rgba(120, 195, 255, 110);
        GlassRender.fillRounded(ctx, x + 12f, trackY, (w - 24f) * u, 2f, 1f, fill, fillEnd);

        ctx.getMatrices().pop();
    }

    private static int pingOf(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.getNetworkHandler() == null || p == null) return -1;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    private static int fpsColor(int fps) {
        if (fps >= 90) return ColorUtil.rgba(120, 220, 150, 235);
        if (fps >= 45) return ColorUtil.rgba(245, 200, 80, 235);
        return ColorUtil.rgba(245, 100, 90, 235);
    }

    private static int pingColor(int ping) {
        if (ping < 0) return ColorUtil.rgba(160, 165, 180, 200);
        if (ping < 60) return ColorUtil.rgba(120, 220, 150, 235);
        if (ping < 150) return ColorUtil.rgba(245, 200, 80, 235);
        return ColorUtil.rgba(245, 100, 90, 235);
    }
}
