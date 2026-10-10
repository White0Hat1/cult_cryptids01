package com.cult.cryptids.client;

import com.cult.cryptids.item.FlashlightItem;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class FlashlightHandler {
    private static final Object2IntArrayMap<Player> POS_CACHE = new Object2IntArrayMap<>();

    static {
        POS_CACHE.defaultReturnValue(Integer.MAX_VALUE);
    }

    /** Логика света фонаря. Яркость зависит от расстояния до блока (макс. 15 блоков) и направления взгляда игрока. **/
    public static int calculateBrightness(EntityGetter entities, BlockPos pos) {
        AtomicInteger brightness = new AtomicInteger();
        Vec3 center = pos.getCenter(); // Центр блока
        entities.players().forEach(player -> {
            Vec3 vec3 = center.subtract(player.getEyePosition()); // Вектор от глаз игрока до центра блока
            double d = vec3.lengthSqr();
            if (d > 1 && d <= 256 && isHoldingFlashlight(player)) { // Проверяем, что до блока 1-15 блоков, а в руке включённый фонарь
                double cos = vec3.dot(player.getLookAngle()); // Скалярное произведение векторов, равное косинусу угла между ними на их длины
                if (cos < 0) return; // Отрицательное значение означает, что блок позади нас
                cos /= Math.sqrt(d); // Делим на длину вектора, (длина второго вектора - 1), чтобы получить косинус
                cos = 2 * cos * cos - 1; // Cos 2a = 2cos^2 a - 1. Теперь свет полностью угасает при отклонении на 45 градусов
                brightness.set(Math.max(brightness.get(), (int) (cos * (16 - 0.058 * d))));
            }
        });
        return brightness.get();
    }

    @SubscribeEvent
    public static void tryClearCache(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Level lvl = event.getCamera().getEntity().level();
        for (Player player: lvl.players()) {
            int newValue = isHoldingFlashlight(player) ? (player.position().hashCode() * 31 +
                    Float.hashCode(player.getXRot())) * 31 + Float.hashCode(player.getYRot()) : Integer.MAX_VALUE;
            if (POS_CACHE.getInt(player) != newValue) {
                POS_CACHE.put(player, newValue);
                ((ClientLevel) lvl).setSectionDirtyWithNeighbors(SectionPos.blockToSectionCoord(player.getBlockX()),
                        SectionPos.blockToSectionCoord(player.getBlockY()), SectionPos.blockToSectionCoord(player.getBlockZ()));
            }
        }
    }

    /** Определяет, держит ли игрок включённый фонарь. **/
    public static boolean isHoldingFlashlight(Player player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof FlashlightItem && FlashlightItem.isOn(stack);
    }
}
