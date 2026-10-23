package com.cult.cryptids.network;

import com.cult.cryptids.CultCryptids;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class BloodMoonNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CultCryptids.MODID, "blood_moon"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++,
                BloodMoonStatePacket.class,
                BloodMoonStatePacket::encode,
                BloodMoonStatePacket::decode,
                BloodMoonStatePacket::handle);

        // 🆕 ЛКМ-выстрел M14
        CHANNEL.registerMessage(id,
                M14ShootPacket.class,
                (packet, buf) -> {},
                buf -> M14ShootPacket.INSTANCE,
                M14ShootPacket::handle);
    }

    public static void broadcast(boolean active, boolean forceSet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new BloodMoonStatePacket(active, forceSet));
    }
}