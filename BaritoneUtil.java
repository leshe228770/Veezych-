package ru.meow.util;

import baritone.api.BaritoneAPI;

/** Baritone bridge ({@code sg.ec.фФ}). */
public final class BaritoneUtil {

    private BaritoneUtil() {
    }

    public static void shutdown() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        } catch (Throwable ignored) {
        }
    }
}
