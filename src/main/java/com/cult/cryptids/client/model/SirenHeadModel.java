package com.cult.cryptids.client.model;

import com.cult.cryptids.entity.NewSirenHead;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SirenHeadModel extends DefaultedEntityGeoModel<NewSirenHead> {
    public SirenHeadModel() {
        super(new ResourceLocation("cult_cryptids", "siren_head"), false);
    }
}