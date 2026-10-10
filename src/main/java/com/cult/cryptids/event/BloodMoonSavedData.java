package com.cult.cryptids.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 💾 Сохраняемое состояние Blood Moon.
 * Живёт в сейве мира (папка world/data/cult_cryptids_bloodmoon.dat),
 * поэтому событие переживает перезапуск сервера.
 */
public class BloodMoonSavedData extends SavedData {

    private static final String DATA_NAME = "cult_cryptids_bloodmoon";
    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_REMAINING = "Remaining";

    private boolean active;
    private int remainingTicks;

    /** 📂 Достаёт (или создаёт) данные из сейва Overworld. */
    public static BloodMoonSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                BloodMoonSavedData::load,
                BloodMoonSavedData::new,
                DATA_NAME
        );
    }

    /** 📖 Чтение из NBT при загрузке мира. */
    public static BloodMoonSavedData load(CompoundTag tag) {
        BloodMoonSavedData data = new BloodMoonSavedData();
        data.active = tag.getBoolean(TAG_ACTIVE);
        data.remainingTicks = tag.getInt(TAG_REMAINING);
        return data;
    }

    /** 💾 Запись в NBT. */
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(TAG_ACTIVE, active);
        tag.putInt(TAG_REMAINING, remainingTicks);
        return tag;
    }

    public boolean isActive() { return active; }

    public int getRemainingTicks() { return remainingTicks; }

    public void start(int durationTicks) {
        this.active = true;
        this.remainingTicks = durationTicks;
        setDirty();
    }

    public void tick() {
        if (!active) return;
        if (--remainingTicks <= 0) {
            this.active = false;
            this.remainingTicks = 0;
        }
        setDirty();
    }

    public void stop() {
        this.active = false;
        this.remainingTicks = 0;
        setDirty();
    }
}