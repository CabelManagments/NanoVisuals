package ru.nanovisuals.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.nanovisuals.modules.render.ChinaHat;
import ru.nanovisuals.modules.render.EnchantmentColor;
import ru.nanovisuals.modules.render.ItemPhysics;
import ru.nanovisuals.modules.render.JumpCircle;
import ru.nanovisuals.modules.render.TargetESP;
import ru.nanovisuals.modules.render.Tracers;

public class ModuleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("NanoVisuals");
    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        tryRegister("JumpCircle", JumpCircle::new);
        tryRegister("TargetESP", TargetESP::new);
        tryRegister("ChinaHat", ChinaHat::new);
        tryRegister("Tracers", Tracers::new);
        tryRegister("ItemPhysics", ItemPhysics::new);
        tryRegister("EnchantmentColor", EnchantmentColor::new);
        LOGGER.info("Registered {} modules total", MODULES.size());
    }

    private static void tryRegister(String name, Supplier<? extends Module> factory) {
        try {
            register(factory.get());
            LOGGER.info("Registered module: {}", name);
        } catch (Throwable t) {
            LOGGER.error("Failed to register module {}: {}", name, t.toString(), t);
        }
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
