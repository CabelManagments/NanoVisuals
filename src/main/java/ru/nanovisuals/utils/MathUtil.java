package ru.nanovisuals.utils;

public class MathUtil {

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float easeOutCubic(float t) {
        float f = t - 1f;
        return f * f * f + 1f;
    }

    public static float frameTimeFactor(float perSecond, float deltaSeconds) {
        return 1f - (float) Math.exp(-perSecond * deltaSeconds);
    }
}
