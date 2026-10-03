package com.cult.cryptids.client;

/**
 * Флаг события на клиенте. Управляется пакетом с сервера.
 */
public class BloodMoonClientState {
    private static boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}