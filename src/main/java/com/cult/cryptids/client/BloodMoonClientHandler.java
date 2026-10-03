package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import net.minecraft.util.Mth;
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

    // 🌫️ Плотность тумана (5–80 блоков)
    private static final float FOG_NEAR = 5.0F;
    private static final float FOG_FAR  = 80.0F;

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!BloodMoonClientState.isActive()) return;

        float skyProgress = CinematicTextHandler.getSkyProgress();

        event.setRed(lerp(event.getRed(), FOG_R, skyProgress));
        event.setGreen(lerp(event.getGreen(), FOG_G, skyProgress));
        event.setBlue(lerp(event.getBlue(), FOG_B, skyProgress));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!BloodMoonClientState.isActive()) return;

        float skyProgress = CinematicTextHandler.getSkyProgress();

        float targetNear = Mth.lerp(skyProgress, event.getNearPlaneDistance(), FOG_NEAR);
        float targetFar  = Mth.lerp(skyProgress, event.getFarPlaneDistance(), FOG_FAR);

        event.setNearPlaneDistance(targetNear);
        event.setFarPlaneDistance(targetFar);
        event.setCanceled(true);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}