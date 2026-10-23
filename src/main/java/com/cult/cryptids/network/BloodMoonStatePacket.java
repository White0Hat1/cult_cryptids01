package com.cult.cryptids.network;

import com.cult.cryptids.client.BloodMoonClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BloodMoonStatePacket(boolean active, boolean forceSet) {
    public static void encode(BloodMoonStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.forceSet);
    }

    public static BloodMoonStatePacket decode(FriendlyByteBuf buf) {
        return new BloodMoonStatePacket(buf.readBoolean(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BloodMoonClientState.setActive(active, forceSet)));
        ctx.get().setPacketHandled(true);
    }
}