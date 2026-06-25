package ru.nanovisuals.hud;

import net.minecraft.client.gui.DrawContext;
import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;
import ru.nanovisuals.utils.Spring;

/**
 * A HUD widget is a Module that draws itself on the in-game overlay. State
 * (position, springs, animations) lives here so render code stays pure.
 */
public abstract class HudWidget extends Module {

    public final Spring inertiaX = new Spring(80f, 14f);
    public final Spring inertiaY = new Spring(80f, 14f);
    public final Spring tilt     = new Spring(60f, 12f);

    protected float anchorX;
    protected float anchorY;

    protected HudWidget(String name, String displayName, float anchorX, float anchorY) {
        super(name, displayName, ModuleCategory.VISUALS);
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public abstract float width();
    public abstract float height();

    public abstract void renderHud(DrawContext ctx, float partialTicks);

    public void updateAnimation(float dt) {
        inertiaX.update(dt);
        inertiaY.update(dt);
        tilt.update(dt);
    }

    public void applyInertia(float yawDelta, float pitchDelta, float speedDelta) {
        inertiaX.kick(-yawDelta * 0.35f);
        inertiaY.kick(-pitchDelta * 0.35f + speedDelta * 0.25f);
        tilt.kick(-yawDelta * 0.04f);
    }

    public float drawX() { return anchorX + inertiaX.position; }
    public float drawY() { return anchorY + inertiaY.position; }
    public float tiltDeg() { return tilt.position; }

    public void setAnchor(float x, float y) {
        this.anchorX = x;
        this.anchorY = y;
    }
}
