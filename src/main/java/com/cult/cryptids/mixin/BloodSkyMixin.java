package com.cult.cryptids.mixin;

import com.cult.cryptids.client.BloodMoonClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class BloodSkyMixin {

    /**
     * Перехватываем ВСЕ вызовы RenderSystem.setShaderColor внутри renderSky.
     * Во время Blood Moon любой цвет заменяется на красный оттенок,
     * сохраняя яркость (чтобы градиент неба остался, но стал красным).
     *
     * Так мы одновременно:
     * - Красим ванильное небо (день/закат/ночь) в красный
     * - Красим солнце в красный
     * - Красим звёзды в красноватый
     * - Оставляем градиент (он формируется через разные яркости r/g/b)
     */
    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"
            )
    )
    private void bloodMoon$redSkyColors(float r, float g, float b, float a) {
        if (BloodMoonClientState.isActive()) {
            // Вычисляем яркость исходного цвета (0.0 — 1.0)
            float brightness = Math.max(r, Math.max(g, b));

            // 🩸 Тёмно-красный для тёмных участков, ярко-красный для светлых
            // Формула: R = яркость, G/B = минимум (даёт оттенок "мяса")
            float newR = brightness * 1.0F;
            float newG = brightness * 0.08F;   // немножко зелёного → "кровавый" оттенок
            float newB = brightness * 0.08F;   // немножко синего — то же

            RenderSystem.setShaderColor(newR, newG, newB, a);
        } else {
            RenderSystem.setShaderColor(r, g, b, a);
        }
    }
}