package ru.nanovisuals.modules;

public enum ModuleCategory {
    RENDER("Render"),
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    MISC("Misc");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
