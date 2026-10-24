package com.cult.cryptids.reg;

import com.cult.cryptids.CultCryptids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "cult_cryptids");

    // ================= BLOOD MOON =================
    public static final RegistryObject<SoundEvent> BLOOD_MOON_MUSIC =
            variableRange("music.blood_moon");
    public static final RegistryObject<SoundEvent> BLOOD_MOON_IMPACT = variableRange("blood_moon_impact");

    // ================= ШАГИ =================
    public static final RegistryObject<SoundEvent> SIREN_WALK =
            variableRange("entity.siren.walk");
    public static final RegistryObject<SoundEvent> SIREN_RUN =
            variableRange("entity.siren.run");

    // ================= 📷 ФОТОАППАРАТ =================
    public static final RegistryObject<SoundEvent> CAMERA_SHUTTER =
            variableRange("item.camera.shutter");

    // ================= 🔦 ФОНАРИК =================
    public static final RegistryObject<SoundEvent> FLASHLIGHT_TOGGLE =
            variableRange("item.flashlight.toggle");

    // ================= 🏃 ТЕМА ПОГОНИ =================
    public static final RegistryObject<SoundEvent> SIREN_CHASE_THEME =
            variableRange("entity.siren.chase");

    // ================= 🔫 M14 =================
    public static final RegistryObject<SoundEvent> M14_SHOOT =
            variableRange("item.m14.shoot");
    public static final RegistryObject<SoundEvent> M14_RELOAD =
            variableRange("item.m14.reload");

    // ================= УТИЛИТА =================
    private static RegistryObject<SoundEvent> variableRange(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(CultCryptids.MODID, name)));
    }
}