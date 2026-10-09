package com.cult.cryptids.event;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.item.FlashlightItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 🔦 Ставит/убирает невидимый блок света перед игроком, пока фонарик включён.
 *
 * Ставит свет ТОЛЬКО в чистый воздух — не ломает траву, воду, растения.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FlashlightLightHandler {

    /** 📏 Дальность луча — 10 блоков. */
    private static final double REACH = 10.0D;

    private static final int LIGHT_LEVEL = 15;

    private static final Map<UUID, BlockPos> PLACED = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide) return;

        UUID id = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();

        boolean shouldShine =
                mainHand.getItem() instanceof FlashlightItem && FlashlightItem.isOn(mainHand);

        if (!shouldShine) {
            removeLight(level, id);
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(REACH));

        // Жидкости = препятствие. Свет не полетит сквозь воду.
        BlockHitResult hit = level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY,
                player
        ));

        BlockPos targetPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockState hitState = level.getBlockState(hit.getBlockPos());
            if (!hitState.getFluidState().isEmpty()) {
                removeLight(level, id);
                return;
            }
            targetPos = hit.getBlockPos().relative(hit.getDirection());
        } else {
            targetPos = BlockPos.containing(end);
        }

        BlockPos old = PLACED.get(id);
        if (old != null && old.equals(targetPos)) return;

        if (old != null) {
            BlockState oldState = level.getBlockState(old);
            if (oldState.is(Blocks.LIGHT)) {
                level.setBlockAndUpdate(old, Blocks.AIR.defaultBlockState());
            }
        }

        // Ставим только в чистый воздух.
        BlockState targetState = level.getBlockState(targetPos);
        if (!targetState.isAir()) {
            PLACED.remove(id);
            return;
        }

        BlockState lightState = Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, LIGHT_LEVEL);
        level.setBlockAndUpdate(targetPos, lightState);
        PLACED.put(id, targetPos);
    }

    private static void removeLight(Level level, UUID id) {
        BlockPos old = PLACED.remove(id);
        if (old == null) return;
        BlockState oldState = level.getBlockState(old);
        if (oldState.is(Blocks.LIGHT)) {
            level.setBlockAndUpdate(old, Blocks.AIR.defaultBlockState());
        }
    }
}