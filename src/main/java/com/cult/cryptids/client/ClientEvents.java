package com.cult.cryptids.client;

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
        float skyProgress = BloodMoonClientState.getSkyProgress();
        event.setRed(Mth.lerp(skyProgress, event.getRed(), 0.235f));
        event.setGreen(Mth.lerp(skyProgress, event.getGreen(), 0.027f));
        event.setBlue(Mth.lerp(skyProgress, event.getBlue(), 0.027f));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!BloodMoonClientState.isActive()) return;
        float skyProgress = BloodMoonClientState.getSkyProgress();
        event.setNearPlaneDistance(Mth.lerp(skyProgress, event.getNearPlaneDistance(), 5));
        event.setFarPlaneDistance(Mth.lerp(skyProgress, event.getFarPlaneDistance(), 80));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
        CameraOffsetManager.recoil();
        M14Item.markShotFired(player);
        BloodMoonNetwork.CHANNEL.sendToServer(M14ShootPacket.INSTANCE);
    }
}
