package ru.nanovisuals.utils;

import java.awt.Color;

public class ColorUtil {

    public static int fade(int offset) {
        float hue = ((System.currentTimeMillis() / 10L + offset) % 360L) / 360f;
        return Color.HSBtoRGB(hue, 1.0f, 1.0f);
    }

    public static int multAlpha(int argb, float mult) {
        int a = (argb >> 24) & 0xFF;
        a = (int) Math.max(0, Math.min(255, a * mult));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    public static int interpolate(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return rgba(r, g, b, a);
    }
}
