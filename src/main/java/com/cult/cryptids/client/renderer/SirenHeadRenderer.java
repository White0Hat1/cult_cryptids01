package com.cult.cryptids.client.renderer;

import com.cult.cryptids.client.model.SirenHeadModel;
import com.cult.cryptids.entity.NewSirenHead;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SirenHeadRenderer extends GeoEntityRenderer<NewSirenHead> {
    public SirenHeadRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SirenHeadModel()); // 👈 используем нашу модель
        this.withScale(2);
    }
}