package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.item.M14Item;
import com.cult.cryptids.network.BloodMoonNetwork;
import com.cult.cryptids.network.M14ShootPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🔫 ЛКМ-выстрел M14.
 *
 * Отменяет ванильный удар, играет анимацию, отправляет пакет на сервер.
 * Кулдаун — свой, без ванильного оверлея (см. M14Item.isOnShootCooldown).
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class M14AttackHandler {

    private M14AttackHandler() {}

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

        if (m14.isDrawing(mc.level.getGameTime())) return;
        if (M14Item.isOnShootCooldown(player)) return;

        m14.markShoot(mc.level.getGameTime());
        m14.resetAnimation();
        M14RecoilClientState.trigger();

        M14Item.markShotFired(player);

        BloodMoonNetwork.CHANNEL.sendToServer(new M14ShootPacket());
    }
}