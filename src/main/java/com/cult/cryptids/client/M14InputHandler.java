package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.ModSounds;
import com.cult.cryptids.item.M14Item;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ⌨️ Обработчик нажатий клавиш для M14.
 *   R — перезарядка.
 *
 * Работает на клиенте — анимация и звук локальные.
 * На сервере никаких действий (пока нет системы патронов).
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class M14InputHandler {

    private static final int RELOAD_ANIM_TICKS = 60;

    private M14InputHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // consumeClick() возвращает true пока есть нажатие в очереди.
        // while — на случай если нажатий несколько.
        while (ModKeyMappings.RELOAD_KEY.consumeClick()) {
            ItemStack mainHand = mc.player.getMainHandItem();
            if (!(mainHand.getItem() instanceof M14Item m14)) continue;

            // ⏱️ Не спамить: если кулдаун ещё активен — пропускаем.
            if (mc.player.getCooldowns().isOnCooldown(m14)) continue;

            long now = mc.level.getGameTime();

            // 🎬 Анимация + звук
            m14.markReload(now);
            m14.resetAnimation();

            mc.level.playLocalSound(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    ModSounds.M14_RELOAD.get(), SoundSource.PLAYERS,
                    1.0F, 1.0F, false
            );

            // ⏳ Кулдаун
            mc.player.getCooldowns().addCooldown(m14, RELOAD_ANIM_TICKS);
        }
    }
}