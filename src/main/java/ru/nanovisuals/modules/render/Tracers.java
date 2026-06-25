package ru.nanovisuals.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import ru.nanovisuals.events.EventHandler;
import ru.nanovisuals.events.WorldRenderEvent;
import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;
import ru.nanovisuals.utils.ColorUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tracers extends Module {

    final float lineWidth = 1.6f;
    final float originForward = 0.15f;
    int colorOffset = 0;

    public Tracers() {
        super("Tracers", "Tracers", ModuleCategory.VISUALS);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;

        Camera camera = e.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d forward = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        Vec3d origin = forward.multiply(originForward);

        float tickDelta = e.getTickDelta();
        int color = ColorUtil.fade(colorOffset);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(lineWidth);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = new Matrix4f();

        boolean hasAny = false;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (!p.isAlive() || p.isSpectator() || p.isInvisible()) continue;

            double tx = MathHelper.lerp(tickDelta, p.prevX, p.getX());
            double ty = MathHelper.lerp(tickDelta, p.prevY, p.getY()) + p.getStandingEyeHeight() * 0.5f;
            double tz = MathHelper.lerp(tickDelta, p.prevZ, p.getZ());

            float ox = (float) origin.x;
            float oy = (float) origin.y;
            float oz = (float) origin.z;
            float dx = (float) (tx - cameraPos.x);
            float dy = (float) (ty - cameraPos.y);
            float dz = (float) (tz - cameraPos.z);

            buf.vertex(matrix, ox, oy, oz).color(ColorUtil.multAlpha(color, 0.2f));
            buf.vertex(matrix, dx, dy, dz).color(color);
            hasAny = true;
        }

        if (hasAny) {
            BufferRenderer.drawWithGlobalProgram(buf.end());
        } else {
            buf.endNullable();
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
