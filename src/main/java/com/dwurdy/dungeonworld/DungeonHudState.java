/*
 * Decompiled with CFR 0.152.
 */
package com.dwurdy.dungeonworld;

final class DungeonHudState {
    static long seconds;
    static long receivedAt;

    DungeonHudState() {
    }

    static void update(long remaining) {
        seconds = remaining;
        receivedAt = System.nanoTime();
    }
}

