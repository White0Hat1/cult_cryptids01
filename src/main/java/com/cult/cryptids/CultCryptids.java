package com.cult.cryptids;

import com.cult.cryptids.command.CultCryptidsCommand;
import com.cult.cryptids.entity.SirenHeadEntity;
import com.cult.cryptids.event.BloodMoonEvent;
import com.cult.cryptids.network.BloodMoonNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CultCryptids.MODID)
public class CultCryptids {
    public static final String MODID = "cult_cryptids";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CultCryptids(FMLJavaModLoadingContext ctx) {
        IEventBus modEventBus = ctx.getModEventBus();

        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);

        modEventBus.addListener(this::registerAttributes);

        BloodMoonNetwork.register();
        MinecraftForge.EVENT_BUS.register(ServerEvents.class);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SIREN_HEAD.get(), SirenHeadEntity.createAttributes().build());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
                BloodMoonEvent.tick(event.getServer());
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            CultCryptidsCommand.register(event.getDispatcher());
        }
    }
}