package ru.nanovisuals.modules.settings;

import ru.nanovisuals.utils.MathUtil;

public class NumberSetting extends Setting<Float> {

    private final float min;
    private final float max;
    private final float step;

    public NumberSetting(String name, float initial, float min, float max, float step) {
        super(name, MathUtil.clamp(initial, min, max));
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getStep() { return step; }

    public void setFromUnit(float u) {
        float v = min + (max - min) * MathUtil.clamp(u, 0f, 1f);
        if (step > 0f) {
            v = min + Math.round((v - min) / step) * step;
        }
        setValue(MathUtil.clamp(v, min, max));
    }

    public float toUnit() {
        if (max <= min) return 0f;
        return (value - min) / (max - min);
    }
}
