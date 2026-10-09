package com.cult.cryptids.client;

import com.cult.cryptids.ModItems;
import com.cult.cryptids.item.FlashlightItem;
import com.cult.cryptids.item.HandedItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "cult_cryptids",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class FlashlightHandRenderer {

    private static final float SCALE = 1.0F;
    private static final float POS_X = -0.50F;
    private static final float POS_Y = -0.50F;
    private static final float POS_Z = -0.10F;
    private static final float ROT_X = 0.0F;
    private static final float ROT_Y = 0.0F;
    private static final float ROT_Z = 0.0F;

    private static final HandedItemRenderer<FlashlightItem> RENDERER = HandedItemRenderer.flashlight();

    private static boolean wasInHand = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            wasInHand = false;
            return;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        boolean inHand = mainHand.getItem() instanceof FlashlightItem;

        // Ушёл из руки → сброс.
        if (!inHand && wasInHand) {
            Item item = ModItems.FLASHLIGHT.get();
            if (item instanceof FlashlightItem flashlight) {
                flashlight.resetAnimation();
            }
        }

        // Пришёл в руку → сброс + draw.
        if (inHand && !wasInHand) {
            FlashlightItem item = (FlashlightItem) mainHand.getItem();
            item.markDraw(mc.level.getGameTime());
            item.resetAnimation();
        }

        wasInHand = inHand;
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null) return;

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof FlashlightItem)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        event.setCanceled(true);

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();

        pose.pushPose();
        pose.translate(POS_X, POS_Y, POS_Z);
        pose.mulPose(Axis.XP.rotationDegrees(ROT_X));
        pose.mulPose(Axis.YP.rotationDegrees(ROT_Y));
        pose.mulPose(Axis.ZP.rotationDegrees(ROT_Z));
        pose.scale(SCALE, SCALE, SCALE);

        RENDERER.renderByItem(stack, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                pose, buffer, light, OverlayTexture.NO_OVERLAY);

        pose.popPose();
    }
}