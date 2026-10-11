package com.cult.cryptids.client;

/**
 * 🔫 Клиентское состояние отдачи M14.
 * Активируется при выстреле, затухает само за несколько тиков.
 */
public class M14RecoilClientState {

    private static int recoilTicks = 0;
    private static int totalDuration = 10;
    private static float yawJitter = 0.0F;

    /** 🎯 Запустить отдачу. yawJitter — случайный боковой сдвиг (-1..1). */
    public static void trigger(int duration) {
        recoilTicks = duration;
        totalDuration = duration;
        yawJitter = (float) ((Math.random() - 0.5) * 2.0);
    }

    public static void tick() {
        if (recoilTicks > 0) recoilTicks--;
    }

    public static boolean isActive() {
        return recoilTicks > 0;
    }

    public static int getRecoilTicks() {
        return recoilTicks;
    }

    public static int getTotalDuration() {
        return totalDuration;
    }

    public static float getYawJitter() {
        return yawJitter;
    }
}