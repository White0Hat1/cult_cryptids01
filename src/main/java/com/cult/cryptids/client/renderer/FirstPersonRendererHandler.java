package com.cult.cryptids.client.renderer;

import com.cult.cryptids.item.CameraItem;
import com.cult.cryptids.item.FirstPersonAnimatedItem;
import com.cult.cryptids.item.FlashlightItem;
import com.cult.cryptids.item.M14Item;
import com.cult.cryptids.reg.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class FirstPersonRendererHandler {
    private static final HandedItemRenderer<CameraItem> CAMERA = HandedItemRenderer.camera();
    private static final HandedItemRenderer<FlashlightItem> FLASHLIGHT = HandedItemRenderer.flashlight();
    private static final HandedItemRenderer<M14Item> M14 = HandedItemRenderer.m14();

    private static FirstPersonAnimatedItem wasInHand;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            wasInHand = null;
            return;
        }

        FirstPersonAnimatedItem item = mc.player.getMainHandItem().getItem() instanceof
                FirstPersonAnimatedItem firstPersonAnimatedItem ? firstPersonAnimatedItem : null;
        if (!Objects.equals(item, wasInHand)) {
            if (wasInHand != null) wasInHand.resetAnimation();
            if (item != null) {
                item.markDraw(mc.level.getGameTime());
                item.resetAnimation();
            }
            wasInHand = item;
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null) return;

        ItemStack stack = event.getItemStack();
        HandedItemRenderer<?> renderer;
        if (stack.is(ModItems.CAMERA.get())) renderer = CAMERA;
        else if (stack.is(ModItems.FLASHLIGHT.get())) renderer = FLASHLIGHT;
        else if (stack.is(ModItems.M14.get())) renderer = M14;
        else return;
        event.setCanceled(true);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-0.5f, -0.5f, -0.1f);
        renderer.renderByItem(stack, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, pose, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}