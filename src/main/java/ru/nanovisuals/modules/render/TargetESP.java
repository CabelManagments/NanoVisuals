package ru.nanovisuals.modules.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;

import ru.nanovisuals.events.EventHandler;
import ru.nanovisuals.events.TickEvent;
import ru.nanovisuals.events.WorldRenderEvent;
import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;
import ru.nanovisuals.utils.ColorUtil;
import ru.nanovisuals.utils.Render3DUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetESP extends Module {

    final Identifier circleTexture = Identifier.of("nanovisuals", "textures/circle.png");

    float radius = 1.6f;
    float animSpeed = 600f;
    float floatAmplitude = 0.15f;
    float searchRange = 6.0f;
    int colorOffset = 0;

    LivingEntity target;

    public TargetESP() {
        super("TargetESP", "Target ESP", ModuleCategory.VISUALS);
    }

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            target = null;
            return;
        }
        target = findTarget();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (target == null || !target.isAlive() || mc.player == null) return;

        float partialTicks = e.getTickDelta();

        double ix = MathHelper.lerp(partialTicks, target.prevX, target.getX());
        double iy = MathHelper.lerp(partialTicks, target.prevY, target.getY());
        double iz = MathHelper.lerp(partialTicks, target.prevZ, target.getZ());

        float time = System.currentTimeMillis() / animSpeed;
        float yBob = (float) Math.sin(time) * floatAmplitude;

        Vec3d discPos = new Vec3d(ix, iy + floatAmplitude + yBob + 0.01, iz);

        renderDisc(discPos, time);
    }

    private LivingEntity findTarget() {
        LivingEntity nearest = null;
        double nearestDist = searchRange;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (p.isInvisible() || p.isSpectator() || !p.isAlive()) continue;

            double d = mc.player.distanceTo(p);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private void renderDisc(Vec3d pos, float time) {
        int baseColor = ColorUtil.fade(colorOffset);
        float alphaPulse = 0.65f + (float) Math.sin(time) * 0.25f;
        int color = ColorUtil.multAlpha(baseColor, alphaPulse);

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d cameraPos = camera.getPos();

        MatrixStack matrixStack = new MatrixStack();

        matrixStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);

        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        MatrixStack.Entry entry = matrixStack.peek();
        Vector4i colors = new Vector4i(color, color, color, color);

        Render3DUtil.drawTexture(entry, circleTexture,
                -radius / 2f, -radius / 2f, radius, radius, colors, true);
    }
}
