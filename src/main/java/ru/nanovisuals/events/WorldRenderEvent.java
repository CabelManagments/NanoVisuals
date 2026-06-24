package ru.nanovisuals.events;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;

public class WorldRenderEvent extends Event {

    private final MatrixStack matrixStack;
    private final float tickDelta;
    private final Camera camera;

    public WorldRenderEvent(MatrixStack matrixStack, float tickDelta, Camera camera) {
        this.matrixStack = matrixStack;
        this.tickDelta = tickDelta;
        this.camera = camera;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public Camera getCamera() {
        return camera;
    }
}
