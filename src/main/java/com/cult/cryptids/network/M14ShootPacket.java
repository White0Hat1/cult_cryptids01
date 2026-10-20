package com.cult.cryptids.network;

import com.cult.cryptids.item.M14Item;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 🔫 Пакет: клиент нажал ЛКМ с M14 в руке.
 * Сервер делает hitscan и играет звук.
 */
public class M14ShootPacket {
    public static final M14ShootPacket INSTANCE = new M14ShootPacket();

    private M14ShootPacket() {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof M14Item m14)) return;

            if (M14Item.isOnShootCooldown(player)) return;

            m14.performShot(player.serverLevel(), player);
            M14Item.markShotFired(player);
        });
        ctx.get().setPacketHandled(true);
    }
}