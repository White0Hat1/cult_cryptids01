package com.cult.cryptids.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * 🎵 Зацикленная музыка. Играет бесконечно, пока не остановим вручную.
 * Категория — MASTER, чтобы не конфликтовать с ванильным MusicManager.
 * Громкость — 2.0 (x2 от обычной).
 */
public class LoopingMusicSound extends AbstractSoundInstance {

    /** Множитель громкости. 1.0 = норма, 2.0 = вдвое громче. */
    private static final float VOLUME_MULTIPLIER = 2.0F;

    public LoopingMusicSound(SoundEvent event) {
        super(event, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = VOLUME_MULTIPLIER;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }
}