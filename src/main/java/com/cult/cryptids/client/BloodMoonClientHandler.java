package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BloodMoonClientHandler {

    // 🩸 Красно-коричневый цвет неба
    private static final float FOG_R = 0.45F;
    private static final float FOG_G = 0.03F;
    private static final float FOG_B = 0.03F;

    // 🌫️ Плотность тумана (5–80 блоков) — небо становится частью тумана
    private static final float FOG_NEAR = 5.0F;
    private static final float FOG_FAR  = 80.0F;

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!BloodMoonClientState.isActive()) return;
        event.setRed(FOG_R);
        event.setGreen(FOG_G);
        event.setBlue(FOG_B);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!BloodMoonClientState.isActive()) return;
        event.setNearPlaneDistance(FOG_NEAR);
        event.setFarPlaneDistance(FOG_FAR);
        event.setCanceled(true);
    }
}