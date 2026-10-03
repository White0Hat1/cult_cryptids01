package com.cult.cryptids.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class CameraHelper {

    private static final double MAX_DIST = 50.0;

    /** Делает «снимок» — ищет сущность в перекрестии и показывает сообщение. */
    public static void doPhoto(Player player) {
        String entityName = findEntityInView(player, MAX_DIST);

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
    private static String findEntityInView(Player player, double maxDist) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDist));

        Entity closest = null;
        double closestDist = maxDist;

        List<Entity> entities = player.level().getEntities(
                player,
                player.getBoundingBox().inflate(maxDist)
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