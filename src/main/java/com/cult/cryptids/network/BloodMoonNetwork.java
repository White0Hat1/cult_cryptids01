package com.cult.cryptids.network;

import com.cult.cryptids.CultCryptids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

        // 🆕 Тряска от удара
        CHANNEL.registerMessage(id++,
                SlamShakePacket.class,
                SlamShakePacket::encode,
                SlamShakePacket::decode,
                SlamShakePacket::handle);
    }

    public static void broadcast(boolean active) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new BloodMoonStatePacket(active));
    }

    public static void sendTo(ServerPlayer player, boolean active) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BloodMoonStatePacket(active));
    }

    public static void broadcastShake(double x, double y, double z, double radius, int duration) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                x, y, z, radius, net.minecraft.world.level.Level.OVERWORLD
        )), new SlamShakePacket(duration));
    }
}