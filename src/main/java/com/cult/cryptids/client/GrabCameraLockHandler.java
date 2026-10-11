package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🎥 Когда Siren держит игрока в attack_grab — плавно поворачиваем
 * самого игрока в сторону Siren. Камера следует за поворотом.
 *
 * Не сбрасываем yRotO/xRotO — иначе Minecraft не интерполирует угол
 * между тиками, и камера «прыгает».
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GrabCameraLockHandler {

    /** 🎚️ Скорость доворота за тик (0..1). Чем меньше — тем плавнее. */
    private static final float LERP_SPEED = 0.12F;

    /** 📏 Мёртвая зона — если угол меньше этого, не дёргаем (в градусах). */
    private static final float DEAD_ZONE = 0.5F;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        SirenHeadEntity grabber = findGrabber(player, mc);
        if (grabber == null) return;

        // 🎯 Куда смотреть — на уровень груди Siren
        double dx = grabber.getX() - player.getX();
        double dy = (grabber.getY() + grabber.getBbHeight() * 0.7D) - player.getEyeY();
        double dz = grabber.getZ() - player.getZ();

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float targetPitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz))
                * (180.0D / Math.PI));

        // 📐 Плавный доворот с wrapDegrees — чтобы не «прокручивало» через ±180
        float currentYaw = player.getYRot();
        float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        float currentPitch = player.getXRot();
        float pitchDelta = targetPitch - currentPitch;

        // 🛑 Мёртвая зона — не дёргаем на микроскопических углах
        float newYaw = Math.abs(yawDelta) < DEAD_ZONE
                ? currentYaw
                : currentYaw + yawDelta * LERP_SPEED;

        float newPitch = Math.abs(pitchDelta) < DEAD_ZONE
                ? currentPitch
                : Mth.lerp(LERP_SPEED, currentPitch, targetPitch);

        newPitch = Mth.clamp(newPitch, -90.0F, 90.0F);

        // 🎯 Устанавливаем только yRot и xRot — БЕЗ сброса yRotO/xRotO.
        //    Minecraft сам интерполирует между старым и новым углом за partialTick.
        player.setYRot(newYaw);
        player.setYHeadRot(newYaw);
        player.setXRot(newPitch);
    }

    private static SirenHeadEntity findGrabber(Player player, Minecraft mc) {
        int playerId = player.getId();
        for (SirenHeadEntity siren : mc.level.getEntitiesOfClass(
                SirenHeadEntity.class,
                player.getBoundingBox().inflate(64.0D))) {
            if (siren.isGrabbing() && siren.getGrabbedTargetId() == playerId) {
                return siren;
            }
        }
        return null;
    }
}