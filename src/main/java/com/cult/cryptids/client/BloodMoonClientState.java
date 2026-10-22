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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
    private static int sequenceTick = 0;
    private static boolean cinematicActive = false;
    private static boolean impactPlayed = false;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        // 🩸 Переход false → true = Blood Moon только что началась
        if (value && !active) say(Component.translatable("message.cult_cryptids.blood_moon_start"));
        // 🔇 Переход true → false = Blood Moon закончилась, глушим музыку
        if (!value && active) Minecraft.getInstance().getMusicManager().stopPlaying(getMusic());
        active = value;
    }

    public static Music getMusic() {
        if (MUSIC == null) MUSIC = new Music(ModSounds.BLOOD_MOON_MUSIC.getHolder().orElseThrow(), 0, 0, true);
        return MUSIC;
    }

    public static void say(String text) {
        fullText = text;
        fearText = Component.translatable("message.cult_cryptids.fear_warning").getString();
        sequenceTick = 0;
        cinematicActive = true;
        impactPlayed = false;
        Minecraft.getInstance().getMusicManager().stopPlaying();
    }

    public static void say(Component text) {
        say(text.getString());
    }

    public static float getSkyProgress() {
        return cinematicActive ? Mth.clamp ((sequenceTick - SKY_FADE_START) / (float) SKY_FADE_TICKS, 0, 1) : 1;
    }

    /**
     * 🌕 Прогресс увеличения луны (0.0 = обычный размер, 1.0 = полный Blood Moon).
     * Стартует в момент появления надписи "BLOOD MOON", растёт 5 секунд.
     */
    public static float getMoonScaleProgress() {
        if (!isActive()) return 0;
        // Кинематик уже закончился, но Blood Moon ещё идёт => полный размер.
        if (!cinematicActive || fullText.isEmpty()) return 1;
        float f = Mth.clamp((float) (sequenceTick - MOON_SCALE_START) / MOON_SCALE_DURATION, 0, 1);
        // smoothstep для плавности
        return f * f * (3 - 2 * f);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (fullText.isEmpty()) return;

        sequenceTick++;

        Minecraft mc = Minecraft.getInstance();

        if (!impactPlayed && sequenceTick >= IMPACT_TRIGGER_TICK && mc.player != null && mc.level != null) {
            mc.getSoundManager().play(new SimpleSoundInstance(new ResourceLocation(CultCryptids.MODID, "blood_moon_impact"),
                    SoundSource.MASTER, 2, 1, SoundInstance.createUnseededRandom(), false,
                    0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
            impactPlayed = true;
        }

        if (sequenceTick >= MUSIC_TRIGGER_TICK) {
            MusicManager manager = mc.getMusicManager();
            if (!manager.isPlayingMusic(getMusic())) manager.startPlaying(getMusic());
        }

        if (sequenceTick >= FADE_OUT_END && cinematicActive) {
            fullText = "";
            fearText = "";
            sequenceTick = 0;
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
        float fearAlpha = sequenceTick < FEAR_HOLD_END ? 1 : sequenceTick < FEAR_FADE_END ?
                1 - (float) (sequenceTick - FEAR_HOLD_END) / (FEAR_FADE_END - FEAR_HOLD_END) : 0;
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
        float warnAlpha = 0;
        if (sequenceTick >= WARN_PRINT_START && sequenceTick < WARN_HOLD_END) warnAlpha = 1;
        else if (sequenceTick >= WARN_HOLD_END && sequenceTick < WARN_FADE_END) warnAlpha = 1 - (float) (sequenceTick - WARN_HOLD_END) / (WARN_FADE_END - WARN_HOLD_END);
        if (warnAlpha > 0) {
            float revealProgress = Mth.clamp((float) (sequenceTick - WARN_PRINT_START) / WARN_PRINT_TICKS, 0, 1);
            int shown = Mth.clamp((int) (revealProgress * fullText.length()), 0, fullText.length());
            int textColor = ((int) (warnAlpha * 255) << 24) | 0xFF5555;
            int shadowColor = (int) (warnAlpha * 204) << 24;

            String visible = fullText.substring(0, shown);
            int textX = (screenW - mc.font.width(visible)) / 2;
            gfx.drawString(mc.font, visible, textX + 1, TOP_TEXT_Y + 1, shadowColor, false);
            gfx.drawString(mc.font, visible, textX, TOP_TEXT_Y, textColor, false);
        }

        float masterAlpha = sequenceTick >= FADE_OUT_START ? Mth.clamp(1 - (float)
                (sequenceTick - FADE_OUT_START) / (FADE_OUT_END - FADE_OUT_START), 0, 1) : 1;

        // MOON (иконка на экране)
        float moonP = 0;
        if (sequenceTick >= MOON_START && sequenceTick < MOON_END) {
            float t = (float) (sequenceTick - MOON_START) / (MOON_END - MOON_START);
            moonP = t * t * (3 - 2 * t);
        } else if (sequenceTick >= MOON_END) moonP = 1;
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
        float titlesP = 0;
        if (sequenceTick >= TITLES_START && sequenceTick < TITLES_END) titlesP = (float) (sequenceTick - TITLES_START) / (TITLES_END - TITLES_START);
        else if (sequenceTick >= TITLES_END) titlesP = 1;

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
        float dividerP = 0;
        if (sequenceTick >= DIVIDER_START && sequenceTick < DIVIDER_END) dividerP = (float) (sequenceTick - DIVIDER_START) / (DIVIDER_END - DIVIDER_START);
        else if (sequenceTick >= DIVIDER_END) dividerP = 1;

        if (dividerP > 0) {
            int halfWidth = (int) (100 * dividerP);
            int centerX = screenW / 2;
            int lineAlpha = (int) (229.5f * masterAlpha);
            int lineColor = (lineAlpha << 24) | 0xFFFFFF;
            gfx.fill(centerX - halfWidth, dividerY, centerX + halfWidth, dividerY + 1, lineColor);
        }
    }
}