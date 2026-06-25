package ru.nanovisuals.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import ru.nanovisuals.events.EventBus;
import ru.nanovisuals.modules.settings.Setting;
import ru.nanovisuals.sound.SoundManager;

public abstract class Module {

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();
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
            SoundManager.getInstance().play(SoundManager.Sound.ENABLE);
        } else {
            onDisable();
            EventBus.getInstance().unsubscribe(this);
            SoundManager.getInstance().play(SoundManager.Sound.DISABLE);
        }
    }

    protected <S extends Setting<?>> S register(S setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean hasSettings() {
        return !settings.isEmpty();
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
