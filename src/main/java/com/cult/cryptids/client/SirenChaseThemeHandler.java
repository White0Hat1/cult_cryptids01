package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.ModSounds;
import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 🏃 Управляет темой погони на клиенте.
 *
 * Логика:
 *   - Ищем ближайшего Siren Head с isChasing() == true в радиусе 40 блоков.
 *   - Громкость затухает плавно: 10 блоков → максимум, 40 блоков → тишина.
 *   - Звук двигается вместе с мобом.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SirenChaseThemeHandler {

    /** Радиус, в котором игрок слышит тему погони. */
    private static final double HEAR_RADIUS = 40.0D;

    /** Ближе этой дистанции громкость максимальная. */
    private static final double MIN_DISTANCE = 10.0D;

    /** Дальше этой дистанции звук не слышен. */
    private static final double MAX_DISTANCE = 40.0D;

    /** Максимальная громкость вблизи. */
    private static final float MAX_VOLUME = 3.0F;

    private static SirenChaseThemeSound activeSound = null;
    private static UUID activeEntityUuid = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stop();
            return;
        }

        SirenHeadEntity target = null;
        double nearestDist = Double.MAX_VALUE;

        for (SirenHeadEntity siren : mc.level.getEntitiesOfClass(
                SirenHeadEntity.class,
                mc.player.getBoundingBox().inflate(HEAR_RADIUS))) {

            if (!siren.isChasing()) continue;
            if (siren.isGrabbing() || siren.isSlamming() || siren.isReaching()) continue;

            double d = siren.distanceTo(mc.player);
            if (d < nearestDist) {
                nearestDist = d;
                target = siren;
            }
        }

        // Никого нет — глушим.
        if (target == null) {
            stop();
            return;
        }

        // Смена цели — пересоздаём звук.
        if (activeSound == null || activeEntityUuid == null
                || !activeEntityUuid.equals(target.getUUID())) {
            stop();
            activeSound = new SirenChaseThemeSound(ModSounds.SIREN_CHASE_THEME.get(), target);
            activeEntityUuid = target.getUUID();
            mc.getSoundManager().play(activeSound);
        }

        // 🎯 Каждый тик — обновляем позицию и громкость.
        activeSound.updatePosition(target.getX(), target.getY(), target.getZ());

        // 📉 Плавное затухание:
        //    dist <= 10 → 1.0
        //    dist >= 40 → 0.0
        float t = (float) ((MAX_DISTANCE - nearestDist) / (MAX_DISTANCE - MIN_DISTANCE));
        t = Mth.clamp(t, 0.0F, 1.0F);

        // 🎢 Smoothstep.
        t = t * t * (3.0F - 2.0F * t);

        activeSound.setVolume(MAX_VOLUME * t);
    }

    private static void stop() {
        if (activeSound != null) {
            Minecraft.getInstance().getSoundManager().stop(activeSound);
            activeSound = null;
        }
        activeEntityUuid = null;
    }
}