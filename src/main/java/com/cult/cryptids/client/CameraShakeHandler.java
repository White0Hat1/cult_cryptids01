package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CameraShakeHandler {

    private static final double SHAKE_RADIUS = 30.0D;

    private static final float BASE_AMPLITUDE_WALK = 1.8F;
    private static final float BASE_AMPLITUDE_RUN  = 4.0F;

    private static final double PULSE_PERIOD_WALK = 20.0D;
    private static final double PULSE_PERIOD_RUN  = 6.0D;

    // 💥 Тряска от удара — ДВОЙНАЯ по силе
    private static final float SLAM_AMPLITUDE = 12.0F;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SlamShakeClientState.tick();
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        double time = player.tickCount + event.getPartialTick();

        // ================= 💥 ДВОЙНАЯ ТРЯСКА ОТ УДАРА =================
        if (SlamShakeClientState.isActive()) {
            int remaining = SlamShakeClientState.getShakeTicks();
            int total = SlamShakeClientState.getTotalDuration();

            // Прогресс: 1.0 в начале → 0.0 в конце
            float progress = (float) remaining / (float) total;

            // Нелинейное затухание — сильнее в начале
            float strength = progress * progress * SLAM_AMPLITUDE;

            // 🔥 СЛОЙ 1: быстрые хаотичные колебания (главная тряска)
            float yaw1   = (float) Math.sin(time * 47.0D) * strength;
            float pitch1 = (float) Math.cos(time * 53.0D) * strength * 0.85F;
            float roll1  = (float) Math.sin(time * 41.0D) * strength * 0.7F;

            // 🔥 СЛОЙ 2: медленная составляющая (дубликат для усиления)
            float yaw2   = (float) Math.sin(time * 23.0D) * strength * 0.6F;
            float pitch2 = (float) Math.cos(time * 29.0D) * strength * 0.5F;
            float roll2  = (float) Math.sin(time * 19.0D) * strength * 0.4F;

            // 🔥 СЛОЙ 3: сверхбыстрый шум для "землетрясения"
            float yaw3   = (float) Math.sin(time * 97.0D) * strength * 0.35F;
            float pitch3 = (float) Math.cos(time * 89.0D) * strength * 0.3F;

            // Суммируем все слои — получаем рваную, хаотичную тряску
            event.setYaw(event.getYaw() + yaw1 + yaw2 + yaw3);
            event.setPitch(event.getPitch() + pitch1 + pitch2 + pitch3);
            event.setRoll(event.getRoll() + roll1 + roll2);
            return;
        }

        // ================= 🏃 ОБЫЧНАЯ ТРЯСКА =================
        SirenHeadEntity nearest = null;
        double nearestDist = SHAKE_RADIUS;
        boolean nearestChasing = false;

        for (SirenHeadEntity siren : player.level().getEntitiesOfClass(
                SirenHeadEntity.class,
                player.getBoundingBox().inflate(SHAKE_RADIUS))) {

            boolean isMoving = siren.getDeltaMovement().horizontalDistanceSqr() > 0.0005;
            if (!isMoving) continue;
            if (siren.isGrabbing() || siren.isSlamming()) continue;

            double d = siren.distanceTo(player);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = siren;
                nearestChasing = siren.isChasing();
            }
        }

        if (nearest == null) return;

        float distanceFactor = (float) (1.0D - nearestDist / SHAKE_RADIUS);
        distanceFactor *= distanceFactor;

        float baseAmplitude = nearestChasing ? BASE_AMPLITUDE_RUN : BASE_AMPLITUDE_WALK;
        double pulsePeriod  = nearestChasing ? PULSE_PERIOD_RUN   : PULSE_PERIOD_WALK;

        float cycle = (float) ((time % pulsePeriod) / pulsePeriod);
        float impulse = (float) Math.exp(-cycle * 7.0F);
        float amplitude = baseAmplitude * distanceFactor * impulse;

        float yawShake   = (float) Math.sin(time * 1.2D) * amplitude;
        float pitchShake = (float) Math.cos(time * 1.0D) * amplitude * 0.7F;
        float rollShake  = (float) Math.sin(time * 1.5D) * amplitude * 0.5F;

        event.setYaw(event.getYaw() + yawShake);
        event.setPitch(event.getPitch() + pitchShake);
        event.setRoll(event.getRoll() + rollShake);
    }
}