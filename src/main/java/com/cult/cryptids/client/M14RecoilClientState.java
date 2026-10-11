package com.cult.cryptids.client;

/**
 * 🔫 Состояние отдачи M14 — накопительное.
 *
 * Каждый выстрел добавляет отдачу. Со временем она плавно рассеивается.
 * Если стрелять очередью — камера задирается вверх.
 */
public class M14RecoilClientState {

    /** 📐 Сколько градусов добавляет один выстрел вверх. */
    public static final float PITCH_PER_SHOT = 2.5F;

    /** 📐 Максимальный боковой снос за один выстрел. */
    public static final float YAW_PER_SHOT = 0.8F;

    /** 📉 Коэффициент затухания за тик (0.90 = теряем 10% в тик). */
    public static final float DECAY_FACTOR = 0.90F;

    /** 🛑 Максимум отдачи вверх (в градусах). */
    public static final float MAX_PITCH = 15.0F;

    /** 🛑 Максимум бокового сноса (в градусах). */
    public static final float MAX_YAW = 5.0F;

    private static float pitchRecoil = 0.0F;
    private static float yawRecoil = 0.0F;

    public static void trigger() {
        pitchRecoil += PITCH_PER_SHOT;
        yawRecoil += (float) ((Math.random() - 0.5) * 2.0) * YAW_PER_SHOT;

        if (pitchRecoil > MAX_PITCH) pitchRecoil = MAX_PITCH;
        if (yawRecoil > MAX_YAW) yawRecoil = MAX_YAW;
        if (yawRecoil < -MAX_YAW) yawRecoil = -MAX_YAW;
    }

    public static void tick() {
        pitchRecoil *= DECAY_FACTOR;
        yawRecoil *= DECAY_FACTOR;

        if (pitchRecoil < 0.01F) pitchRecoil = 0.0F;
        if (Math.abs(yawRecoil) < 0.01F) yawRecoil = 0.0F;
    }

    public static float getPitch() { return pitchRecoil; }
    public static float getYaw()   { return yawRecoil; }

    public static boolean isActive() {
        return pitchRecoil > 0.01F || Math.abs(yawRecoil) > 0.01F;
    }
}