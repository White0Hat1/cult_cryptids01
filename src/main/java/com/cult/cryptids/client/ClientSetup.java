package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.client.renderer.SirenHeadRenderer;
import com.cult.cryptids.item.FlashlightItem;
import com.cult.cryptids.reg.ModEntities;
import com.cult.cryptids.reg.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CultCryptids.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    @SubscribeEvent
    public static void itemProps(FMLClientSetupEvent event) {
        // Регистрация свойств предметов для моделей
        event.enqueueWork(() -> ItemProperties.register(ModItems.FLASHLIGHT.get(), new ResourceLocation(CultCryptids.MODID, "on"),
                (stack, lvl, entity, i) -> FlashlightItem.isOn(stack) ? 1 : 0));
    }

    /** Регистрация рендера сущностей. */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SIREN_HEAD.get(), SirenHeadRenderer::new);
    }
}