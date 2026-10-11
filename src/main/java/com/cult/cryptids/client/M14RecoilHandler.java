package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🔫 Применяет накопительную отдачу к камере.
 * Работает каждый кадр, поэтому плавно.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class M14RecoilHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            M14RecoilClientState.tick();
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!M14RecoilClientState.isActive()) return;

        // 📐 Отдача — «смотрим вверх». В Minecraft pitch отрицательный = вверх.
        event.setPitch(event.getPitch() - M14RecoilClientState.getPitch());
        event.setYaw(event.getYaw() + M14RecoilClientState.getYaw());
    }
}