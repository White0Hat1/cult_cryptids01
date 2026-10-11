package com.cult.cryptids.event;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.item.M14Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🛡️ Запрещает ванильный удар по мобу, если в руке M14.
 * Настоящий урон идёт через M14ShootPacket → performShot().
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class M14ServerAttackHandler {

    private M14ServerAttackHandler() {}

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof M14Item)) return;

        // 🚫 Никакого рукопашного урона — только выстрел через пакет
        event.setCanceled(true);
    }
}