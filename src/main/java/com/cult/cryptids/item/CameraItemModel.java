package com.cult.cryptids.item;

import com.cult.cryptids.CultCryptids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CameraItemModel extends GeoModel<CameraItem> {

    private static final ResourceLocation FALLBACK =
            new ResourceLocation("minecraft", "textures/entity/steve.png");

    @Override
    public ResourceLocation getModelResource(CameraItem animatable) {
        return new ResourceLocation(CultCryptids.MODID, "geo/item/camera.geo.json");
    }

    /**
     * 🎨 Основная модель использует ТЕКСТУРУ СКИНА игрока.
     * Все кости рук (arm_right, arm_left) рисуются как настоящие руки.
     */
    @Override
    public ResourceLocation getTextureResource(CameraItem animatable) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player != null) {
            return player.getSkinTextureLocation();
        }
        return FALLBACK;
    }

    @Override
    public ResourceLocation getAnimationResource(CameraItem animatable) {
        return new ResourceLocation(CultCryptids.MODID, "animations/item/camera.animation.json");
    }
}