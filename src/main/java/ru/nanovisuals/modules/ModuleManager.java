package ru.nanovisuals.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.nanovisuals.modules.render.JumpCircle;
import ru.nanovisuals.modules.render.TargetESP;

public class ModuleManager {

    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        register(new JumpCircle());
        register(new TargetESP());
    }

    public static void register(Module module) {
        MODULES.add(module);
    }

    public static List<Module> getModules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> getByCategory(ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.getCategory() == category) result.add(m);
        }
        return result;
    }
}
