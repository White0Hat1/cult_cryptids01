package com.cult.cryptids.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * 🩸 Единоразовый звук «удара» Кровавой Луны.
 *
 * Работает по тому же принципу, что и LoopingMusicSound:
 *   - SoundSource.MASTER → не глушится настройками Music/Ambient
 *   - Attenuation.NONE → слышно по всему миру
 *   - relative = true → играет локально у игрока
 *
 * Отличие: looping = false — играет один раз и заканчивается.
 */
public class BloodMoonImpactSound extends AbstractSoundInstance {

    /** Множитель громкости. 2.0 = вдвое громче обычного. */
    private static final float VOLUME = 2.0F;

    public BloodMoonImpactSound(SoundEvent event) {
        super(event, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.delay = 0;
        this.volume = VOLUME;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }
}