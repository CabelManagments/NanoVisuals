package ru.nanovisuals.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;
import ru.nanovisuals.modules.ModuleManager;

public class ClickGUI extends Screen {

    private static final int PANEL_WIDTH = 110;
    private static final int HEADER_HEIGHT = 18;
    private static final int ROW_HEIGHT = 14;
    private static final int PANEL_GAP = 8;

    private static final int COLOR_BACKDROP = 0x80000000;
    private static final int COLOR_HEADER = 0xFF1E1E2E;
    private static final int COLOR_PANEL = 0xCC141420;
    private static final int COLOR_ROW = 0x00000000;
    private static final int COLOR_ROW_HOVER = 0x335A9EFF;
    private static final int COLOR_ACCENT = 0xFF5A9EFF;
    private static final int COLOR_TEXT = 0xFFE8E8F0;
    private static final int COLOR_TEXT_DIM = 0xFF7A7A8A;

    private final List<Panel> panels = new ArrayList<>();
    private Panel dragging;
    private double dragOffsetX;
    private double dragOffsetY;

    public ClickGUI() {
        super(Text.literal("NanoVisuals"));
    }

    @Override
    protected void init() {
        if (panels.isEmpty()) {
            int x = 20;
            int y = 20;
            for (ModuleCategory category : ModuleCategory.values()) {
                panels.add(new Panel(category, x, y));
                x += PANEL_WIDTH + PANEL_GAP;
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, COLOR_BACKDROP);
        super.render(ctx, mouseX, mouseY, delta);

        String banner = "NanoVisuals \u2014 " + ModuleManager.getModules().size() + " modules";
        ctx.drawText(this.textRenderer, banner, 8, 6, COLOR_ACCENT, true);

        for (Panel panel : panels) {
            panel.render(ctx, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (!panel.contains(mouseX, mouseY)) continue;

            panels.remove(i);
            panels.add(panel);

            if (panel.isInHeader(mouseX, mouseY) && button == 0) {
                dragging = panel;
                dragOffsetX = mouseX - panel.x;
                dragOffsetY = mouseY - panel.y;
                return true;
            }

            return panel.onClick(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null && button == 0) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.x = (int) (mouseX - dragOffsetX);
            dragging.y = (int) (mouseY - dragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class Panel {

        final ModuleCategory category;
        final List<Module> modules;
        int x;
        int y;

        Panel(ModuleCategory category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.modules = ModuleManager.getByCategory(category);
        }

        int width() {
            return PANEL_WIDTH;
        }

        int height() {
            return HEADER_HEIGHT + modules.size() * ROW_HEIGHT + 2;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + width() && my >= y && my <= y + height();
        }

        boolean isInHeader(double mx, double my) {
            return mx >= x && mx <= x + width() && my >= y && my <= y + HEADER_HEIGHT;
        }

        boolean onClick(double mx, double my, int button) {
            if (button != 0) return false;
            int rowY = y + HEADER_HEIGHT;
            for (Module module : modules) {
                if (mx >= x && mx <= x + width() && my >= rowY && my <= rowY + ROW_HEIGHT) {
                    module.toggle();
                    return true;
                }
                rowY += ROW_HEIGHT;
            }
            return false;
        }

        void render(DrawContext ctx, int mouseX, int mouseY) {
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = width();
            int h = height();

            ctx.fill(x, y + HEADER_HEIGHT, x + w, y + h, COLOR_PANEL);
            ctx.fill(x, y, x + w, y + HEADER_HEIGHT, COLOR_HEADER);
            ctx.fill(x, y + HEADER_HEIGHT - 1, x + w, y + HEADER_HEIGHT, COLOR_ACCENT);

            ctx.drawText(mc.textRenderer, category.getDisplayName(),
                    x + 6, y + (HEADER_HEIGHT - mc.textRenderer.fontHeight) / 2 + 1,
                    COLOR_TEXT, false);

            int rowY = y + HEADER_HEIGHT;
            for (Module module : modules) {
                boolean hovered = mouseX >= x && mouseX <= x + w
                        && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
                if (hovered) {
                    ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, COLOR_ROW_HOVER);
                } else if ((COLOR_ROW & 0xFF000000) != 0) {
                    ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, COLOR_ROW);
                }

                int textColor = module.isEnabled() ? COLOR_ACCENT : COLOR_TEXT_DIM;
                ctx.drawText(mc.textRenderer, module.getDisplayName(),
                        x + 8, rowY + (ROW_HEIGHT - mc.textRenderer.fontHeight) / 2 + 1,
                        textColor, false);

                if (module.isEnabled()) {
                    ctx.fill(x + 2, rowY + 3, x + 4, rowY + ROW_HEIGHT - 3, COLOR_ACCENT);
                }

                rowY += ROW_HEIGHT;
            }
        }
    }
}
