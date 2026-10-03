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

public class CameraItemRenderer extends GeoItemRenderer<CameraItem> {

    private static final ResourceLocation CAMERA_TEX =
            new ResourceLocation(CultCryptids.MODID, "textures/item/camera_tex.png");

    public CameraItemRenderer() {
        super(new CameraItemModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, CameraItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        String name = bone.getName();

        // 🖐️ Кости рук — скин игрока
        if (isArmBone(name)) {
            AbstractClientPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            boolean isSlim = "slim".equals(player.getModelName());
            boolean isThickBone = name.equals("arm_right_thick") || name.equals("arm_left_thick");
            boolean isSlimBone  = name.equals("arm_right_slim")  || name.equals("arm_left_slim");

            if ((isThickBone && isSlim) || (isSlimBone && !isSlim)) return;

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

        // 📷 Всё остальное — текстура камеры
        RenderType camType = RenderType.entityCutoutNoCull(CAMERA_TEX);
        VertexConsumer camBuffer = bufferSource.getBuffer(camType);

        super.renderRecursively(
                poseStack, animatable, bone,
                camType, bufferSource, camBuffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha
        );
    }

    private static boolean isArmBone(String name) {
        return name.equals("arm_right_thick")
                || name.equals("arm_left_thick")
                || name.equals("arm_right_slim")
                || name.equals("arm_left_slim");
    }
}