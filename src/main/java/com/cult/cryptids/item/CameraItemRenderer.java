package com.cult.cryptids.item;

import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CameraItemRenderer extends GeoItemRenderer<CameraItem> {
    public CameraItemRenderer() {
        super(new CameraItemModel());
    }
}