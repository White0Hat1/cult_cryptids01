package com.cult.cryptids.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import java.util.HashMap;
import java.util.Map;

public class SirenHeadModel extends DefaultedEntityGeoModel<SirenHeadEntity> {
    private final Map<String, Float> baseRotY = new HashMap<>();

    private static final int SPIN_PERIOD_TICKS = 400;
    private static final int SPIN_DURATION_TICKS = 40;

    public SirenHeadModel() {
        super(new ResourceLocation("cult_cryptids", "siren_head"), false);
    }

    @Override
    public void setCustomAnimations(SirenHeadEntity animatable, long instanceId,
                                    AnimationState<SirenHeadEntity> animationState) {
        restoreBase("head");
        restoreBase("torso");
        restoreBase("body");
        restoreBase("rightSiren");
        restoreBase("leftSiren");

        super.setCustomAnimations(animatable, instanceId, animationState);

        saveBase("head");
        saveBase("torso");
        saveBase("body");
        saveBase("rightSiren");
        saveBase("leftSiren");

        // 🛑 Во время grab/slam/reach/crawl/wall_climb — не крутим сиренами
        // и не поворачиваем тело. Играет только анимация из JSON.
        if (animatable.isGrabbing()
                || animatable.isSlamming()
                || animatable.isReaching()
                || animatable.isCrawling()
                || animatable.isWallClimbing()) {
            return;
        }

        // 🎡 Кручение сиренов — раз в 20 сек
        long cycleTick = animatable.tickCount % SPIN_PERIOD_TICKS;
        if (cycleTick < SPIN_DURATION_TICKS) {
            float progress = (float) cycleTick / (float) SPIN_DURATION_TICKS;
            float spinDeg = progress * 360.0F;
            float spinRad = (float) Math.toRadians(spinDeg);

            CoreGeoBone rightSiren = this.getAnimationProcessor().getBone("rightSiren");
            if (rightSiren != null) {
                rightSiren.setRotY(rightSiren.getRotY() + spinRad);
            }

            CoreGeoBone leftSiren = this.getAnimationProcessor().getBone("leftSiren");
            if (leftSiren != null) {
                leftSiren.setRotY(leftSiren.getRotY() + spinRad);
            }
        }

        // === Поворот головы/торса/тела за игроком ===
        float deltaYaw = Mth.wrapDegrees(animatable.getYHeadRot() - animatable.yBodyRot);

        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            float yaw = Mth.clamp(deltaYaw, -80F, 80F);
            head.setRotY(head.getRotY() + (float) Math.toRadians(yaw));
        }

        float partial = Mth.clamp(deltaYaw * 0.4F, -30F, 30F);
        float partialRad = (float) Math.toRadians(partial);

        CoreGeoBone torso = this.getAnimationProcessor().getBone("torso");
        if (torso != null) {
            torso.setRotY(torso.getRotY() + partialRad);
        }

        CoreGeoBone body = this.getAnimationProcessor().getBone("body");
        if (body != null) {
            body.setRotY(body.getRotY() + partialRad);
        }
    }

    private void saveBase(String name) {
        CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
        if (bone != null) {
            baseRotY.put(name, bone.getRotY());
        }
    }

    private void restoreBase(String name) {
        CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
        if (bone != null && baseRotY.containsKey(name)) {
            bone.setRotY(baseRotY.get(name));
        }
    }
}