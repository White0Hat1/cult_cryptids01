package com.cult.cryptids.mixin;

import com.cult.cryptids.client.BloodMoonClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class MoonTextureMixin {

    // 🌕 Ванильная текстура луны (вшита в Minecraft)
    private static final ResourceLocation VANILLA_MOON =
            new ResourceLocation("textures/environment/moon_phases.png");

    // 🩸 Наша кровавая текстура (лежит в assets/cult_cryptids/textures/environment/)
    private static final ResourceLocation BLOOD_MOON =
            new ResourceLocation("cult_cryptids", "textures/environment/blood_moon_phases.png");

    /**
     * Перехватываем вызов RenderSystem.setShaderTexture(...) внутри LevelRenderer.renderSky.
     * Если Blood Moon активна и игра пытается нарисовать ванильную луну —
     * подсовываем свою кровавую текстуру.
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
}