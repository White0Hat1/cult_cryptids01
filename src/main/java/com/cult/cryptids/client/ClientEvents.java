package com.cult.cryptids.client;

import com.cult.cryptids.entity.NewSirenHead;
import com.cult.cryptids.item.M14Item;
import com.cult.cryptids.network.BloodMoonNetwork;
import com.cult.cryptids.network.M14ShootPacket;
import com.cult.cryptids.reg.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!BloodMoonClientState.isActive()) return;
        float skyProgress = CinematicTextHandler.getSkyProgress();
        event.setRed(Mth.lerp(skyProgress, event.getRed(), 0.235f));
        event.setGreen(Mth.lerp(skyProgress, event.getGreen(), 0.027f));
        event.setBlue(Mth.lerp(skyProgress, event.getBlue(), 0.027f));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!BloodMoonClientState.isActive()) return;
        float skyProgress = CinematicTextHandler.getSkyProgress();
        event.setNearPlaneDistance(Mth.lerp(skyProgress, event.getNearPlaneDistance(), 5));
        event.setFarPlaneDistance(Mth.lerp(skyProgress, event.getFarPlaneDistance(), 80));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        SlamShakeClientState.tick();
        M14RecoilClientState.tick();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Перезарядка винтовки.
        // consumeClick() возвращает true, пока есть нажатие в очереди.
        // while — на случай если нажатий несколько.
        while (ModKeyMappings.RELOAD_KEY.consumeClick()) {
            ItemStack mainHand = mc.player.getMainHandItem();
            // ⏱️ Не спамить: если кулдаун ещё активен — пропускаем.
            if (!(mainHand.getItem() instanceof M14Item m14) || mc.player.getCooldowns().isOnCooldown(m14)) continue;
            long now = mc.level.getGameTime();
            // 🎬 Анимация + звук
            m14.markReload(now);
            m14.resetAnimation();
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), ModSounds.M14_RELOAD.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F, false);
            // ⏳ Кулдаун
            mc.player.getCooldowns().addCooldown(m14, 60);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        double time = player.tickCount + event.getPartialTick();
        // Отдача винтовки
        float yaw = M14RecoilClientState.getYaw(), pitch = -M14RecoilClientState.getPitch(), roll = 0;

        // ================= 💥 ДВОЙНАЯ ТРЯСКА ОТ УДАРА =================
        if (SlamShakeClientState.isActive()) {
            int remaining = SlamShakeClientState.getShakeTicks();
            int total = SlamShakeClientState.getTotalDuration();
            // Прогресс: 1.0 в начале → 0.0 в конце
            float progress = (float) remaining / (float) total;
            // Нелинейное затухание — сильнее в начале
            float strength = progress * progress * 12;
            yaw += (float) (Math.sin(time * 47) + Math.sin(time * 23) * 0.6 + Math.sin(time * 97) * 0.35) * strength;
            pitch += (float) (Math.cos(time * 53) * 0.85 + Math.cos(time * 29) * 0.5 + Math.cos(time * 89) * 0.3) * strength;
            roll += (float) (Math.sin(time * 41) * 0.7 + Math.sin(time * 19) * 0.4) * strength;
        }

        // ================= 🏃 ОБЫЧНАЯ ТРЯСКА =================
        else {
            NewSirenHead nearest = null;
            double nearestDist = 30;
            boolean nearestChasing = false;
            for (NewSirenHead siren : player.level().getEntitiesOfClass(NewSirenHead.class, player.getBoundingBox().inflate(30))) {
                if (siren.getDeltaMovement().horizontalDistanceSqr() <= 0.0005 ||
                        siren.getAbility() != NewSirenHead.Ability.NONE) continue;
                double d = siren.distanceTo(player);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = siren;
                    nearestChasing = siren.isAggressive();
                }
            }
            if (nearest == null) return;

            float distanceFactor = (float) (1.0D - nearestDist / 30);
            distanceFactor *= distanceFactor;
            float baseAmplitude = nearestChasing ? 4 : 1.8f;
            double pulsePeriod = nearestChasing ? 6 : 20;
            float cycle = (float) ((time % pulsePeriod) / pulsePeriod);
            float impulse = (float) Math.exp(-cycle * 7.0F);
            float amplitude = baseAmplitude * distanceFactor * impulse;

            yaw += (float) Math.sin(time * 1.2) * amplitude;
            pitch += (float) Math.cos(time) * amplitude * 0.7f;
            roll += (float) Math.sin(time * 1.5) * amplitude * 0.5f;
        }

        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }

    /** Выстрел из винтовки. */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof M14Item m14)) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        if (m14.isDrawing(mc.level.getGameTime()) || M14Item.isOnShootCooldown(player)) return;

        m14.markShoot(mc.level.getGameTime());
        m14.resetAnimation();
        M14RecoilClientState.trigger();
        M14Item.markShotFired(player);
        BloodMoonNetwork.CHANNEL.sendToServer(M14ShootPacket.INSTANCE);
    }
}
