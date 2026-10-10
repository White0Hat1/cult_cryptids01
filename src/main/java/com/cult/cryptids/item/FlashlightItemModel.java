package com.cult.cryptids.item;

import com.cult.cryptids.CultCryptids;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FlashlightItemModel extends GeoModel<FlashlightItem> {

    @Override
    public ResourceLocation getModelResource(FlashlightItem animatable) {
        return new ResourceLocation(CultCryptids.MODID, "geo/item/flashlight.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FlashlightItem animatable) {
        return new ResourceLocation(CultCryptids.MODID, "textures/item/flashlight_tex.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FlashlightItem animatable) {
        return new ResourceLocation(CultCryptids.MODID, "animations/item/flashlight.animation.json");
    }
}