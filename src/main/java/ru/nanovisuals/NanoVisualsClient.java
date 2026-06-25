package ru.nanovisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

import ru.nanovisuals.events.EventBus;
import ru.nanovisuals.events.TickEvent;
import ru.nanovisuals.events.WorldRenderEvent;
import ru.nanovisuals.gui.ClickGUI;
import ru.nanovisuals.hud.HudManager;
import ru.nanovisuals.modules.ModuleManager;
import ru.nanovisuals.sound.CombatSounds;
import ru.nanovisuals.sound.SoundManager;

public class NanoVisualsClient implements ClientModInitializer {

    public static final String MOD_ID = "nanovisuals";

    private KeyBinding clickGuiKey;

    @Override
    public void onInitializeClient() {
        SoundManager.getInstance().init();
        ModuleManager.init();

        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nanovisuals.clickgui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.nanovisuals"
        ));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof LivingEntity le) {
                CombatSounds.getInstance().onAttack(le);
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clickGuiKey.wasPressed()) {
                client.setScreen(new ClickGUI());
            }
            EventBus.getInstance().post(new TickEvent());
            CombatSounds.getInstance().tick();
        });

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            float tickDelta = ctx.tickCounter().getTickDelta(true);
            EventBus.getInstance().post(new WorldRenderEvent(
                    ctx.matrixStack(),
                    tickDelta,
                    ctx.camera()
            ));
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
                HudManager.getInstance().onHudRender(drawContext, tickCounter.getTickDelta(true))
        );
    }
}
