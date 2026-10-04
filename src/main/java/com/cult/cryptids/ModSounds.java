package com.cult.cryptids;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "cult_cryptids");

    // ================= LEGACY =================
    public static final RegistryObject<SoundEvent> SIREN_AMBIENT =
            registerSound("siren_ambient");
    public static final RegistryObject<SoundEvent> SIREN_STEP =
            registerSound("siren_step");
    public static final RegistryObject<SoundEvent> SIREN_SCREAM =
            registerSound("siren_scream");
    public static final RegistryObject<SoundEvent> SIREN_SCREECH =
            registerSound("siren_screech");
    public static final RegistryObject<SoundEvent> SIREN_HURT =
            registerSound("siren_hurt");
    public static final RegistryObject<SoundEvent> SIREN_DEATH =
            registerSound("siren_death");

    // ================= BLOOD MOON =================
    public static final RegistryObject<SoundEvent> BLOOD_MOON_MUSIC =
            registerSound("blood_moon_music");
    public static final RegistryObject<SoundEvent> BLOOD_MOON_IMPACT =
            registerSound("blood_moon_impact");
    public static final RegistryObject<SoundEvent> BLOOD_MOON_ICECREAM =
            registerSound("blood_moon_icecream");

    // ================= AMBIENT СИСТЕМА =================
    public static final RegistryObject<SoundEvent> SIREN_FAR =
            registerSound("siren_far");
    public static final RegistryObject<SoundEvent> SIREN_SEARCH =
            registerSound("siren_search");
    public static final RegistryObject<SoundEvent> SIREN_CLOSE =
            registerSound("siren_close");
    public static final RegistryObject<SoundEvent> SIREN_CHASE_FAR =
            registerSound("siren_chase_far");
    public static final RegistryObject<SoundEvent> SIREN_STATIC =
            registerSound("siren_static");

    // ================= ШАГИ =================
    public static final RegistryObject<SoundEvent> SIREN_WALK =
            registerSound("siren_walk");
    public static final RegistryObject<SoundEvent> SIREN_RUN =
            registerSound("siren_run");

    // ================= 📷 ФОТОАППАРАТ =================
    public static final RegistryObject<SoundEvent> CAMERA_SHUTTER =
            registerSound("camera_shutter");

    // ================= 🔦 ФОНАРИК =================
    public static final RegistryObject<SoundEvent> FLASHLIGHT_TOGGLE =
            registerSound("flashlight_toggle");

    // ================= РЕЧЬ СИРЕНА =================
    public static final RegistryObject<SoundEvent> SIREN_SPEECH =
            registerSound("siren_speech");

    // Утилита — создаёт SoundEvent по имени
    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation("cult_cryptids", name)));
    }
}