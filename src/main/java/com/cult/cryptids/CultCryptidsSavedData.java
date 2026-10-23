package com.cult.cryptids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 💾 Сохраняемое состояние Blood Moon.
 * Живёт в сейве мира (папка world/data/cult_cryptids.dat),
 * поэтому событие переживает перезапуск сервера.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CultCryptidsSavedData extends SavedData {
    private static final String DATA_NAME = "cult_cryptids";
    private static final String TAG_BLOOD_MOON = "BloodMoon";

    private boolean bloodMoon;

    /** 📂 Достаёт (или создаёт) данные из сейва Overworld. */
    public static CultCryptidsSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                CultCryptidsSavedData::load,
                CultCryptidsSavedData::new,
                DATA_NAME
        );
    }

    /** 📖 Чтение из NBT при загрузке мира. */
    public static CultCryptidsSavedData load(CompoundTag tag) {
        CultCryptidsSavedData data = new CultCryptidsSavedData();
        data.bloodMoon = tag.getBoolean(TAG_BLOOD_MOON);
        return data;
    }

    /** 💾 Запись в NBT. */
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(TAG_BLOOD_MOON, bloodMoon);
        return tag;
    }

    public boolean isBloodMoon() { return bloodMoon; }

    public void startBloodMoon() {
        this.bloodMoon = true;
        setDirty();
    }

    public void stopBloodMoon() {
        this.bloodMoon = false;
        setDirty();
    }
}