package com.cult.cryptids.client;

import com.cult.cryptids.client.sound.SirenChaseSoundInstance;
import com.cult.cryptids.entity.NewSirenHead;
import net.minecraft.client.Minecraft;

public class ClientUtil {
    public static void sirenChase(NewSirenHead entity) {
        Minecraft.getInstance().getSoundManager().play(new SirenChaseSoundInstance(entity));
    }
}