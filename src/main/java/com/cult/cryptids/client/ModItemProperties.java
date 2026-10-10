package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.ModItems;
import com.cult.cryptids.item.FlashlightItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModItemProperties {
    @SubscribeEvent
    public static void reg(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.FLASHLIGHT.get(), new ResourceLocation(CultCryptids.MODID, "on"),
                (stack, lvl, entity, i) -> FlashlightItem.isOn(stack) ? 1 : 0));
    }
}
