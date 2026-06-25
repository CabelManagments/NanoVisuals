package ru.nanovisuals.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import ru.nanovisuals.events.EventHandler;
import ru.nanovisuals.events.WorldRenderEvent;
import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;
import ru.nanovisuals.utils.ColorUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChinaHat extends Module {

    final int sides = 48;
    final float coneRadius = 0.42f;
    final float coneHeight = 0.55f;
    final float brimRadius = 0.78f;
    final float yOffset = 0.35f;

    public ChinaHat() {
        super("ChinaHat", "China Hat", ModuleCategory.VISUALS);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null) return;

        ClientPlayerEntity player = mc.player;
        float tickDelta = e.getTickDelta();

        double px = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double py = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double pz = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
        double headY = py + player.getStandingEyeHeight() + yOffset;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d cameraPos = camera.getPos();

        MatrixStack matrices = new MatrixStack();
        matrices.translate(px - cameraPos.x, headY - cameraPos.y, pz - cameraPos.z);

        renderHat(matrices.peek(), ColorUtil.fade(0));
    }

    private void renderHat(MatrixStack.Entry entry, int baseColor) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Matrix4f matrix = entry.getPositionMatrix();
        int apex = ColorUtil.multAlpha(baseColor, 0.95f);
        int rim = ColorUtil.multAlpha(baseColor, 0.55f);
        int edge = ColorUtil.multAlpha(baseColor, 0.25f);

        Tessellator tess = Tessellator.getInstance();

        BufferBuilder cone = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        cone.vertex(matrix, 0f, coneHeight, 0f).color(apex);
        for (int i = 0; i <= sides; i++) {
            double angle = i * Math.PI * 2.0 / sides;
            float x = (float) Math.cos(angle) * coneRadius;
            float z = (float) Math.sin(angle) * coneRadius;
            cone.vertex(matrix, x, 0f, z).color(rim);
        }
        BufferRenderer.drawWithGlobalProgram(cone.end());

        BufferBuilder brim = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        brim.vertex(matrix, 0f, 0f, 0f).color(rim);
        for (int i = 0; i <= sides; i++) {
            double angle = i * Math.PI * 2.0 / sides;
            float x = (float) Math.cos(angle) * brimRadius;
            float z = (float) Math.sin(angle) * brimRadius;
            brim.vertex(matrix, x, -0.06f, z).color(edge);
        }
        BufferRenderer.drawWithGlobalProgram(brim.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
