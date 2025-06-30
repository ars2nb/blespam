package com.tutozz.blespam;

import org.jetbrains.annotations.NotNull;

public interface Spammer {
    @NotNull

    boolean isSpamming();
    void start();
    void stop();
    void setBlinkRunnable(Runnable blinkRunnable);
    Runnable getBlinkRunnable();
}
