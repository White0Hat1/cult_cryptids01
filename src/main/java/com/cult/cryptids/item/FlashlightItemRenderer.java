package com.cult.cryptids.item;

import com.cult.cryptids.CultCryptids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class FlashlightItemRenderer extends GeoItemRenderer<FlashlightItem> {

    private static final ResourceLocation FLASHLIGHT_TEX =
            new ResourceLocation(CultCryptids.MODID, "textures/item/flashlight_tex.png");

    public FlashlightItemRenderer() {
        super(new FlashlightItemModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FlashlightItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        String name = bone.getName();

        // =====================================================
        // 🖐️ КОСТИ РУК — рисуем СКИНОМ игрока
        // =====================================================
        if (isArmBone(name)) {
            AbstractClientPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            // Скипаем неподходящие под модель игрока руки (slim vs thick)
            boolean isSlim = "slim".equals(player.getModelName());
            boolean isThickBone = name.equals("arm_right_thick") || name.equals("arm_left_thick");
            boolean isSlimBone  = name.equals("arm_right_slim")  || name.equals("arm_left_slim");

            if ((isThickBone && isSlim) || (isSlimBone && !isSlim)) {
                return; // эта рука не для нашего типа игрока
            }

            // Подменяем RenderType + VertexConsumer на скин
            RenderType skinType = RenderType.entityTranslucent(player.getSkinTextureLocation());
            VertexConsumer skinBuffer = bufferSource.getBuffer(skinType);

            super.renderRecursively(
                    poseStack, animatable, bone,
                    skinType, bufferSource, skinBuffer,
                    isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha
            );
            return;
        }

        // =====================================================
        // 🔦 ВСЁ ОСТАЛЬНОЕ — текстура фонарика
        // =====================================================
        RenderType flType = RenderType.entityCutoutNoCull(FLASHLIGHT_TEX);
        VertexConsumer flBuffer = bufferSource.getBuffer(flType);

        super.renderRecursively(
                poseStack, animatable, bone,
                flType, bufferSource, flBuffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha
        );
    }

    /** Проверяем имя кости — это рука? */
    private static boolean isArmBone(String name) {
        return name.equals("arm_right_thick")
                || name.equals("arm_left_thick")
                || name.equals("arm_right_slim")
                || name.equals("arm_left_slim");
    }
}