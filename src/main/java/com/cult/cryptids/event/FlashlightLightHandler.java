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
 * 🔦 Ставит/убирает невидимый блок света (Blocks.LIGHT) перед игроком,
 * пока фонарик включён. Работает только на сервере.
 */
@Mod.EventBusSubscriber(modid = CultCryptids.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FlashlightLightHandler {

    /** Дистанция луча (в блоках). */
    private static final double REACH = 6.0D;

    /** Уровень света (0–15). 15 = как факел. */
    private static final int LIGHT_LEVEL = 15;

    /** Позиции, где мы в прошлый раз поставили свет (по UUID игрока). */
    private static final Map<UUID, BlockPos> PLACED = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();

        // Только сервер — свет должен быть в мировых данных
        if (level.isClientSide) return;

        UUID id = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();

        // Фонарик в руке и включён?
        boolean shouldShine =
                mainHand.getItem() instanceof FlashlightItem && FlashlightItem.isOn(mainHand);

        if (!shouldShine) {
            removeLight(level, id);
            return;
        }

        // Куда смотрит игрок?
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(REACH));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        BlockPos targetPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            // Воздух прямо перед стеной
            targetPos = hit.getBlockPos().relative(hit.getDirection());
        } else {
            // Ничего не задели — ставим в конечной точке луча
            targetPos = BlockPos.containing(end);
        }

        // Ничего не поменялось — не трогаем мир
        BlockPos old = PLACED.get(id);
        if (old != null && old.equals(targetPos)) return;

        // Убираем старый свет
        if (old != null) {
            BlockState oldState = level.getBlockState(old);
            if (oldState.is(Blocks.LIGHT)) {
                level.setBlockAndUpdate(old, Blocks.AIR.defaultBlockState());
            }
        }

        // Проверяем, можно ли поставить свет в новое место
        BlockState existing = level.getBlockState(targetPos);
        if (!existing.isAir() && !existing.canBeReplaced()) {
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