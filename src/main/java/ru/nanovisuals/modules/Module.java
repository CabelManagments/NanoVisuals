package ru.nanovisuals.modules;

import net.minecraft.client.MinecraftClient;
import ru.nanovisuals.events.EventBus;

public abstract class Module {

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private boolean enabled;

    public Module(String name, String displayName, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean state) {
        if (enabled == state) return;
        enabled = state;
        if (enabled) {
            EventBus.getInstance().subscribe(this);
            onEnable();
        } else {
            onDisable();
            EventBus.getInstance().unsubscribe(this);
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
