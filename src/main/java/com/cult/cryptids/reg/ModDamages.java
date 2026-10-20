package com.cult.cryptids.reg;

import com.cult.cryptids.CultCryptids;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public class ModDamages {
    public static final ResourceKey<DamageType> CRYPTID = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(CultCryptids.MODID, "cryptid"));

    public static DamageSource cryptid(Entity cryptid) {
        return new DamageSource(cryptid.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(CRYPTID), cryptid);
    }
}
