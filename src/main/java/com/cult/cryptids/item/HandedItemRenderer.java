package com.cult.cryptids.item;

import com.cult.cryptids.CultCryptids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

public class HandedItemRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {
    private static final String[] ARM_BONES = {"arm_right_thick", "arm_left_thick", "arm_right_slim", "arm_left_slim"};

    public HandedItemRenderer(GeoModel<T> model) {
        super(model);
        addRenderLayer(new ArmsLayer<>(this));
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        for (String s: ARM_BONES) model.getBone(s).ifPresent(bone -> bone.setHidden(true));
    }

    public static HandedItemRenderer<CameraItem> camera() {
        return new HandedItemRenderer<>(new DefaultedItemGeoModel<>(new ResourceLocation(CultCryptids.MODID, "camera")));
    }

    public static HandedItemRenderer<FlashlightItem> flashlight() {
        return new HandedItemRenderer<>(new DefaultedItemGeoModel<>(new ResourceLocation(CultCryptids.MODID, "flashlight")));
    }

    public static class ArmsLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
        public ArmsLayer(GeoRenderer<T> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            boolean right = "RightArm".equals(bone.getName()), left = "LeftArm".equals(bone.getName());
            if (right || left) {
                AbstractClientPlayer player = Minecraft.getInstance().player;
                if (player != null && Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player)
                        instanceof LivingEntityRenderer<?,?> livingEntityRenderer && livingEntityRenderer.getModel()
                        instanceof PlayerModel<?> playerModel) {
                    ResourceLocation texture = player.getSkinTextureLocation();
                    RenderType solid = RenderType.entitySolid(texture), translucent = RenderType.entityTranslucent(texture);
                    poseStack.pushPose();
                    RenderUtils.translateToPivotPoint(poseStack, bone);
                    poseStack.scale(-1, -1, 1);
                    if (right) {
                        playerModel.rightArm.loadPose(PartPose.ZERO);
                        playerModel.rightArm.render(poseStack, bufferSource.getBuffer(solid), packedLight, packedOverlay);
                        playerModel.rightSleeve.loadPose(PartPose.ZERO);
                        playerModel.rightSleeve.render(poseStack, bufferSource.getBuffer(translucent), packedLight, packedOverlay);
                    } else {
                        playerModel.leftArm.loadPose(PartPose.ZERO);
                        playerModel.leftArm.render(poseStack, bufferSource.getBuffer(solid), packedLight, packedOverlay);
                        playerModel.leftSleeve.loadPose(PartPose.ZERO);
                        playerModel.leftSleeve.render(poseStack, bufferSource.getBuffer(translucent), packedLight, packedOverlay);
                    }
                    poseStack.popPose();
                    bufferSource.getBuffer(renderType);
                }
            }
        }
    }
}