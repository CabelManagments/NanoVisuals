package ru.nanovisuals.modules.settings;

public abstract class Setting<T> {

    protected final String name;
    protected T value;

    protected Setting(String name, T initial) {
        this.name = name;
        this.value = initial;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
