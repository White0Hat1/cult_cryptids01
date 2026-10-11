package com.cult.cryptids.network;

import com.cult.cryptids.item.M14Item;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 🔫 Пакет: клиент нажал ЛКМ с M14 в руке.
 * Сервер делает hitscan и играет звук.
 */
public class M14ShootPacket {

    public M14ShootPacket() {}

    public static void encode(M14ShootPacket msg, FriendlyByteBuf buf) {
        // Ничего не передаём
    }

    public static M14ShootPacket decode(FriendlyByteBuf buf) {
        return new M14ShootPacket();
    }

    public static void handle(M14ShootPacket msg, Supplier<NetworkEvent.Context> ctx) {
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