package com.cult.cryptids.client;

/**
 * Клиентское состояние тряски от удара по земле.
 * Активируется пакетом с сервера.
 */
public class SlamShakeClientState {
    private static int shakeTicks = 0;
    private static int totalDuration = 20;

    public static void trigger(int duration) {
        shakeTicks = duration;
        totalDuration = duration;
    }

    public static void tick() {
        if (shakeTicks > 0) shakeTicks--;
    }

    public static boolean isActive() {
        return shakeTicks > 0;
    }

    public static int getShakeTicks() {
        return shakeTicks;
    }

    public static int getTotalDuration() {
        return totalDuration;
    }
}