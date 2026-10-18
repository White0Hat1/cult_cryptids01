package com.cult.cryptids.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SirenHeadModel extends DefaultedEntityGeoModel<NewSirenHead> {
    public SirenHeadModel() {
        super(new ResourceLocation("cult_cryptids", "siren_head"), false);
    }
}