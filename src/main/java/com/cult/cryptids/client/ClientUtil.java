package com.cult.cryptids.client;

import com.cult.cryptids.client.sound.SirenChaseSoundInstance;
import com.cult.cryptids.entity.NewSirenHead;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class ClientUtil {
    private static final double FIND_ENTITY_MAX_DIST = 64;

    public static void sirenChase(NewSirenHead entity) {
        Minecraft.getInstance().getSoundManager().play(new SirenChaseSoundInstance(entity));
    }

    /** Делает «снимок» — ищет сущность в перекрестии и показывает сообщение. */
    public static void doPhoto(Player player) {
        String entityName = findEntityInView(player);

        if (entityName != null) {
            player.displayClientMessage(
                    Component.literal("§a📸 На фото: §e" + entityName),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("§7📸 Пустой кадр"),
                    true
            );
        }
    }

    /** Raycast по перекрестию: ищем ближайшую живую сущность. */
    private static String findEntityInView(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(FIND_ENTITY_MAX_DIST));

        Entity closest = null;
        double closestDist = FIND_ENTITY_MAX_DIST;

        List<Entity> entities = player.level().getEntities(
                player,
                player.getBoundingBox().inflate(FIND_ENTITY_MAX_DIST)
        );

        for (Entity e : entities) {
            if (e == player) continue;
            if (e.isSpectator()) continue;
            if (!(e instanceof LivingEntity)) continue;

            AABB box = e.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = box.clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceTo(hit.get());
                if (d < closestDist) {
                    closestDist = d;
                    closest = e;
                }
            }
        }

        if (closest == null) return null;
        return closest.getType().getDescription().getString();
    }
}