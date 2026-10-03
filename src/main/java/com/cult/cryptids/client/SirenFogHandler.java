package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🌫️ Серый туман в радиусе от Сиреноголового.
 * Чем ближе игрок к Сирену — тем плотнее и темнее туман.
 * Не срабатывает во время Blood Moon (там свой красный туман).
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SirenFogHandler {

    // 📏 Радиус тумана (в блоках)
    private static final double FOG_RADIUS = 100.0D;

    // 🎨 Цвет тумана (серый)
    private static final float FOG_R = 0.35F;
    private static final float FOG_G = 0.35F;
    private static final float FOG_B = 0.35F;

    // 📐 Плотность: на какой дистанции туман начинается и где он полностью закрашивает
    // На границе радиуса туман невидим, вблизи Сирена — очень плотный
    private static final float FOG_NEAR_MIN = 0.5F;   // вблизи Сирена (очень близко)
    private static final float FOG_FAR_MIN  = 8.0F;   // вблизи Сирена (далеко закрашен)
    private static final float FOG_NEAR_MAX = 40.0F;  // на границе радиуса (далеко начинается)
    private static final float FOG_FAR_MAX  = 120.0F; // на границе радиуса (поздно заканчивается)

    // 🎚️ Максимальная сила тумана у Сирена (0.0 – 1.0)
    private static final float MAX_STRENGTH = 0.85F;

    /**
     * Возвращает 0.0 (игрок далеко) → 1.0 (игрок вплотную к Сирену).
     * Если Сирен не найден или Blood Moon активна — 0.0.
     */
    private static float getSirenFogStrength(Player player) {
        // Не срабатываем во время Blood Moon — там свой туман
        if (BloodMoonClientState.isActive()) return 0.0F;

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

        // 1.0 у Сирена → 0.0 на границе радиуса, сглаживание smoothstep
        float t = 1.0F - (float) (nearestDist / FOG_RADIUS);
        t = t * t * (3.0F - 2.0F * t);
        return t * MAX_STRENGTH;
    }

    // 🎨 Цвет тумана
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        float strength = getSirenFogStrength(player);
        if (strength <= 0.0F) return;

        event.setRed(lerp(event.getRed(), FOG_R, strength));
        event.setGreen(lerp(event.getGreen(), FOG_G, strength));
        event.setBlue(lerp(event.getBlue(), FOG_B, strength));
    }

    // 🌫️ Плотность тумана
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        float strength = getSirenFogStrength(player);
        if (strength <= 0.0F) return;

        // Смешиваем ванильные near/far с нашими значениями
        float vanillaNear = event.getNearPlaneDistance();
        float vanillaFar  = event.getFarPlaneDistance();

        float targetNear = Mth.lerp(strength, FOG_NEAR_MAX, FOG_NEAR_MIN);
        float targetFar  = Mth.lerp(strength, FOG_FAR_MAX,  FOG_FAR_MIN);

        // Берём минимум с ванильным — туман никогда не становится реже
        float finalNear = Math.min(vanillaNear, targetNear);
        float finalFar  = Math.min(vanillaFar,  targetFar);

        event.setNearPlaneDistance(finalNear);
        event.setFarPlaneDistance(finalFar);
        event.setCanceled(true); // блокируем дальнейшие модификации тумана
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}