package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🔫 Отдача M14.
 * Камера подлетает вверх при выстреле и плавно возвращается.
 * Работает независимо от CameraShakeHandler — эффекты складываются.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class M14RecoilHandler {

    /** 📐 Сила подброса камеры вверх (в градусах). */
    private static final float RECOIL_PITCH = 4.5F;

    /** 📐 Максимальный боковой сдвиг (в градусах, случайный ±). */
    private static final float RECOIL_YAW_MAX = 0.8F;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            M14RecoilClientState.tick();
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!M14RecoilClientState.isActive()) return;

        int remaining = M14RecoilClientState.getRecoilTicks();
        int total = M14RecoilClientState.getTotalDuration();
        if (total <= 0) return;

        // 📉 progress: 1.0 в начале → 0.0 в конце
        float p = (float) remaining / (float) total;

        // 🎢 Квадратичное затухание — резкий подброс, плавный возврат
        float kick = p * p;

        // ⬆️ В Minecraft pitch отрицательный = смотреть вверх
        float pitchDelta = -RECOIL_PITCH * kick;

        // ↔️ Лёгкий боковой сдвиг со случайным знаком
        float yawDelta = M14RecoilClientState.getYawJitter() * RECOIL_YAW_MAX * kick;

        event.setPitch(event.getPitch() + pitchDelta);
        event.setYaw(event.getYaw() + yawDelta);
    }
}