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
 * ⚠️ Свет ставится ТОЛЬКО в чистый воздух:
 *   - не поверх травы / цветов / водорослей (иначе они исчезают)
 *   - не в воду / лаву (иначе ломаются водные потоки)
 *   - не в блоки с коллизией (стены, брёвна и т.д.)
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FlashlightLightHandler {

    private static final double REACH = 6.0D;
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

        // 🎯 Жидкости = препятствие. Свет не полетит сквозь воду/лаву.
        BlockHitResult hit = level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY,
                player
        ));

        // Позиция для света — воздух ПЕРЕД блоком, в который попали.
        BlockPos targetPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            // Если попали в жидкость — свет не ставим вообще.
            BlockState hitState = level.getBlockState(hit.getBlockPos());
            if (!hitState.getFluidState().isEmpty()) {
                removeLight(level, id);
                return;
            }
            targetPos = hit.getBlockPos().relative(hit.getDirection());
        } else {
            targetPos = BlockPos.containing(end);
        }

        // Ничего не поменялось — не трогаем мир.
        BlockPos old = PLACED.get(id);
        if (old != null && old.equals(targetPos)) return;

        // Убираем старый свет перед тем, как ставить новый.
        if (old != null) {
            BlockState oldState = level.getBlockState(old);
            if (oldState.is(Blocks.LIGHT)) {
                level.setBlockAndUpdate(old, Blocks.AIR.defaultBlockState());
            }
        }

        // 🚫 КЛЮЧЕВАЯ ПРОВЕРКА: ставим только в чистый воздух.
        BlockState targetState = level.getBlockState(targetPos);
        if (!targetState.isAir()) {
            // Там трава / вода / снег / цветок / стена / что угодно ещё —
            // просто не ставим свет, чтобы ничего не сломать.
            PLACED.remove(id);
            return;
        }

        // Ставим свет.
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