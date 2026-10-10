package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🌙 Ночное небо — почти чёрное, с лёгким тёплым подтоном.
 * Не срабатывает во время Blood Moon — там свой красный туман.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NightSkyHandler {

    // 🎨 Цвет почти чёрного неба (очень тёмный, чуть тёплый)
    private static final float TARGET_R = 0.04F;
    private static final float TARGET_G = 0.02F;
    private static final float TARGET_B = 0.02F;

    // ⏰ Временное окно ночи
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_PEAK  = 18000L;
    private static final long NIGHT_END   = 23000L;

    // 🌫️ Сила эффекта
    private static final float MAX_STRENGTH = 0.95F;

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 🩸 Во время Blood Moon — не срабатываем.
        if (BloodMoonClientState.isActive()) return;

        long dayTime = mc.level.getDayTime() % 24000L;
        float strength = getNightStrength(dayTime);

        if (strength <= 0.0F) return;

        event.setRed(lerp(event.getRed(), TARGET_R, strength));
        event.setGreen(lerp(event.getGreen(), TARGET_G, strength));
        event.setBlue(lerp(event.getBlue(), TARGET_B, strength));
    }

    private static float getNightStrength(long dayTime) {
        if (dayTime < NIGHT_START || dayTime > NIGHT_END) return 0.0F;

        float factor;
        if (dayTime <= NIGHT_PEAK) {
            factor = (float) (dayTime - NIGHT_START) / (float) (NIGHT_PEAK - NIGHT_START);
        } else {
            factor = 1.0F - (float) (dayTime - NIGHT_PEAK) / (float) (NIGHT_END - NIGHT_PEAK);
        }

        factor = factor * factor * (3.0F - 2.0F * factor);
        return factor * MAX_STRENGTH;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}