package ru.nanovisuals.modules.render;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;

import ru.nanovisuals.events.EventHandler;
import ru.nanovisuals.events.TickEvent;
import ru.nanovisuals.events.WorldRenderEvent;
import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;
import ru.nanovisuals.utils.ColorUtil;
import ru.nanovisuals.utils.Counter;
import ru.nanovisuals.utils.Render3DUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class JumpCircle extends Module {

    private final List<Circle> circles = new ArrayList<>();
    final Identifier circleTexture = Identifier.of("nanovisuals", "textures/circle.png");
    boolean wasOnGround = true;

    public JumpCircle() {
        super("JumpCircle", "Jump Circle", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player == null) return;

        boolean isOnGround = mc.player.isOnGround();

        if (wasOnGround && !isOnGround) {
            Vec3d pos = new Vec3d(
                    mc.player.getX(),
                    Math.floor(mc.player.getY()) + 0.001,
                    mc.player.getZ()
            );
            circles.add(new Circle(pos, new Counter()));
        }

        wasOnGround = isOnGround;

        circles.removeIf(c -> c.timer.passedMs(3000));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        renderCircles();
    }

    private void renderCircles() {
        if (circles.isEmpty()) return;

        for (Circle circle : circles) {
            renderSingleCircle(circle);
        }
    }

    private void renderSingleCircle(Circle circle) {
        float lifeTime = (float) circle.timer.getPassedTimeMs();
        float maxTime = 3000f;
        float progress = lifeTime / maxTime;

        if (progress >= 1f) return;

        float scale = progress * 2f;
        float alpha = 1f - (progress * progress);

        int baseColor = ColorUtil.fade((int)(progress * 360f));
        int color = ColorUtil.multAlpha(baseColor, alpha);

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d cameraPos = camera.getPos();
        Vec3d circlePos = circle.pos();

        MatrixStack matrixStack = new MatrixStack();

        matrixStack.translate(circlePos.x - cameraPos.x, circlePos.y - cameraPos.y, circlePos.z - cameraPos.z);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        MatrixStack.Entry entry = matrixStack.peek();
        Vector4i colors = new Vector4i(color, color, color, color);

        Render3DUtil.drawTexture(entry, circleTexture, -scale / 2f, -scale / 2f, scale, scale, colors, true);
    }

    public record Circle(Vec3d pos, Counter timer) {}
}
