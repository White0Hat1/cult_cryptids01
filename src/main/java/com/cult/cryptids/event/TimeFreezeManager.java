package com.cult.cryptids.event;

import com.cult.cryptids.CultCryptids;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🕐 Заморозка времени неба (цикла дня/ночи) на заданном тике.
 *
 * — gameTime продолжает идти (редстоун, кулдауны, тики) — не ломаем ничего.
 * — dayTime зафиксирован на значении, которое задал игрок.
 * — Blood Moon может обновить точку заморозки.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeFreezeManager {

    private static boolean frozen = false;
    private static long frozenDayTime = 0;

    private TimeFreezeManager() {}

    public static boolean isFrozen() {
        return frozen;
    }

    public static long getFrozenDayTime() {
        return frozenDayTime;
    }

    /**
     * 🧊 Замораживает цикл дня/ночи на указанном тике.
     * Сразу же выставляет dayTime, чтобы игрок увидел результат мгновенно.
     */
    public static void freezeAt(MinecraftServer server, long dayTime) {
        frozen = true;
        frozenDayTime = dayTime;
        server.overworld().setDayTime(dayTime);
    }

    /** 🔄 Снимает заморозку — время снова идёт своим ходом. */
    public static void unfreeze() {
        frozen = false;
    }

    /**
     * 🎯 Обновить точку заморозки (используется Blood Moon).
     */
    public static void setFrozenDayTime(long dayTime) {
        frozenDayTime = dayTime;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (!sl.dimension().equals(Level.OVERWORLD)) return;
        if (!frozen) return;

        if (sl.getDayTime() != frozenDayTime) {
            sl.setDayTime(frozenDayTime);
        }
    }
}