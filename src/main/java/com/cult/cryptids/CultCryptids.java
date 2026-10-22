package com.cult.cryptids;

import com.cult.cryptids.command.CultCryptidsCommand;
import com.cult.cryptids.entity.NewSirenHead;
import com.cult.cryptids.item.M14Item;
import com.cult.cryptids.network.BloodMoonNetwork;
import com.cult.cryptids.network.BloodMoonStatePacket;
import com.cult.cryptids.reg.ModBlocks;
import com.cult.cryptids.reg.ModEntities;
import com.cult.cryptids.reg.ModItems;
import com.cult.cryptids.reg.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.Objects;

@Mod(CultCryptids.MODID)
public class CultCryptids {
    public static final String MODID = "cult_cryptids";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CultCryptids() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

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
        event.put(ModEntities.SIREN_HEAD.get(), NewSirenHead.createAttributes().build());
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class ServerEvents {
        /** Отправка новому игроку данных о кровавой луне */
        @SubscribeEvent
        public static void join(PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            BloodMoonNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                    new BloodMoonStatePacket(BloodMoonSavedData.get(Objects.requireNonNull(player.getServer())).isActive()));
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            CultCryptidsCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            BloodMoonSavedData data = BloodMoonSavedData.get(event.getServer());
            if (!data.isActive()) return;
            BloodMoonSavedData.get(event.getServer()).tick();
            if (!data.isActive()) BloodMoonNetwork.broadcast(false);
        }

        /** Никакого рукопашного урона — только выстрел через пакет */
        @SubscribeEvent
        public static void onAttack(AttackEntityEvent event) {
            if (event.getEntity().getMainHandItem().getItem() instanceof M14Item) event.setCanceled(true);
        }
    }
}