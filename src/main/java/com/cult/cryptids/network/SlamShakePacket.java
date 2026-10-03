package com.cult.cryptids.network;

import com.cult.cryptids.client.SlamShakeClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SlamShakePacket {
    private final int duration;

    public SlamShakePacket(int duration) {
        this.duration = duration;
    }

    public static void encode(SlamShakePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.duration);
    }

    public static SlamShakePacket decode(FriendlyByteBuf buf) {
        return new SlamShakePacket(buf.readInt());
    }

    public static void handle(SlamShakePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SlamShakeClientState.trigger(msg.duration)));
        ctx.get().setPacketHandled(true);
    }
}