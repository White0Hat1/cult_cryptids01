package com.cult.cryptids.mixin;

import com.cult.cryptids.entity.SirenHeadEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 🌲 Убирает коллизию листвы для Siren Head.
 * Игрок и другие мобы — по-прежнему упираются в листву.
 */
@Mixin(Block.class)
public class LeavesNoCollisionMixin {

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void cult$ignoreLeavesForSiren(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context,
                                           CallbackInfoReturnable<VoxelShape> cir) {
        // Быстрая проверка — это листва?
        if (!state.is(BlockTags.LEAVES)) return;

        // Контекст от сущности?
        if (!(context instanceof EntityCollisionContext entityCtx)) return;

        // 🆕 В 1.20.1 getEntity() возвращает Entity (может быть null), не Optional
        Entity entity = entityCtx.getEntity();
        if (entity == null) return;

        // Только для нашего Сирена
        if (entity instanceof SirenHeadEntity) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}