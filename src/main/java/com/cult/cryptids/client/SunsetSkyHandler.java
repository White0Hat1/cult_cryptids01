package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SunsetSkyHandler {

    // 🎨 Цвет закатного тумана (фиолетово-розовый)
    private static final float TARGET_R = 0.55F;
    private static final float TARGET_G = 0.30F;
    private static final float TARGET_B = 0.65F;

    // ⏰ Временные окна заката (тики, 0-24000)
    // 12000 = закат, 13000 = сумерки, 18000 = полночь
    private static final long SUNSET_START = 11500L;
    private static final long SUNSET_PEAK  = 12750L;
    private static final long SUNSET_END   = 14000L;

    // 🌫️ Максимальная сила эффекта (0.0-1.0)
    private static final float MAX_STRENGTH = 0.75F;

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long dayTime = mc.level.getDayTime() % 24000L;
        float strength = getSunsetStrength(dayTime);

        if (strength <= 0) return;

        // Смешиваем текущий цвет тумана с нашим закатным
        event.setRed(lerp(event.getRed(), TARGET_R, strength));
        event.setGreen(lerp(event.getGreen(), TARGET_G, strength));
        event.setBlue(lerp(event.getBlue(), TARGET_B, strength));
    }

    /**
     * Возвращает 0.0-1.0 — насколько сильно применить закатный цвет.
     * Плавное нарастание до пика, потом плавное затухание.
     */
    private static float getSunsetStrength(long dayTime) {
        if (dayTime < SUNSET_START || dayTime > SUNSET_END) return 0.0F;

        float factor;
        if (dayTime <= SUNSET_PEAK) {
            // Нарастание: 11500 → 12750
            factor = (float) (dayTime - SUNSET_START) / (float) (SUNSET_PEAK - SUNSET_START);
        } else {
            // Затухание: 12750 → 14000
            factor = 1.0F - (float) (dayTime - SUNSET_PEAK) / (float) (SUNSET_END - SUNSET_PEAK);
        }

        // Плавная кривая (smoothstep) для органичного перехода
        factor = factor * factor * (3.0F - 2.0F * factor);

        return factor * MAX_STRENGTH;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}