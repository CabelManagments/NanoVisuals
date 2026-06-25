package ru.nanovisuals.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Owns shared HUD state: per-frame timing, camera/velocity deltas that drive
 * inertia springs, and dispatches the draw call to each enabled widget.
 */
public class HudManager {

    private static final HudManager INSTANCE = new HudManager();

    public static HudManager getInstance() {
        return INSTANCE;
    }

    private final List<HudWidget> widgets = new ArrayList<>();

    private long lastFrameNs;
    private float lastYaw;
    private float lastPitch;
    private double lastSpeed;
    private boolean primed;

    public void register(HudWidget widget) {
        widgets.add(widget);
    }

    public List<HudWidget> getWidgets() {
        return widgets;
    }

    public void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        long now = System.nanoTime();
        if (lastFrameNs == 0L) lastFrameNs = now;
        float dt = (now - lastFrameNs) / 1_000_000_000f;
        lastFrameNs = now;
        if (dt > 0.1f) dt = 0.1f;

        ClientPlayerEntity player = mc.player;
        if (player != null) {
            float yaw = player.getYaw();
            float pitch = player.getPitch();
            double speed = Math.hypot(player.getVelocity().x, player.getVelocity().z);
            if (primed) {
                float yawD = wrapDeg(yaw - lastYaw);
                float pitchD = pitch - lastPitch;
                float speedD = (float) (speed - lastSpeed);
                for (HudWidget w : widgets) {
                    if (w.isEnabled()) w.applyInertia(yawD, pitchD, speedD);
                }
            }
            lastYaw = yaw;
            lastPitch = pitch;
            lastSpeed = speed;
            primed = true;
        }

        for (HudWidget w : widgets) {
            if (!w.isEnabled()) continue;
            w.updateAnimation(dt);
            w.renderHud(ctx, tickDelta);
        }
    }

    private static float wrapDeg(float d) {
        d %= 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}
