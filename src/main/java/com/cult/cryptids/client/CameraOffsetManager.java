package com.cult.cryptids.client;

import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class CameraOffsetManager {
    private static final RandomSource random = RandomSource.create(Util.getNanos());
    private static long shakeSeed;
    private static float shakeAmplitude;
    private static int shakeTicks;
    private static int recoil;

    public static void screenShake(float amplitude, int ticks) {
        shakeSeed = random.nextLong();
        shakeAmplitude = Math.max(shakeAmplitude, amplitude);
        shakeTicks = Math.max(shakeTicks, ticks);
    }

    public static void recoil() {
        recoil = 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END) return;
        if (shakeTicks > 0 && --shakeTicks == 0) shakeAmplitude = 0;
        if (recoil < 10) recoil++;
    }

    @SubscribeEvent
    public static void compute(ViewportEvent.ComputeCameraAngles event) {
        float yaw = event.getYaw(), pitch = event.getPitch(), roll = event.getRoll();

        if (shakeTicks > 0) {
            random.setSeed(shakeSeed);
            float ticks = shakeTicks - (float) event.getPartialTick();
            yaw += shakeAmplitude * (random.nextFloat() * Mth.sin(ticks * rngV()) - random.nextFloat() * Mth.sin(ticks * rngV()));
            pitch += shakeAmplitude * (random.nextFloat() * Mth.sin(ticks * rngV()) - random.nextFloat() * Mth.sin(ticks * rngV()));
            roll += shakeAmplitude * 0.5f * (random.nextFloat() * Mth.sin(ticks * rngV()) - random.nextFloat() * Mth.sin(ticks * rngV()));
        }

        if (recoil < 10) pitch -= 22.5f * (float) Math.sin((recoil + event.getPartialTick()) * 0.1 * Math.PI);

        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(roll);
    }

    /** Случайное число, подходящее под скорость тряски */
    private static float rngV() {
        float f = random.nextFloat();
        return f < 0.5f ? f + 0.5f : f * 2;
    }
}
