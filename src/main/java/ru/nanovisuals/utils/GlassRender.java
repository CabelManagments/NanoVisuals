package ru.nanovisuals.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/**
 * Liquid-glass primitives drawn on top of vanilla DrawContext.
 *
 * Real Gaussian blur would require a custom shader / framebuffer pass.
 * We approximate the iOS frosted-glass look with: a translucent dark base,
 * a thin top→bottom highlight stroke and soft drop shadow. The look reads as
 * "glass" without paying for a per-frame separable blur.
 */
public final class GlassRender {

    public static final int GLASS_FILL   = ColorUtil.rgba(15, 15, 15, 90);
    public static final int GLASS_BASE   = ColorUtil.rgba(0, 0, 0, 110);
    public static final int BORDER_TOP   = ColorUtil.rgba(255, 255, 255, 51);
    public static final int BORDER_BOT   = ColorUtil.rgba(255, 255, 255, 12);
    public static final int SHADOW_INNER = ColorUtil.rgba(0, 0, 0, 110);
    public static final int SHADOW_OUTER = ColorUtil.rgba(0, 0, 0, 0);

    private static final int CORNER_SEGMENTS = 10;

    private GlassRender() {}

    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r, float spread) {
        if (spread <= 0f) return;
        int layers = 5;
        for (int i = layers; i >= 1; i--) {
            float t = i / (float) layers;
            float pad = spread * t;
            int alpha = (int) (40f * (1f - t) + 8f);
            int color = ColorUtil.rgba(0, 0, 0, alpha);
            fillRounded(ctx, x - pad, y - pad + spread * 0.4f,
                    w + pad * 2f, h + pad * 2f, r + pad, color, color);
        }
    }

    public static void glassPanel(DrawContext ctx, float x, float y, float w, float h, float r) {
        shadow(ctx, x, y, w, h, r, 10f);
        fillRounded(ctx, x, y, w, h, r, GLASS_BASE, GLASS_BASE);
        fillRounded(ctx, x, y, w, h, r, GLASS_FILL, GLASS_FILL);
        strokeRounded(ctx, x, y, w, h, r, 1f, BORDER_TOP, BORDER_BOT);
    }

    public static void accentGlow(DrawContext ctx, float x, float y, float w, float h, float r, int accent) {
        int a1 = ColorUtil.multAlpha(accent, 0.55f);
        int a0 = ColorUtil.multAlpha(accent, 0.0f);
        fillRounded(ctx, x - 2f, y - 2f, w + 4f, h + 4f, r + 2f, a1, a0);
    }

    public static void fillRounded(DrawContext ctx, float x, float y, float w, float h,
                                   float r, int colorTop, int colorBottom) {
        if (w <= 0f || h <= 0f) return;
        r = Math.min(r, Math.min(w, h) * 0.5f);

        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        prepBlend();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder b = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        int centerColor = ColorUtil.interpolate(colorTop, colorBottom, 0.5f);
        b.vertex(m, cx, cy, 0f).color(centerColor);

        Vert first = null;
        Vert prev = null;
        prev = perimVert(b, m, x + r, y, y, h, colorTop, colorBottom);
        first = prev;
        perimVert(b, m, x + w - r, y, y, h, colorTop, colorBottom);
        arc(b, m, x + w - r, y + r, r, -90f, 0f, y, h, colorTop, colorBottom);
        perimVert(b, m, x + w, y + h - r, y, h, colorTop, colorBottom);
        arc(b, m, x + w - r, y + h - r, r, 0f, 90f, y, h, colorTop, colorBottom);
        perimVert(b, m, x + r, y + h, y, h, colorTop, colorBottom);
        arc(b, m, x + r, y + h - r, r, 90f, 180f, y, h, colorTop, colorBottom);
        perimVert(b, m, x, y + r, y, h, colorTop, colorBottom);
        arc(b, m, x + r, y + r, r, 180f, 270f, y, h, colorTop, colorBottom);
        b.vertex(m, first.x, first.y, 0f).color(first.color);

        BufferRenderer.drawWithGlobalProgram(b.end());
        restoreBlend();
    }

    public static void strokeRounded(DrawContext ctx, float x, float y, float w, float h,
                                     float r, float thickness, int colorTop, int colorBottom) {
        if (w <= 0f || h <= 0f || thickness <= 0f) return;
        r = Math.min(r, Math.min(w, h) * 0.5f);
        float ri = Math.max(0f, r - thickness);
        float ix = x + thickness, iy = y + thickness;
        float iw = w - thickness * 2f, ih = h - thickness * 2f;
        if (iw <= 0f || ih <= 0f) return;

        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        prepBlend();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder b = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        ringCorner(b, m, x + r, y + r, r, ix + ri, iy + ri, ri, 180f, 270f, y, h, colorTop, colorBottom);
        ringCorner(b, m, x + w - r, y + r, r, ix + iw - ri, iy + ri, ri, 270f, 360f, y, h, colorTop, colorBottom);
        ringCorner(b, m, x + w - r, y + h - r, r, ix + iw - ri, iy + ih - ri, ri, 0f, 90f, y, h, colorTop, colorBottom);
        ringCorner(b, m, x + r, y + h - r, r, ix + ri, iy + ih - ri, ri, 90f, 180f, y, h, colorTop, colorBottom);

        float ox0 = x + r, oy0 = y;
        float ix0 = ix + ri, iy0 = iy;
        int co = colorAtY(oy0, y, h, colorTop, colorBottom);
        int ci = colorAtY(iy0, y, h, colorTop, colorBottom);
        b.vertex(m, ox0, oy0, 0f).color(co);
        b.vertex(m, ix0, iy0, 0f).color(ci);

        BufferRenderer.drawWithGlobalProgram(b.end());
        restoreBlend();
    }

    private static void ringCorner(BufferBuilder b, Matrix4f m,
                                   float ocx, float ocy, float or,
                                   float icx, float icy, float ir,
                                   float a0, float a1,
                                   float yBase, float hBase, int top, int bot) {
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            float t = i / (float) CORNER_SEGMENTS;
            float ang = (float) Math.toRadians(a0 + (a1 - a0) * t);
            float cos = (float) Math.cos(ang);
            float sin = (float) Math.sin(ang);
            float ox = ocx + cos * or, oy = ocy + sin * or;
            float ix = icx + cos * ir, iy = icy + sin * ir;
            b.vertex(m, ox, oy, 0f).color(colorAtY(oy, yBase, hBase, top, bot));
            b.vertex(m, ix, iy, 0f).color(colorAtY(iy, yBase, hBase, top, bot));
        }
    }

    private static Vert perimVert(BufferBuilder b, Matrix4f m, float x, float y,
                                  float yBase, float hBase, int top, int bot) {
        int color = colorAtY(y, yBase, hBase, top, bot);
        b.vertex(m, x, y, 0f).color(color);
        return new Vert(x, y, color);
    }

    private static void arc(BufferBuilder b, Matrix4f m, float cx, float cy, float r,
                            float a0, float a1, float yBase, float hBase, int top, int bot) {
        for (int i = 1; i <= CORNER_SEGMENTS; i++) {
            float t = i / (float) CORNER_SEGMENTS;
            float ang = (float) Math.toRadians(a0 + (a1 - a0) * t);
            float vx = cx + (float) Math.cos(ang) * r;
            float vy = cy + (float) Math.sin(ang) * r;
            int color = colorAtY(vy, yBase, hBase, top, bot);
            b.vertex(m, vx, vy, 0f).color(color);
        }
    }

    private static int colorAtY(float y, float yBase, float h, int top, int bot) {
        if (h <= 0f) return top;
        float t = MathUtil.clamp((y - yBase) / h, 0f, 1f);
        return ColorUtil.interpolate(top, bot, t);
    }

    private static void prepBlend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    private static void restoreBlend() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private record Vert(float x, float y, int color) {}
}
