package ru.nanovisuals.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;
import ru.nanovisuals.modules.ModuleManager;
import ru.nanovisuals.modules.settings.BooleanSetting;
import ru.nanovisuals.modules.settings.NumberSetting;
import ru.nanovisuals.modules.settings.Setting;
import ru.nanovisuals.utils.ColorUtil;
import ru.nanovisuals.utils.Easing;
import ru.nanovisuals.utils.GlassRender;
import ru.nanovisuals.utils.MathUtil;
import ru.nanovisuals.utils.Spring;

public class ClickGUI extends Screen {

    private static final float PANEL_W      = 150f;
    private static final float HEADER_H     = 26f;
    private static final float ROW_H        = 18f;
    private static final float SETTING_H    = 16f;
    private static final float CORNER_R     = 14f;
    private static final float PANEL_GAP    = 14f;
    private static final float SCROLL_FRICTION = 12f;
    private static final float OVERSCROLL_RESIST = 0.35f;

    private static final int ACCENT = ColorUtil.rgba(120, 195, 255, 255);
    private static final int TEXT_BRIGHT = ColorUtil.rgba(240, 240, 250, 255);
    private static final int TEXT_DIM = ColorUtil.rgba(155, 160, 175, 255);
    private static final int BACKDROP = ColorUtil.rgba(0, 0, 0, 120);

    private final Map<ModuleCategory, Panel> panelMap = new EnumMap<>(ModuleCategory.class);
    private final List<Panel> panels = new ArrayList<>();

    private Panel dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private long lastFrameNs;

    public ClickGUI() {
        super(Text.literal("NanoVisuals"));
    }

    @Override
    protected void init() {
        if (panels.isEmpty()) {
            float x = 20f, y = 36f;
            for (ModuleCategory c : ModuleCategory.values()) {
                List<Module> mods = ModuleManager.getByCategory(c);
                if (mods.isEmpty()) continue;
                Panel p = new Panel(c, mods, x, y);
                panels.add(p);
                panelMap.put(c, p);
                x += PANEL_W + PANEL_GAP;
            }
        }
        lastFrameNs = System.nanoTime();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = (now - lastFrameNs) / 1_000_000_000f;
        lastFrameNs = now;
        if (dt > 0.1f) dt = 0.1f;

        // 1) Backdrop is queued through DrawContext. Force-flush it before any
        //    immediate BufferRenderer draws so the glass panels don't get
        //    repainted over by the end-of-frame flush.
        ctx.fill(0, 0, this.width, this.height, BACKDROP);
        ctx.draw();

        for (Panel p : panels) {
            p.update(dt);
        }

        // 2) Per panel: render all glass geometry immediately, then queue text
        //    and flush it with the body scissor active so labels stay crisp
        //    and clip to the scrollable area.
        for (Panel p : panels) {
            p.renderGlass(ctx, mouseX, mouseY);
            p.renderText(ctx, mouseX, mouseY);
        }

        // 3) Top banner — queued; the engine flushes it at end-of-frame above
        //    everything else.
        renderTopBar(ctx);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTopBar(DrawContext ctx) {
        String banner = "NanoVisuals  \u2022  " + ModuleManager.getModules().size() + " modules";
        ctx.drawText(this.textRenderer, banner, 16, 14, ACCENT, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (!p.contains(mx, my)) continue;

            panels.remove(i);
            panels.add(p);

            if (p.isInHeader(mx, my)) {
                if (button == 0) {
                    if (p.isHeaderToggleClick(mx)) {
                        p.toggleCollapse();
                    } else {
                        dragging = p;
                        dragOffsetX = mx - p.x;
                        dragOffsetY = my - p.y;
                    }
                    return true;
                }
                if (button == 1) {
                    p.toggleCollapse();
                    return true;
                }
            }

            return p.clickBody(mx, my, button);
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null && button == 0) {
            dragging = null;
            return true;
        }
        for (Panel p : panels) p.releaseDrag();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.x = (float) (mx - dragOffsetX);
            dragging.y = (float) (my - dragOffsetY);
            return true;
        }
        for (Panel p : panels) {
            if (p.dragSlider(mx)) return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.contains(mx, my)) {
                p.scrollImpulse((float) (-vAmt * 22f));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        for (Panel p : panels) p.releaseDrag();
        super.removed();
    }

    private static final class Panel {

        final ModuleCategory category;
        final List<Row> rows;
        float x, y;

        boolean collapsed;
        final Spring openness = new Spring(170f, 19f);
        final Spring height   = new Spring(170f, 19f);

        float scroll;
        float scrollVel;
        float maxScroll;

        ActiveSlider activeSlider;

        Panel(ModuleCategory category, List<Module> mods, float x, float y) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.rows = new ArrayList<>(mods.size());
            for (Module m : mods) rows.add(new Row(m));
            openness.snap(1f);
            height.snap(targetBodyHeight());
        }

        float targetBodyHeight() {
            if (collapsed) return 0f;
            float h = 6f;
            for (Row r : rows) {
                h += ROW_H + r.expandSpring.position * r.settingsHeight();
            }
            return Math.min(h + 6f, 260f);
        }

        float contentHeight() {
            float h = 6f;
            for (Row r : rows) {
                h += ROW_H + r.expandSpring.position * r.settingsHeight();
            }
            return h + 6f;
        }

        float renderedBodyHeight() {
            return Math.max(0f, height.position);
        }

        boolean contains(double mx, double my) {
            float total = HEADER_H + renderedBodyHeight();
            return mx >= x && mx <= x + PANEL_W && my >= y && my <= y + total;
        }

        boolean isInHeader(double mx, double my) {
            return mx >= x && mx <= x + PANEL_W && my >= y && my <= y + HEADER_H;
        }

        boolean isHeaderToggleClick(double mx) {
            return mx >= x + PANEL_W - 22f;
        }

        void toggleCollapse() {
            collapsed = !collapsed;
            openness.setTarget(collapsed ? 0f : 1f);
        }

        void update(float dt) {
            for (Row r : rows) r.update(dt);

            float bodyTarget = targetBodyHeight();
            height.setTarget(bodyTarget);
            height.update(dt);

            float content = contentHeight();
            maxScroll = Math.max(0f, content - bodyTarget);

            if (Math.abs(scrollVel) > 0.01f) {
                scroll += scrollVel * dt;
                scrollVel *= (float) Math.exp(-SCROLL_FRICTION * dt);
            }
            float clamped = MathUtil.clamp(scroll, 0f, maxScroll);
            float over = scroll - clamped;
            if (Math.abs(over) > 0.01f) {
                scroll = MathUtil.lerp(scroll, clamped,
                        MathUtil.frameTimeFactor(14f, dt));
                scrollVel *= OVERSCROLL_RESIST;
            }
        }

        void scrollImpulse(float v) {
            scrollVel += v;
        }

        boolean clickBody(double mx, double my, int button) {
            if (collapsed) return false;

            float rowStartY = y + HEADER_H + 4f - scroll;
            float bodyTop = y + HEADER_H;
            float bodyBot = bodyTop + renderedBodyHeight();
            if (my < bodyTop || my > bodyBot) return false;

            for (Row r : rows) {
                float rowTop = rowStartY;
                float rowBot = rowStartY + ROW_H;
                if (mx >= x && mx <= x + PANEL_W && my >= rowTop && my <= rowBot) {
                    if (button == 0) {
                        r.module.toggle();
                        return true;
                    }
                    if (button == 1 && r.module.hasSettings()) {
                        r.expanded = !r.expanded;
                        r.expandSpring.setTarget(r.expanded ? 1f : 0f);
                        return true;
                    }
                    return false;
                }

                if (r.expandSpring.position > 0.02f) {
                    float settingY = rowBot;
                    float scale = r.expandSpring.position;
                    float visibleSettingsH = scale * r.settingsHeight();
                    if (mx >= x && mx <= x + PANEL_W
                            && my >= settingY && my <= settingY + visibleSettingsH) {
                        Setting<?> s = r.settingAt(my - settingY, scale);
                        if (s != null) {
                            handleSettingClick(s, mx, button);
                            return true;
                        }
                    }
                    rowStartY += visibleSettingsH;
                }
                rowStartY += ROW_H;
            }
            return false;
        }

        private void handleSettingClick(Setting<?> s, double mx, int button) {
            if (s instanceof BooleanSetting b && button == 0) {
                b.toggle();
            } else if (s instanceof NumberSetting n && button == 0) {
                float trackX = x + 10f;
                float trackW = PANEL_W - 20f;
                float u = (float) ((mx - trackX) / trackW);
                n.setFromUnit(u);
                activeSlider = new ActiveSlider(n, trackX, trackW);
            }
        }

        boolean dragSlider(double mx) {
            if (activeSlider == null) return false;
            float u = (float) ((mx - activeSlider.trackX) / activeSlider.trackW);
            activeSlider.setting.setFromUnit(u);
            return true;
        }

        void releaseDrag() {
            activeSlider = null;
        }

        /**
         * PHASE 1 — glass geometry only. Drawn via raw BufferRenderer
         * (immediate) so it lands on screen before any queued DrawContext
         * batches flush. No text is queued here.
         */
        void renderGlass(DrawContext ctx, int mouseX, int mouseY) {
            float totalH = HEADER_H + renderedBodyHeight();
            GlassRender.glassPanel(ctx, x, y, PANEL_W, totalH, CORNER_R);
            renderChevron(ctx, x + PANEL_W - 16f, y + HEADER_H / 2f, openness.position);

            int accentBarAlpha = (int) (180f * openness.position + 40f);
            int accentBar = ColorUtil.withAlpha(ACCENT & 0x00FFFFFF, accentBarAlpha);
            GlassRender.fillRounded(ctx, x + 10f, y + HEADER_H - 3f, 18f, 2f, 1f, accentBar, accentBar);

            float bodyTop = y + HEADER_H;
            float bodyH = renderedBodyHeight();
            if (bodyH >= 1f) {
                ctx.enableScissor((int) x, (int) bodyTop, (int) (x + PANEL_W), (int) (bodyTop + bodyH));

                float rowY = bodyTop + 4f - scroll;
                for (Row r : rows) {
                    updateRowHover(r, rowY, mouseX, mouseY);
                    renderRowGlass(ctx, r, rowY);
                    rowY += ROW_H;
                    if (r.expandSpring.position > 0.001f) {
                        float h = r.expandSpring.position * r.settingsHeight();
                        renderSettingsGlass(ctx, r, rowY, h);
                        rowY += h;
                    }
                }

                ctx.disableScissor();
            }

            if (maxScroll > 0.5f) {
                renderScrollIndicator(ctx, bodyTop, bodyH);
            }
        }

        /**
         * PHASE 2 — text and icons. All queued through DrawContext. We force a
         * flush between header (no scissor) and body (scrolled, scissored) so
         * labels render crisp and clip correctly to the panel body.
         */
        void renderText(DrawContext ctx, int mouseX, int mouseY) {
            MinecraftClient mc = MinecraftClient.getInstance();

            // Header text — no scissor needed
            ctx.drawText(mc.textRenderer, category.getDisplayName(),
                    (int) (x + 12f), (int) (y + 9f), TEXT_BRIGHT, false);
            ctx.draw();

            float bodyTop = y + HEADER_H;
            float bodyH = renderedBodyHeight();
            if (bodyH < 1f) return;

            ctx.enableScissor((int) x, (int) bodyTop, (int) (x + PANEL_W), (int) (bodyTop + bodyH));

            float rowY = bodyTop + 4f - scroll;
            for (Row r : rows) {
                renderRowText(ctx, mc, r, rowY);
                rowY += ROW_H;
                if (r.expandSpring.position > 0.001f) {
                    float h = r.expandSpring.position * r.settingsHeight();
                    renderSettingsText(ctx, mc, r, rowY, h);
                    rowY += h;
                }
            }

            // Flush body text WHILE scissor is still active so it clips to the body.
            ctx.draw();
            ctx.disableScissor();
        }

        private void updateRowHover(Row r, float rowY, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX <= x + PANEL_W
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;
            float hoverT = MathUtil.lerp(r.hoverGlow.position, hovered ? 1f : 0f, 0.3f);
            r.hoverGlow.snap(hoverT);
        }

        private void renderRowGlass(DrawContext ctx, Row r, float rowY) {
            float glow = r.hoverGlow.position;
            if (r.module.isEnabled()) {
                int c1 = ColorUtil.withAlpha(ACCENT & 0x00FFFFFF, 40);
                int c0 = ColorUtil.withAlpha(ACCENT & 0x00FFFFFF, 0);
                GlassRender.fillRounded(ctx, x + 4f, rowY + 1f, PANEL_W - 8f, ROW_H - 2f, 6f, c1, c0);
            } else if (glow > 0.02f) {
                int hov = ColorUtil.rgba(255, 255, 255, (int) (18f * glow + 4f));
                GlassRender.fillRounded(ctx, x + 4f, rowY + 1f, PANEL_W - 8f, ROW_H - 2f, 6f, hov, hov);
            }

            float dotX = x + PANEL_W - 14f;
            float dotY = rowY + ROW_H / 2f;
            int dotColor = r.module.isEnabled() ? ACCENT : ColorUtil.rgba(90, 95, 110, 220);
            GlassRender.fillRounded(ctx, dotX - 3f, dotY - 3f, 6f, 6f, 3f, dotColor, dotColor);
            if (r.module.isEnabled()) {
                int halo = ColorUtil.multAlpha(ACCENT, 0.4f);
                int halo0 = ColorUtil.multAlpha(ACCENT, 0f);
                GlassRender.fillRounded(ctx, dotX - 6f, dotY - 6f, 12f, 12f, 6f, halo, halo0);
            }

            if (r.module.hasSettings()) {
                int gear = ColorUtil.rgba(180, 185, 200, 160);
                GlassRender.fillRounded(ctx, x + PANEL_W - 24f, rowY + ROW_H / 2f - 1f, 3f, 2f, 1f, gear, gear);
            }
        }

        private void renderRowText(DrawContext ctx, MinecraftClient mc, Row r, float rowY) {
            int textColor = r.module.isEnabled() ? TEXT_BRIGHT : TEXT_DIM;
            ctx.drawText(mc.textRenderer, r.module.getDisplayName(),
                    (int) (x + 12f), (int) (rowY + (ROW_H - mc.textRenderer.fontHeight) / 2f + 1f),
                    textColor, false);
        }

        private void renderSettingsGlass(DrawContext ctx, Row r, float sy, float visibleH) {
            int bg  = ColorUtil.rgba(255, 255, 255, 10);
            int bg0 = ColorUtil.rgba(255, 255, 255, 0);
            GlassRender.fillRounded(ctx, x + 6f, sy, PANEL_W - 12f, visibleH, 6f, bg, bg0);

            float t = sy + 4f;
            float scale = r.expandSpring.position;
            int alpha = (int) (255f * MathUtil.clamp(scale, 0f, 1f));

            for (Setting<?> s : r.module.getSettings()) {
                if (t + SETTING_H < sy || t > sy + visibleH) {
                    t += SETTING_H;
                    continue;
                }
                if (s instanceof BooleanSetting b) {
                    float tx = x + PANEL_W - 30f;
                    float ty2 = t + 3f;
                    int trackOff = ColorUtil.rgba(60, 65, 80, alpha);
                    int trackOn  = ColorUtil.withAlpha(ACCENT & 0x00FFFFFF, alpha);
                    int track = b.getValue() ? trackOn : trackOff;
                    GlassRender.fillRounded(ctx, tx, ty2, 18f, 8f, 4f, track, track);
                    float knobX = b.getValue() ? tx + 11f : tx + 1f;
                    int knob = ColorUtil.rgba(245, 245, 250, alpha);
                    GlassRender.fillRounded(ctx, knobX, ty2 + 1f, 6f, 6f, 3f, knob, knob);
                } else if (s instanceof NumberSetting n) {
                    float trackX = x + 14f;
                    float trackY = t + 12f;
                    float trackW = PANEL_W - 28f;
                    int back = ColorUtil.rgba(50, 55, 70, alpha);
                    GlassRender.fillRounded(ctx, trackX, trackY, trackW, 2f, 1f, back, back);
                    float fillW = trackW * n.toUnit();
                    int fill = ColorUtil.withAlpha(ACCENT & 0x00FFFFFF, alpha);
                    int fillEnd = ColorUtil.withAlpha(ACCENT & 0x00FFFFFF, alpha / 2);
                    GlassRender.fillRounded(ctx, trackX, trackY, fillW, 2f, 1f, fill, fillEnd);
                }
                t += SETTING_H;
            }
        }

        private void renderSettingsText(DrawContext ctx, MinecraftClient mc, Row r,
                                       float sy, float visibleH) {
            float t = sy + 4f;
            float scale = r.expandSpring.position;
            int alpha = (int) (255f * MathUtil.clamp(scale, 0f, 1f));

            for (Setting<?> s : r.module.getSettings()) {
                if (t + SETTING_H < sy || t > sy + visibleH) {
                    t += SETTING_H;
                    continue;
                }
                int label = ColorUtil.withAlpha(TEXT_DIM & 0x00FFFFFF, alpha);
                if (s instanceof BooleanSetting) {
                    ctx.drawText(mc.textRenderer, s.getName(),
                            (int) (x + 14f), (int) (t + 4f), label, false);
                } else if (s instanceof NumberSetting n) {
                    ctx.drawText(mc.textRenderer, s.getName(),
                            (int) (x + 14f), (int) (t + 2f), label, false);
                    String val = formatNumber(n.getValue(), n.getStep());
                    int valColor = ColorUtil.withAlpha(TEXT_BRIGHT & 0x00FFFFFF, alpha);
                    int valW = mc.textRenderer.getWidth(val);
                    ctx.drawText(mc.textRenderer, val,
                            (int) (x + PANEL_W - 14f - valW), (int) (t + 2f), valColor, false);
                }
                t += SETTING_H;
            }
        }

        private static String formatNumber(float v, float step) {
            if (step >= 1f) return String.valueOf((int) v);
            if (step >= 0.1f) return String.format("%.1f", v);
            return String.format("%.2f", v);
        }

        private void renderScrollIndicator(DrawContext ctx, float bodyTop, float bodyH) {
            float content = contentHeight();
            if (content <= bodyH) return;
            float ratio = bodyH / content;
            float thumbH = Math.max(20f, bodyH * ratio);
            float u = maxScroll <= 0f ? 0f : MathUtil.clamp(scroll / maxScroll, 0f, 1f);
            float thumbY = bodyTop + (bodyH - thumbH) * u;
            int color = ColorUtil.rgba(255, 255, 255, 60);
            GlassRender.fillRounded(ctx, x + PANEL_W - 4f, thumbY, 2f, thumbH, 1f, color, color);
        }

        private void renderChevron(DrawContext ctx, float cx, float cy, float openness) {
            float t = Easing.easeOutBack(MathUtil.clamp(openness, 0f, 1f));
            float arm = 4f;
            int color = ColorUtil.rgba(220, 225, 240, 200);
            float tipDrop = (1f - t) * 2f;
            for (int i = 0; i < (int) arm; i++) {
                float yOff = i;
                float xPad = (arm - 1 - i);
                GlassRender.fillRounded(ctx,
                        cx - arm + xPad, cy - 2f + yOff + tipDrop,
                        1.4f, 1.4f, 0.6f, color, color);
                GlassRender.fillRounded(ctx,
                        cx + arm - 1.4f - xPad, cy - 2f + yOff + tipDrop,
                        1.4f, 1.4f, 0.6f, color, color);
            }
        }
    }

    private static final class Row {
        final Module module;
        boolean expanded;
        final Spring expandSpring = new Spring(180f, 20f);
        final Spring hoverGlow = new Spring();

        Row(Module module) {
            this.module = module;
            hoverGlow.snap(0f);
        }

        float settingsHeight() {
            return 6f + module.getSettings().size() * SETTING_H;
        }

        void update(float dt) {
            expandSpring.update(dt);
            hoverGlow.update(dt);
        }

        Setting<?> settingAt(double localY, float scale) {
            float t = 4f * scale;
            List<Setting<?>> all = module.getSettings();
            for (Setting<?> s : all) {
                if (localY >= t && localY <= t + SETTING_H * scale) return s;
                t += SETTING_H * scale;
            }
            return null;
        }
    }

    private record ActiveSlider(NumberSetting setting, float trackX, float trackW) {}
}
