package com.cult.cryptids.item;

import com.cult.cryptids.CultCryptids;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class M14ItemModel extends GeoModel<M14Item> {

    @Override
    public ResourceLocation getModelResource(M14Item animatable) {
        return new ResourceLocation(CultCryptids.MODID, "geo/item/m14.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M14Item animatable) {
        return new ResourceLocation(CultCryptids.MODID, "textures/item/m14_tex.png");
    }

    @Override
    public ResourceLocation getAnimationResource(M14Item animatable) {
        return new ResourceLocation(CultCryptids.MODID, "animations/item/m14.animation.json");
    }
}