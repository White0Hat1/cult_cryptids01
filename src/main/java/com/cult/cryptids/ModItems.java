package com.cult.cryptids;

import com.cult.cryptids.item.CameraItem;
import com.cult.cryptids.item.FlashlightItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    // Реестр предметов
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "cult_cryptids");

    // Реестр креативных вкладок
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "cult_cryptids");

    // ================= ПРЕДМЕТЫ =================

    // 🥚 Спавн-яйцо Сирена
    public static final RegistryObject<ForgeSpawnEggItem> SIREN_HEAD_SPAWN_EGG =
            ITEMS.register("siren_head_spawn_egg", () -> new ForgeSpawnEggItem(
                    ModEntities.SIREN_HEAD,
                    0xFFFFFF,
                    0xFFFFFF,
                    new Item.Properties()
            ));

    // 📷 Фотоаппарат
    public static final RegistryObject<CameraItem> CAMERA =
            ITEMS.register("camera", () -> new CameraItem(
                    new Item.Properties().stacksTo(1)));

    // 🔦 Фонарик
    public static final RegistryObject<FlashlightItem> FLASHLIGHT =
            ITEMS.register("flashlight", () -> new FlashlightItem(
                    new Item.Properties().stacksTo(1)));

    // ================= КРЕАТИВНАЯ ВКЛАДКА =================

    public static final RegistryObject<CreativeModeTab> CULT_CRYPTIDS_TAB =
            CREATIVE_TABS.register("cult_cryptids_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cult_cryptids"))
                    .icon(() -> new ItemStack(SIREN_HEAD_SPAWN_EGG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(SIREN_HEAD_SPAWN_EGG.get());
                        output.accept(ModBlocks.TOY_HATTER_ITEM.get());
                        output.accept(CAMERA.get());
                        output.accept(FLASHLIGHT.get());
                    })
                    .build());
}