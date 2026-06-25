package ru.nanovisuals.hud.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import ru.nanovisuals.hud.HudWidget;
import ru.nanovisuals.utils.ColorUtil;
import ru.nanovisuals.utils.GlassRender;
import ru.nanovisuals.utils.MathUtil;
import ru.nanovisuals.utils.Spring;

public class EffectsHud extends HudWidget {

    private final Map<String, Spring> bars = new HashMap<>();

    public EffectsHud() {
        super("EffectsHud", "Active Effects", 14f, 130f);
    }

    @Override
    public float width()  { return 170f; }
    @Override
    public float height() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return 30f;
        int n = p.getStatusEffects().size();
        return 26f + n * 20f + 4f;
    }

    @Override
    public void renderHud(DrawContext ctx, float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        List<StatusEffectInstance> effects = new ArrayList<>(p.getStatusEffects());
        if (effects.isEmpty()) return;

        float x = drawX();
        float y = drawY();
        float w = width();
        float h = 26f + effects.size() * 20f + 4f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + w / 2f, y + h / 2f, 0);
        ctx.getMatrices().multiply(
                new org.joml.Quaternionf().rotateZ((float) Math.toRadians(tiltDeg() * 0.5f)));
        ctx.getMatrices().translate(-(x + w / 2f), -(y + h / 2f), 0);

        GlassRender.glassPanel(ctx, x, y, w, h, 12f);
        int label = ColorUtil.rgba(220, 225, 240, 220);
        ctx.drawText(mc.textRenderer, "Effects",
                (int) (x + 12f), (int) (y + 8f), label, false);

        float row = y + 24f;
        for (StatusEffectInstance e : effects) {
            renderEffectRow(ctx, mc, e, x, row, w);
            row += 20f;
        }
        ctx.getMatrices().pop();
    }

    private void renderEffectRow(DrawContext ctx, MinecraftClient mc, StatusEffectInstance e,
                                 float x, float row, float w) {
        RegistryEntry<?> entry = e.getEffectType();
        int color = e.getEffectType().value().getColor() | 0xFF000000;

        Text name = e.getEffectType().value().getName();
        String amp = e.getAmplifier() > 0 ? " " + roman(e.getAmplifier() + 1) : "";
        String duration = ticksToTime(e.getDuration());

        int textColor = ColorUtil.rgba(235, 240, 250, 235);
        ctx.drawText(mc.textRenderer, name.getString() + amp,
                (int) (x + 12f), (int) (row + 1f), textColor, false);

        int durColor = ColorUtil.rgba(170, 175, 195, 200);
        int durW = mc.textRenderer.getWidth(duration);
        ctx.drawText(mc.textRenderer, duration,
                (int) (x + w - 12f - durW), (int) (row + 1f), durColor, false);

        float trackX = x + 12f;
        float trackY = row + 12f;
        float trackW = w - 24f;
        int back = ColorUtil.rgba(45, 50, 65, 200);
        GlassRender.fillRounded(ctx, trackX, trackY, trackW, 3f, 1.5f, back, back);

        String key = entry.getKey().map(k -> k.getValue().toString()).orElse(name.getString());
        Spring spring = bars.computeIfAbsent(key, k -> {
            Spring s = new Spring(140f, 22f);
            s.snap(1f);
            return s;
        });
        float u = MathUtil.clamp(e.getDuration() / 600f, 0f, 1f);
        spring.setTarget(u);
        // dt of 1 frame ~ already advanced via updateAnimation? bars need dt here:
        // use a short fixed step since updateAnimation doesn't know about them
        spring.update(1f / 60f);

        float fillW = trackW * spring.position;
        int fillEnd = ColorUtil.multAlpha(color, 0.55f);
        if (fillW > 0.5f) {
            GlassRender.fillRounded(ctx, trackX, trackY, fillW, 3f, 1.5f, color, fillEnd);
        }
    }

    private static String ticksToTime(int ticks) {
        if (ticks <= 0) return "0s";
        int sec = ticks / 20;
        if (sec < 60) return sec + "s";
        int m = sec / 60;
        int s = sec % 60;
        return m + ":" + (s < 10 ? "0" + s : s);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
