package ru.nanovisuals.modules.settings;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean initial) {
        super(name, initial);
    }

    public void toggle() {
        setValue(!value);
    }
}
