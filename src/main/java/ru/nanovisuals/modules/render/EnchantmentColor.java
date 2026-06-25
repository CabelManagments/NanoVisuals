package ru.nanovisuals.modules.render;

import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import ru.nanovisuals.modules.Module;
import ru.nanovisuals.modules.ModuleCategory;
import ru.nanovisuals.utils.ColorUtil;

public class EnchantmentColor extends Module {

    private static EnchantmentColor INSTANCE;

    public EnchantmentColor() {
        super("EnchantmentColor", "Enchantment Color", ModuleCategory.VISUALS);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static int getColor() {
        return ColorUtil.fade(0);
    }

    public static boolean isGlintShader(ShaderProgramKey key) {
        if (key == null) return false;
        return key == ShaderProgramKeys.RENDERTYPE_GLINT
                || key == ShaderProgramKeys.RENDERTYPE_GLINT_TRANSLUCENT
                || key == ShaderProgramKeys.RENDERTYPE_ENTITY_GLINT
                || key == ShaderProgramKeys.RENDERTYPE_ARMOR_ENTITY_GLINT;
    }
}
