package com.cult.cryptids.mixin;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.client.BloodMoonClientState;
import com.cult.cryptids.client.FlashlightHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    // 🌕 Ванильная текстура луны (вшита в Minecraft)
    private static final @Unique ResourceLocation VANILLA_MOON =
            new ResourceLocation("textures/environment/moon_phases.png");
    // 🩸 Наша кровавая текстура (лежит в assets/cult_cryptids/textures/environment/)
    private static final @Unique ResourceLocation BLOOD_MOON =
            new ResourceLocation(CultCryptids.MODID, "textures/environment/blood_moon_phases.png");

    /** Заменяет текстуру Луны на кровавую. **/
    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"))
    private void bloodMoon$replaceMoonTexture(int shaderTexture, ResourceLocation location) {
        if (BloodMoonClientState.isActive() && VANILLA_MOON.equals(location)) RenderSystem.setShaderTexture(shaderTexture, BLOOD_MOON);
        RenderSystem.setShaderTexture(shaderTexture, location);
    }

    /**
     * Перехватываем ВСЕ вызовы RenderSystem.setShaderColor внутри renderSky.
     * Во время Blood Moon любой цвет заменяется на красный оттенок,
     * сохраняя яркость (чтобы градиент неба остался, но стал красным).
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
            float newG = brightness * 0.08F;   // немножко зелёного → "кровавый" оттенок
            float newB = brightness * 0.08F;   // немножко синего — то же

            RenderSystem.setShaderColor(brightness, newG, newB, a);
        } else {
            RenderSystem.setShaderColor(r, g, b, a);
        }
    }

    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("TAIL"), cancellable = true)
    private static void getLightColor(BlockAndTintGetter bitg, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int brightness = FlashlightHandler.calculateBrightness(Objects.requireNonNull(Minecraft.getInstance().level), pos);
        if (brightness > 0) cir.setReturnValue(LightTexture.pack(Math.max(LightTexture.block(cir.getReturnValue()),
                brightness), LightTexture.sky(cir.getReturnValue())));
    }
}
