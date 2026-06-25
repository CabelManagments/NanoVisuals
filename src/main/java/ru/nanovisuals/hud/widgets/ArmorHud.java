package ru.nanovisuals.hud.widgets;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import ru.nanovisuals.hud.HudWidget;
import ru.nanovisuals.utils.ColorUtil;
import ru.nanovisuals.utils.GlassRender;
import ru.nanovisuals.utils.MathUtil;
import ru.nanovisuals.utils.Spring;

public class ArmorHud extends HudWidget {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final Spring[] fill = new Spring[SLOTS.length];

    public ArmorHud() {
        super("ArmorHud", "Armor HUD", 14f, 14f);
        for (int i = 0; i < fill.length; i++) {
            fill[i] = new Spring(150f, 22f);
            fill[i].snap(0f);
        }
    }

    @Override
    public float width()  { return 152f; }
    @Override
    public float height() { return 30f + SLOTS.length * 16f; }

    @Override
    public void updateAnimation(float dt) {
        super.updateAnimation(dt);
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack s = p.getEquippedStack(SLOTS[i]);
            float u = 0f;
            if (!s.isEmpty() && s.isDamageable()) {
                int max = s.getMaxDamage();
                u = max <= 0 ? 1f : MathUtil.clamp(1f - s.getDamage() / (float) max, 0f, 1f);
            } else if (!s.isEmpty()) {
                u = 1f;
            }
            fill[i].setTarget(u);
            fill[i].update(dt);
        }
    }

    @Override
    public void renderHud(DrawContext ctx, float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

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

        int label = ColorUtil.rgba(220, 225, 240, 220);
        ctx.drawText(mc.textRenderer, "Armor", (int) (x + 12f), (int) (y + 8f), label, false);

        float row = y + 24f;
        List<String> tags = new ArrayList<>();
        tags.add("Helm"); tags.add("Chest"); tags.add("Legs"); tags.add("Boots");

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = p.getEquippedStack(SLOTS[i]);
            int textColor = stack.isEmpty()
                    ? ColorUtil.rgba(120, 125, 140, 180)
                    : ColorUtil.rgba(230, 235, 250, 220);
            ctx.drawText(mc.textRenderer, tags.get(i),
                    (int) (x + 12f), (int) (row + 4f), textColor, false);

            if (!stack.isEmpty()) {
                ctx.drawItem(stack, (int) (x + w - 28f), (int) (row - 1f));
            }

            float trackX = x + 42f;
            float trackY = row + 6f;
            float trackW = w - 78f;
            int back = ColorUtil.rgba(45, 50, 65, 200);
            GlassRender.fillRounded(ctx, trackX, trackY, trackW, 3f, 1.5f, back, back);

            float u = fill[i].position;
            int color = barColor(u);
            int colorEnd = ColorUtil.multAlpha(color, 0.6f);
            float fillW = trackW * u;
            if (fillW > 0.5f) {
                GlassRender.fillRounded(ctx, trackX, trackY, fillW, 3f, 1.5f, color, colorEnd);
            }
            row += 16f;
        }
        ctx.getMatrices().pop();
    }

    private static int barColor(float u) {
        if (u > 0.6f) return ColorUtil.rgba(120, 220, 150, 235);
        if (u > 0.3f) return ColorUtil.rgba(245, 200, 80, 235);
        return ColorUtil.rgba(245, 100, 90, 235);
    }
}
