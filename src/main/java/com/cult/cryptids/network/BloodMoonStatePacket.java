package com.cult.cryptids.network;

import com.cult.cryptids.client.BloodMoonClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BloodMoonStatePacket {
    private final boolean active;

    public BloodMoonStatePacket(boolean active) { this.active = active; }

    public static void encode(BloodMoonStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static BloodMoonStatePacket decode(FriendlyByteBuf buf) {
        return new BloodMoonStatePacket(buf.readBoolean());
    }

    public static void handle(BloodMoonStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> BloodMoonClientState.setActive(msg.active)));
        ctx.get().setPacketHandled(true);
    }
}