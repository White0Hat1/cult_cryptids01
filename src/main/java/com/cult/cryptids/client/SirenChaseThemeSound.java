package com.cult.cryptids.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * 🏃 Зацикленная тема погони Siren Head.
 *
 * Позиция и громкость обновляются СНАРУЖИ через updatePosition() и setVolume().
 * Звук привязан к сущности (двигается вместе с ней).
 */
public class SirenChaseThemeSound extends AbstractSoundInstance {

    public SirenChaseThemeSound(SoundEvent event, Entity tracked) {
        super(event, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());

        this.looping = true;
        this.delay = 0;
        this.volume = 3.0F;
        this.pitch = 1.0F;

        // relative = false → звук в мировых координатах, стерео работает корректно.
        this.relative = false;

        // Attenuation.NONE — чтобы MC не обрезал звук своими 16 блоками.
        this.attenuation = Attenuation.NONE;

        this.x = tracked.getX();
        this.y = tracked.getY();
        this.z = tracked.getZ();
    }

    /** 🎯 Двигаем звук вслед за сущностью. */
    public void updatePosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** 🔊 Меняем громкость каждый тик. */
    public void setVolume(float volume) {
        this.volume = volume;
    }
}