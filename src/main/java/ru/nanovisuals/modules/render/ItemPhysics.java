package ru.nanovisuals.modules.render;

import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;

public class ItemPhysics extends Module {

    private static ItemPhysics INSTANCE;

    public ItemPhysics() {
        super("ItemPhysics", "Item Physics", ModuleCategory.VISUALS);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}
