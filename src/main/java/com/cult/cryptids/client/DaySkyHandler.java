package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ☀️ Дневное небо — тёплый светло-голубой оттенок.
 *
 * Цвет снят с референса (RGB 162, 205, 222).
 * Работает как SunsetSkyHandler, но для временного окна ДНЯ (0–12000 тиков).
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DaySkyHandler {

    // 🎨 Цвет с референса — светло-голубой с тёплым подтоном
    // RGB (162, 205, 222) → float (0.635, 0.804, 0.871)
    private static final float TARGET_R = 0.635F;
    private static final float TARGET_G = 0.804F;
    private static final float TARGET_B = 0.871F;

    // ⏰ Временные окна дня (тики, 0–24000)
    private static final long DAY_START = 1000L;   // после рассвета
    private static final long DAY_PEAK  = 6000L;   // полдень
    private static final long DAY_END   = 11000L;  // до начала заката (SunsetSkyHandler стартует с 11500)

    // 🌫️ Максимальная сила эффекта (0.0–1.0)
    private static final float MAX_STRENGTH = 0.85F;

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 🩸 Не срабатываем во время Blood Moon — там свой красный туман.
        if (BloodMoonClientState.isActive()) return;

        long dayTime = mc.level.getDayTime() % 24000L;
        float strength = getDayStrength(dayTime);

        if (strength <= 0.0F) return;

        event.setRed(lerp(event.getRed(), TARGET_R, strength));
        event.setGreen(lerp(event.getGreen(), TARGET_G, strength));
        event.setBlue(lerp(event.getBlue(), TARGET_B, strength));
    }

    /**
     * Возвращает 0.0–1.0 — насколько сильно применить дневной цвет.
     * Плавное нарастание до полудня, потом плавное затухание к закату.
     */
    private static float getDayStrength(long dayTime) {
        if (dayTime < DAY_START || dayTime > DAY_END) return 0.0F;

        float factor;
        if (dayTime <= DAY_PEAK) {
            factor = (float) (dayTime - DAY_START) / (float) (DAY_PEAK - DAY_START);
        } else {
            factor = 1.0F - (float) (dayTime - DAY_PEAK) / (float) (DAY_END - DAY_PEAK);
        }

        // Плавная кривая (smoothstep)
        factor = factor * factor * (3.0F - 2.0F * factor);

        return factor * MAX_STRENGTH;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}