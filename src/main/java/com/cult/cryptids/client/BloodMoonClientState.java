package com.cult.cryptids.client;

import net.minecraft.network.chat.Component;

/**
 * Флаг события на клиенте. Управляется пакетом с сервера.
 */
public class BloodMoonClientState {

    private static boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        // 🩸 Переход false → true = Blood Moon только что началась
        if (value && !active) {
            CinematicTextHandler.say(
                    Component.translatable("message.cult_cryptids.blood_moon_start")
            );
        }

        // 🔇 Переход true → false = Blood Moon закончилась, глушим музыку
        if (!value && active) {
            CinematicTextHandler.stopMusic();
        }

        active = value;
    }
}