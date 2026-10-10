package com.cult.cryptids.event;

import com.cult.cryptids.ModSounds;
import com.cult.cryptids.network.BloodMoonNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

public class BloodMoonEvent {

    public static final int DURATION_TICKS = 24000;
    private static final long START_DAY_TIME = 14000L;

    public static boolean isActive() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        return BloodMoonSavedData.get(server).isActive();
    }

    public static int getRemainingTicks() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        return BloodMoonSavedData.get(server).getRemainingTicks();
    }

    public static void start(MinecraftServer server) {
        BloodMoonSavedData data = BloodMoonSavedData.get(server);
        if (data.isActive()) return;
        data.start(DURATION_TICKS);

        ServerLevel overworld = server.overworld();
        overworld.setDayTime(START_DAY_TIME);

        // 🕐 Если время заморожено — сдвигаем точку заморозки на момент Blood Moon,
        //    иначе TimeFreezeManager вернёт время обратно.
        if (TimeFreezeManager.isFrozen()) {
            TimeFreezeManager.setFrozenDayTime(START_DAY_TIME);
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Level level = player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.BLOOD_MOON_MUSIC.get(),
                    SoundSource.MUSIC, 1.0F, 1.0F);
        }

        BloodMoonNetwork.broadcast(true);
    }

    public static void stop(MinecraftServer server) {
        BloodMoonSavedData data = BloodMoonSavedData.get(server);
        if (!data.isActive()) return;
        data.stop();
        BloodMoonNetwork.broadcast(false);
    }

    public static void tick(MinecraftServer server) {
        BloodMoonSavedData data = BloodMoonSavedData.get(server);
        if (!data.isActive()) return;
        data.tick();
        if (!data.isActive()) {
            BloodMoonNetwork.broadcast(false);
        }
    }
}