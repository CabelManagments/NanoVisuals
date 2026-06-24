package ru.nanovisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.nanovisuals.modules.render.EnchantmentColor;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(
        method = "setShader(Lnet/minecraft/client/gl/ShaderProgramKey;)V",
        at = @At("TAIL")
    )
    private static void nanovisuals$tintGlint(ShaderProgramKey key, CallbackInfo ci) {
        if (!EnchantmentColor.isActive()) return;
        if (!EnchantmentColor.isGlintShader(key)) return;

        int color = EnchantmentColor.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1f);
    }
}
