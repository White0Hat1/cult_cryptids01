package com.cult.cryptids.client;

import com.cult.cryptids.item.CameraItem;
import com.cult.cryptids.item.CameraItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "cult_cryptids",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class CameraHandRenderer {

    private static final CameraItemRenderer CAMERA_RENDERER = new CameraItemRenderer();

    // Стандартные параметры FP-модели
    private static final float SCALE = 1.0F;
    private static final float POS_X = -0.50F;
    private static final float POS_Y = -0.50F;
    private static final float POS_Z = -0.10F;
    private static final float ROT_X = 0.0F;
    private static final float ROT_Y = 0.0F;
    private static final float ROT_Z = 0.0F;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = event.getItemStack();

        // Работаем только если в главной руке камера
        if (!(stack.getItem() instanceof CameraItem)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        // Отменяем ванильный рендер
        event.setCanceled(true);

        AbstractClientPlayer player = mc.player;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();

        float equipProgress = event.getEquipProgress();
        float swingProgress = event.getSwingProgress();

        HumanoidArm arm = player.getMainArm();
        boolean right = arm == HumanoidArm.RIGHT;

        poseStack.pushPose();

        // 1. Ставим руку в первое лицо
        renderPlayerArm(poseStack, equipProgress, swingProgress, arm);

        // 2. Рисуем ванильную руку игрока (скин Steve/Alex)
        if (!player.isInvisible()) {
            PlayerRenderer playerRenderer =
                    (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);

            if (right) {
                playerRenderer.renderRightHand(poseStack, buffer, light, player);
            } else {
                playerRenderer.renderLeftHand(poseStack, buffer, light, player);
            }
        }

        // 3. Смещаем камеру относительно руки
        float x = right ? POS_X : -POS_X;

        poseStack.translate(x, POS_Y, POS_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(ROT_X));
        poseStack.mulPose(Axis.YP.rotationDegrees(ROT_Y * (right ? 1.0F : -1.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ROT_Z * (right ? 1.0F : -1.0F)));
        poseStack.scale(SCALE, SCALE, SCALE);

        // 4. Рисуем GeckoLib-модель камеры
        CAMERA_RENDERER.renderByItem(
                stack,
                right
                        ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack,
                buffer,
                light,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }

    /**
     * Копия ванильного renderPlayerArm из ItemInHandRenderer.
     * Ставит PoseStack в положение руки от первого лица.
     */
    private static void renderPlayerArm(
            PoseStack poseStack,
            float equipProgress,
            float swingProgress,
            HumanoidArm arm
    ) {
        boolean right = arm == HumanoidArm.RIGHT;
        float f = right ? 1.0F : -1.0F;

        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * ((float) Math.PI * 2F));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);

        poseStack.translate(
                f * (f2 + 0.64000005F),
                f3 + -0.6F + equipProgress * -0.6F,
                f4 + -0.71999997F
        );

        poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));

        float f5 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);

        poseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));

        poseStack.translate(f * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        poseStack.translate(f * 5.6F, 0.0F, 0.0F);
    }
}