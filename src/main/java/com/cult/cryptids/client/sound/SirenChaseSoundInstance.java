package com.cult.cryptids.client.sound;

import com.cult.cryptids.entity.NewSirenHead;
import com.cult.cryptids.reg.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * 🏃 Зацикленная тема погони Siren Head.
 * Позиция и громкость обновляются СНАРУЖИ через updatePosition() и setVolume().
 * Звук привязан к сущности (двигается вместе с ней).
 */
public class SirenChaseSoundInstance extends AbstractTickableSoundInstance {
    private final NewSirenHead tracked;

    public SirenChaseSoundInstance(NewSirenHead tracked) {
        super(ModSounds.SIREN_CHASE_THEME.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.tracked = tracked;
        looping = true;
        this.attenuation = Attenuation.NONE; // Чтобы MC не обрезал звук своими 16 блоками.
    }

    @Override
    public void tick() {
        if (tracked.isRemoved() || !tracked.playsChase()) stop();
        else if (tracked.getAbility() == NewSirenHead.Ability.GRAB) volume = 0;
        else {
            volume = 3;
            x = tracked.getX();
            y = tracked.getY();
            z = tracked.getZ();
        }
    }
}