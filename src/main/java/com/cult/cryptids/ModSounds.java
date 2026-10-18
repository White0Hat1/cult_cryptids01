package com.cult.cryptids;

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
            variableRange("blood_moon_music");
    public static final RegistryObject<SoundEvent> BLOOD_MOON_IMPACT =
            variableRange("blood_moon_impact");
    public static final RegistryObject<SoundEvent> BLOOD_MOON_ICECREAM =
            variableRange("blood_moon_icecream");

    // ================= AMBIENT СИСТЕМА =================
    public static final RegistryObject<SoundEvent> SIREN_FAR =
            variableRange("entity.siren.far");
    public static final RegistryObject<SoundEvent> SIREN_SEARCH =
            variableRange("entity.siren.search");
    public static final RegistryObject<SoundEvent> SIREN_CLOSE =
            variableRange("entity.siren.close");
    public static final RegistryObject<SoundEvent> SIREN_CHASE_FAR =
            variableRange("entity.siren.chase_far");
    public static final RegistryObject<SoundEvent> SIREN_STATIC =
            variableRange("entity.siren.static");

    // ================= ШАГИ =================
    public static final RegistryObject<SoundEvent> SIREN_WALK =
            variableRange("entity.siren.walk");
    public static final RegistryObject<SoundEvent> SIREN_RUN =
            variableRange("entity.siren.run");

    // ================= 📷 ФОТОАППАРАТ =================
    public static final RegistryObject<SoundEvent> CAMERA_SHUTTER =
            variableRange("camera_shutter");

    // ================= 🔦 ФОНАРИК =================
    public static final RegistryObject<SoundEvent> FLASHLIGHT_TOGGLE =
            variableRange("flashlight_toggle");

    // ================= 🏃 ТЕМА ПОГОНИ =================
    public static final RegistryObject<SoundEvent> SIREN_CHASE_THEME =
            variableRange("entity.siren.chase_theme");

    // ================= 🔫 M14 =================
    public static final RegistryObject<SoundEvent> M14_SHOOT =
            variableRange("m14_shoot");
    public static final RegistryObject<SoundEvent> M14_RELOAD =
            variableRange("m14_reload");

    // ================= УТИЛИТА =================
    private static RegistryObject<SoundEvent> variableRange(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(CultCryptids.MODID, name)));
    }
}