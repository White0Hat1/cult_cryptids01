package com.cult.cryptids.mixin;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.client.BloodMoonClientState;
import com.cult.cryptids.client.CinematicTextHandler;
import com.cult.cryptids.client.FlashlightHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
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
    /** 🔍 Итоговый множитель размера луны. */
    private static final @Unique float MAX_SCALE = 4;

    /**
     * Заменяет текстуру Луны на кровавую во время Blood Moon.
     * ⚠️ Именно здесь, а не отдельным MoonTextureMixin — чтобы не плодить лишние файлы.
     */
    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"
            )
    )
    private void bloodMoon$replaceMoonTexture(int shaderTexture, ResourceLocation location) {
        if (BloodMoonClientState.isActive() && VANILLA_MOON.equals(location)) {
            RenderSystem.setShaderTexture(shaderTexture, BLOOD_MOON);
        } else {
            RenderSystem.setShaderTexture(shaderTexture, location);
        }
    }

    @ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 20.0F))
    private float bloodMoon$scaleMoon(float original) {
        if (!BloodMoonClientState.isActive()) return original;

        float progress = CinematicTextHandler.getMoonScaleProgress();
        // Плавно интерполируем от 1.0 до MAX_SCALE
        float scale = 1 + (MAX_SCALE - 1) * progress;
        return original * scale;
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"))
    private void cult$skyColor(float r, float g, float b, float a) {
        if (BloodMoonClientState.isActive()) {
            // "Покраснение" неба.
            float f = CinematicTextHandler.getSkyProgress();
            // 🩸 Красный: R = яркость, G/B = минимум → "кровавый" оттенок.
            float brightness = Math.max(r, Math.max(g, b));
            RenderSystem.setShaderColor(Mth.lerp(f, r, brightness), Mth.lerp(f, g, brightness * 0.08F),
                    Mth.lerp(f, b, brightness * 0.08F), a);
        } else {
            // Обычный ванильный цвет.
            RenderSystem.setShaderColor(r, g, b, a);
        }
    }

    /**
     * 🔦 Дополнительное освещение от фонарика.
     * Работает поверх ванильного — берёт максимум из ванильного и нашего света.
     */
    @Inject(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void getLightColor(BlockAndTintGetter bitg, BlockState state, BlockPos pos,
                                      CallbackInfoReturnable<Integer> cir) {
        int brightness = FlashlightHandler.calculateBrightness(
                Objects.requireNonNull(Minecraft.getInstance().level), pos);
        if (brightness > 0) {
            cir.setReturnValue(LightTexture.pack(
                    Math.max(LightTexture.block(cir.getReturnValue()), brightness),
                    LightTexture.sky(cir.getReturnValue())
            ));
        }
    }
}