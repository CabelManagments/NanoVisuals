package ru.nanovisuals.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4i;

public class Render3DUtil {

    public static void drawTexture(MatrixStack.Entry entry, Identifier texture,
                                   float x, float y, float width, float height,
                                   Vector4i colors, boolean blend) {

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        if (blend) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        Matrix4f matrix = entry.getPositionMatrix();

        int c0 = colors.x;
        int c1 = colors.y;
        int c2 = colors.z;
        int c3 = colors.w;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buffer.vertex(matrix, x,         y,          0f).texture(0f, 0f).color(c0);
        buffer.vertex(matrix, x,         y + height, 0f).texture(0f, 1f).color(c1);
        buffer.vertex(matrix, x + width, y + height, 0f).texture(1f, 1f).color(c2);
        buffer.vertex(matrix, x + width, y,          0f).texture(1f, 0f).color(c3);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        if (blend) {
            RenderSystem.disableBlend();
        }
    }
}
