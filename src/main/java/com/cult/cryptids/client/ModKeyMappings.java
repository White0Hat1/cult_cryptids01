package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * ⌨️ Клавиши мода. Регистрируются в MOD-bus.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModKeyMappings {

    /** 🔫 R — перезарядка M14. */
    public static final KeyMapping RELOAD_KEY = new KeyMapping(
            "key.cult_cryptids.reload",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.cult_cryptids"
    );

    private ModKeyMappings() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(RELOAD_KEY);
    }
}