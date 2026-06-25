package ru.nanovisuals.utils;

public final class Easing {

    private Easing() {}

    public static float easeOutExpo(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return 1f - (float) Math.pow(2.0, -10.0 * t);
    }

    public static float easeOutBack(float t) {
        final float c1 = 1.70158f;
        final float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    public static float easeOutQuart(float t) {
        float u = 1f - t;
        return 1f - u * u * u * u;
    }

    public static float easeInOutCubic(float t) {
        return t < 0.5f
                ? 4f * t * t * t
                : 1f - (float) Math.pow(-2.0 * t + 2.0, 3.0) / 2f;
    }

    public static float bezier(float t, float p1y, float p2y) {
        float u = 1f - t;
        return 3f * u * u * t * p1y + 3f * u * t * t * p2y + t * t * t;
    }
}
