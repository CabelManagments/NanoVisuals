package ru.nanovisuals.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.nanovisuals.modules.render.ItemPhysics;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Inject(
        method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void nanovisuals$layFlat(ItemEntity entity, float yaw, float tickDelta,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, CallbackInfo ci) {
        if (!ItemPhysics.isActive()) return;
        if (!entity.isOnGround()) return;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        matrices.translate(0f, -0.12f, 0f);
    }
}
