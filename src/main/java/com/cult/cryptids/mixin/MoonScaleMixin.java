package com.cult.cryptids.mixin;

import com.cult.cryptids.client.BloodMoonClientState;
import com.cult.cryptids.client.CinematicTextHandler;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 🌕 Плавно увеличивает луну во время Blood Moon.
 * Рост стартует в момент надписи "BLOOD MOON" и длится 5 секунд.
 */
@Mixin(LevelRenderer.class)
public class MoonScaleMixin {

    /** 🔍 Итоговый множитель размера луны. */
    private static final float MAX_SCALE = 4.0F;

    @ModifyConstant(
            method = "renderSky",
            constant = @Constant(floatValue = 20.0F)
    )
    private float bloodMoon$scaleMoon(float original) {
        if (!BloodMoonClientState.isActive()) return original;

        float progress = CinematicTextHandler.getMoonScaleProgress();
        // Плавно интерполируем от 1.0 до MAX_SCALE
        float scale = 1.0F + (MAX_SCALE - 1.0F) * progress;
        return original * scale;
    }
}