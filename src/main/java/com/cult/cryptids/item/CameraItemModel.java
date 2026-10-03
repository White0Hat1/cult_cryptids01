package com.cult.cryptids.item;

import com.cult.cryptids.CultCryptids;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CameraItemModel extends GeoModel<CameraItem> {

    @Override
    public ResourceLocation getModelResource(CameraItem animatable) {
        return new ResourceLocation(CultCryptids.MODID, "geo/item/camera.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CameraItem animatable) {
        return new ResourceLocation(CultCryptids.MODID, "textures/item/camera_tex.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CameraItem animatable) {
        return new ResourceLocation(CultCryptids.MODID, "animations/item/camera.animation.json");
    }
}