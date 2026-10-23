package com.cult.cryptids.client;

import com.cult.cryptids.CultCryptids;
import com.cult.cryptids.reg.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

/** Флаг события на клиенте. Управляется пакетом с сервера. */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BloodMoonClientState {
    // ============ ТАЙМИНГИ ============
    private static final int FEAR_HOLD_END = 60;
    private static final int FEAR_FADE_END = 70;

    private static final int WARN_PRINT_START = FEAR_FADE_END;
    private static final int WARN_PRINT_TICKS = 90;
    private static final int WARN_PRINT_END = WARN_PRINT_START + WARN_PRINT_TICKS;
    private static final int WARN_HOLD_END = WARN_PRINT_END + 80;
    private static final int WARN_FADE_END = WARN_HOLD_END + 20;

    private static final int MOON_START = WARN_FADE_END + 10;
    private static final int MOON_END = MOON_START + 20;

    private static final int TITLES_START = MOON_END;
    private static final int TITLES_END = TITLES_START + 15;

    private static final int DIVIDER_START = TITLES_END;
    private static final int DIVIDER_END = DIVIDER_START + 15;

    private static final int FADE_OUT_START = DIVIDER_END + 120;
    private static final int FADE_OUT_END = FADE_OUT_START + 40;

    // ============ 🌕 УВЕЛИЧЕНИЕ ЛУНЫ ============
    /** 🎯 Начинаем увеличение ровно в момент появления надписи "BLOOD MOON". */
    private static final int MOON_SCALE_START = TITLES_START;
    /** ⏱️ Длительность увеличения — 5 секунд (100 тиков). */
    private static final int MOON_SCALE_DURATION = 100;
    // ============ 🎵 ЗВУКИ ============
    private static final int IMPACT_LENGTH_TICKS = 60;
    private static final int IMPACT_TRIGGER_TICK = MOON_START;
    private static final int MUSIC_TRIGGER_TICK = IMPACT_TRIGGER_TICK + IMPACT_LENGTH_TICKS;
    // ============ НЕБО ============
    private static final int SKY_FADE_START = TITLES_START;
    private static final int SKY_FADE_TICKS = 100;
    // ============ РАЗМЕРЫ ============
    private static final int MOON_SIZE = 64;
    private static final int MOON_Y = 40;
    private static final int TOP_TEXT_Y = 40;

    private static Music MUSIC;
    private static final ResourceLocation MOON_TEX = new ResourceLocation(CultCryptids.MODID, "textures/gui/blood_moon_icon.png");

    private static boolean active = false;
    private static String fullText = "";
    private static String fearText = "";
    private static boolean cinematicActive = false;
    private static boolean impactPlayed = false;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value, boolean forceSet) {
        if (value) say(Component.translatable("message.cult_cryptids.blood_moon_start"), forceSet);
        else {
            active = false;
            Minecraft.getInstance().getMusicManager().stopPlaying(getMusic());
        }
    }

    public static Music getMusic() {
        if (MUSIC == null) MUSIC = new Music(ModSounds.BLOOD_MOON_MUSIC.getHolder().orElseThrow(), 0, 0, true);
        return MUSIC;
    }

    private static int getSequenceTick() {
        Level lvl = Minecraft.getInstance().level;
        return lvl != null ? (int) (Objects.requireNonNull(Minecraft.getInstance().level).getDayTime() - 13000) : 0;
    }

    public static void say(String text, boolean forceSet) {
        fullText = text;
        fearText = Component.translatable("message.cult_cryptids.fear_warning").getString();
        active = forceSet;
        cinematicActive = true;
        impactPlayed = false;
        Minecraft.getInstance().getMusicManager().stopPlaying();
    }

    public static void say(Component text, boolean forceSet) {
        say(text.getString(), forceSet);
    }

    public static float getSkyProgress() {
        return active ? nightProgress(SKY_FADE_START, SKY_FADE_TICKS) : 0;
    }

    /**
     * 🌕 Прогресс увеличения луны (0.0 = обычный размер, 1.0 = полный Blood Moon).
     * Стартует в момент появления надписи "BLOOD MOON", растёт 5 секунд.
     */
    public static float getMoonScaleProgress() {
        if (!isActive()) return 0;
        float f = nightProgress(MOON_SCALE_START, MOON_SCALE_DURATION);
        // smoothstep для плавности
        return f * f * (3 - 2 * f);
    }

    private static float nightProgress(int start, int duration) {
        return cinematicActive ? Mth.clamp((float) (getSequenceTick() - start) / duration, 0, 1) :
                Math.min((10000f - getSequenceTick()) / duration, 1);

    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!active) {
            if (cinematicActive) {
                int c = getSequenceTick();
                if (c >= 0 && c < 480) active = true;
                else return;
            } else return;
        }
        if (fullText.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int c = getSequenceTick();
        if (!impactPlayed && c >= IMPACT_TRIGGER_TICK && mc.player != null && mc.level != null) {
            mc.getSoundManager().play(new SimpleSoundInstance(new ResourceLocation(CultCryptids.MODID, "blood_moon_impact"),
                    SoundSource.MASTER, 2, 1, SoundInstance.createUnseededRandom(), false,
                    0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
            impactPlayed = true;
        }

        if (c >= MUSIC_TRIGGER_TICK) {
            MusicManager manager = mc.getMusicManager();
            if (!manager.isPlayingMusic(getMusic())) manager.startPlaying(getMusic());
        }

        if (c >= FADE_OUT_END && cinematicActive) {
            fullText = "";
            fearText = "";
            cinematicActive = false;
            impactPlayed = false;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (fullText.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int screenW = gfx.guiWidth();
        int c = getSequenceTick();
        float masterAlpha = 1 - progress(c, FADE_OUT_START, FADE_OUT_END);

        float fearAlpha = 1 - progress(c, FEAR_HOLD_END, FEAR_FADE_END);
        if (fearAlpha > 0 && !fearText.isEmpty()) {
            int fa = (int) (fearAlpha * 255);
            int fearColor = (fa << 24) | 0xFFFFFF;
            int markColor = (fa << 24) | 0xFF2222;
            int shadowColor = (int) (fearAlpha * 200) << 24;

            String mark = "!";
            int markX = (screenW - mc.font.width(mark)) / 2;
            int markY = TOP_TEXT_Y - 18;
            gfx.drawString(mc.font, mark, markX + 1, markY + 1, shadowColor, false);
            gfx.drawString(mc.font, mark, markX, markY, markColor, false);

            int fearX = (screenW - mc.font.width(fearText)) / 2;
            gfx.drawString(mc.font, fearText, fearX + 1, TOP_TEXT_Y + 1, shadowColor, false);
            gfx.drawString(mc.font, fearText, fearX, TOP_TEXT_Y, fearColor, false);
        }

        // WARNING
        float warnAlpha = c >= WARN_PRINT_START ? 1 - progress(c, WARN_HOLD_END, WARN_FADE_END) : 0;
        if (warnAlpha > 0) {
            float revealProgress = Mth.clamp((float) (c - WARN_PRINT_START) / WARN_PRINT_TICKS, 0, 1);
            int shown = Mth.clamp((int) (revealProgress * fullText.length()), 0, fullText.length());
            int textColor = ((int) (warnAlpha * 255) << 24) | 0xFF5555;
            int shadowColor = (int) (warnAlpha * 204) << 24;

            String visible = fullText.substring(0, shown);
            int textX = (screenW - mc.font.width(visible)) / 2;
            gfx.drawString(mc.font, visible, textX + 1, TOP_TEXT_Y + 1, shadowColor, false);
            gfx.drawString(mc.font, visible, textX, TOP_TEXT_Y, textColor, false);
        }

        // MOON (иконка на экране)
        float moonP = progress(c, MOON_START, MOON_END);
        moonP = moonP * moonP * (3 - 2 * moonP);
        if (moonP > 0) {
            float moonScale = 0.6f + 0.4f * moonP;
            RenderSystem.enableBlend();
            gfx.setColor(1, 1, 1, masterAlpha * moonP);
            PoseStack pose = gfx.pose();
            pose.pushPose();
            pose.translate(screenW * 0.5f, MOON_Y, 0);
            pose.scale(moonScale, moonScale, 1);
            gfx.blit(MOON_TEX, -MOON_SIZE / 2, 0, 0, 0, MOON_SIZE, MOON_SIZE, MOON_SIZE, MOON_SIZE);
            pose.popPose();
            gfx.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }

        // TITLES
        int titleY = MOON_Y + MOON_SIZE + 10;
        int dividerY = titleY + 26;
        int subtitleY = dividerY + 10;
        float titlesP = progress(c, TITLES_START, TITLES_END);
        if (titlesP > 0) {
            String title = "BLOOD MOON";
            int titleAlpha = (int) (255 * masterAlpha * titlesP);
            int titleColor = (titleAlpha << 24) | 0xFFFFFF;
            int titleShadow = (int) (titleAlpha * 0.9f) << 24;

            PoseStack pose = gfx.pose();
            pose.pushPose();
            pose.translate(screenW * 0.5f, titleY, 0);
            pose.scale(2, 2, 2);
            int titleW = mc.font.width(title);
            mc.font.drawInBatch(title, titleW * -0.5f, 0, titleColor, true, pose.last().pose(),
                    gfx.bufferSource(), Font.DisplayMode.NORMAL, titleShadow, 15728880, mc.font.isBidirectional());
            pose.popPose();

            String sub = "Special Event";
            int subAlpha = (int) (255 * masterAlpha * titlesP);
            int subColor = (subAlpha << 24) | 0xFFFFFF;
            int subShadow = (int) (subAlpha * 0.8f) << 24;
            int subW = mc.font.width(sub);
            mc.font.drawInBatch(sub, (screenW - subW) * 0.5f, subtitleY + 1, subColor, true,
                    pose.last().pose(), gfx.bufferSource(), Font.DisplayMode.NORMAL, subShadow, 15728880, mc.font.isBidirectional());
        }

        // DIVIDER
        float dividerP = progress(c, DIVIDER_START, DIVIDER_END);
        if (dividerP > 0) {
            int halfWidth = (int) (100 * dividerP);
            int centerX = screenW / 2;
            int lineAlpha = (int) (229.5f * masterAlpha);
            int lineColor = (lineAlpha << 24) | 0xFFFFFF;
            gfx.fill(centerX - halfWidth, dividerY, centerX + halfWidth, dividerY + 1, lineColor);
        }
    }

    private static float progress(int ticks, int startTicks, int endTicks) {
        if (ticks <= startTicks) return 0;
        if (ticks >= endTicks) return 1;
        return (float) (ticks - startTicks) / (endTicks - startTicks);
    }
}