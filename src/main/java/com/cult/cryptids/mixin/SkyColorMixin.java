package com.cult.cryptids.mixin;

import com.cult.cryptids.client.BloodMoonClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 🌌 Красит небо во время Blood Moon в красный оттенок.
 * Вне Blood Moon — ванильный цвет.
 *
 * Один @Redirect вместо двух (BloodSkyMixin + NightSkyMixin) —
 * Mixin не разрешает двум @Redirect бить в один и тот же вызов
 * RenderSystem.setShaderColor внутри renderSky.
 */
@Mixin(LevelRenderer.class)
public class SkyColorMixin {

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"
            )
    )
    private void cult$skyColor(float r, float g, float b, float a) {
        if (BloodMoonClientState.isActive()) {
            // 🩸 Красный: R = яркость, G/B = минимум → "кровавый" оттенок.
            float brightness = Math.max(r, Math.max(g, b));
            RenderSystem.setShaderColor(brightness, brightness * 0.08F, brightness * 0.08F, a);
        } else {
            // Обычный ванильный цвет.
            RenderSystem.setShaderColor(r, g, b, a);
        }
    }
}