package com.cult.cryptids;

import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "cult_cryptids");

    public static final RegistryObject<EntityType<SirenHeadEntity>> SIREN_HEAD =
            ENTITY_TYPES.register("siren_head", () -> EntityType.Builder
                    .of(SirenHeadEntity::new, MobCategory.MONSTER)
                    .sized(3.0f, 12.0f)         // 👈 хитбокс 3 блока ширина × 12 блоков высота
                    .clientTrackingRange(64)
                    .build("siren_head"));
}