package com.cult.cryptids.item;

public interface FirstPersonAnimatedItem {
    /** Публичный сброс анимации — используется при переключении предметов в FP. */
    void resetAnimation();
    /** Запускает анимацию доставания предмета. */
    void markDraw(long gameTime);
}
