package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🌫️ Туман вокруг Siren Head.
 *
 * Особенности:
 *   - Не появляется мгновенно — плавно наступает.
 *   - Плотность зависит от расстояния до ближайшего Siren.
 *   - Когда Siren далеко или исчез, туман медленно отступает.
 *   - Не срабатывает во время Blood Moon (там свой туман).
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SirenFogHandler {

    // 📏 Радиус обнаружения Siren — если он ближе, туман начнёт наползать.
    private static final double FOG_RADIUS = 100.0D;

    // 🎨 Цвет тумана (серый).
    private static final float FOG_R = 0.35F;
    private static final float FOG_G = 0.35F;
    private static final float FOG_B = 0.35F;

    // 📐 Границы near/far в зависимости от плотности.
    private static final float FOG_NEAR_MIN = 0.5F;
    private static final float FOG_FAR_MIN  = 8.0F;
    private static final float FOG_NEAR_MAX = 40.0F;
    private static final float FOG_FAR_MAX  = 120.0F;

    // 🎚️ Максимальная сила тумана (0.0–1.0).
    private static final float MAX_STRENGTH = 0.85F;

    // ⏱️ Скорость плавного наступления/отступления тумана (за 1 тик).
    //    Чем меньше — тем медленнее. 0.005 = ~3.3 секунды до полной силы при 0 → 1.
    //    Хочешь, чтобы туман наползал медленнее — уменьшай.
    private static final float FADE_IN_PER_TICK  = 0.003F;  // наползание (медленно)
    private static final float FADE_OUT_PER_TICK = 0.006F;  // отступление (чуть быстрее)

    /** 🌫️ Текущая плотность — плавно меняется каждый тик. */
    private static float currentFogStrength = 0.0F;

    /** 🎯 Целевая плотность — куда стремимся. */
    private static float targetFogStrength = 0.0F;

    // =========================================================
    // 🧠 ТИК — обновляем current → target
    // =========================================================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.level == null || mc.screen != null) {
            targetFogStrength = 0.0F;
            fadeToward(0.0F);
            return;
        }

        // 🩸 Во время Blood Moon — не срабатываем.
        if (BloodMoonClientState.isActive()) {
            targetFogStrength = 0.0F;
            fadeToward(0.0F);
            return;
        }

        // 🎯 Считаем целевую силу по расстоянию до ближайшего Siren.
        targetFogStrength = computeTargetStrength(player);

        // 🌫️ Плавно двигаем current к target.
        fadeToward(targetFogStrength);
    }

    /**
     * Считает целевую силу тумана по расстоянию до ближайшего Siren.
     * Если Siren нет — 0.
     */
    private static float computeTargetStrength(Player player) {
        SirenHeadEntity nearest = null;
        double nearestDist = FOG_RADIUS;

        for (SirenHeadEntity siren : player.level().getEntitiesOfClass(
                SirenHeadEntity.class,
                player.getBoundingBox().inflate(FOG_RADIUS))) {
            double d = siren.distanceTo(player);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = siren;
            }
        }

        if (nearest == null) return 0.0F;

        // Ближе Siren — сильнее туман.
        float t = 1.0F - (float) (nearestDist / FOG_RADIUS);
        t = t * t * (3.0F - 2.0F * t); // smoothstep
        return t * MAX_STRENGTH;
    }

    /**
     * Плавно двигает currentFogStrength к target.
     * Разная скорость для нарастания и отступления.
     */
    private static void fadeToward(float target) {
        if (currentFogStrength < target) {
            // 🌫️ Наползает — медленно.
            currentFogStrength = Math.min(target, currentFogStrength + FADE_IN_PER_TICK);
        } else if (currentFogStrength > target) {
            // 🌤️ Отступает — чуть быстрее.
            currentFogStrength = Math.max(target, currentFogStrength - FADE_OUT_PER_TICK);
        }
    }

    // =========================================================
    // 🎨 РЕНДЕР ТУМАНА
    // =========================================================
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (currentFogStrength <= 0.001F) return;

        event.setRed(lerp(event.getRed(), FOG_R, currentFogStrength));
        event.setGreen(lerp(event.getGreen(), FOG_G, currentFogStrength));
        event.setBlue(lerp(event.getBlue(), FOG_B, currentFogStrength));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (currentFogStrength <= 0.001F) return;

        float vanillaNear = event.getNearPlaneDistance();
        float vanillaFar  = event.getFarPlaneDistance();

        float targetNear = Mth.lerp(currentFogStrength, FOG_NEAR_MAX, FOG_NEAR_MIN);
        float targetFar  = Mth.lerp(currentFogStrength, FOG_FAR_MAX,  FOG_FAR_MIN);

        float finalNear = Math.min(vanillaNear, targetNear);
        float finalFar  = Math.min(vanillaFar,  targetFar);

        event.setNearPlaneDistance(finalNear);
        event.setFarPlaneDistance(finalFar);
        event.setCanceled(true);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}