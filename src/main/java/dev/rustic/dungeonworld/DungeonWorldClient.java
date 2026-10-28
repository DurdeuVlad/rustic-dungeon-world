/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.neoforge.client.event.RenderGuiEvent$Post
 *  net.neoforged.neoforge.common.NeoForge
 */
package dev.rustic.dungeonworld;

import dev.rustic.dungeonworld.DungeonHudState;
import dev.rustic.dungeonworld.DungeonWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value="rustic_dungeon_world", dist={Dist.CLIENT})
public final class DungeonWorldClient {
    public DungeonWorldClient() {
        NeoForge.EVENT_BUS.addListener(DungeonWorldClient::hud);
    }

    private static void hud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !mc.level.dimension().equals(DungeonWorld.DIMENSION) || DungeonHudState.seconds == 0L) {
            return;
        }
        long seconds = Math.max(0L, DungeonHudState.seconds - (System.nanoTime() - DungeonHudState.receivedAt) / 1000000000L);
        String label = "Dungeon time: " + DungeonWorld.clock(seconds);
        GuiGraphics g = event.getGuiGraphics();
        int x = (g.guiWidth() - mc.font.width(label)) / 2;
        int y = g.guiHeight() - 64;
        g.fill(x - 4, y - 3, x + mc.font.width(label) + 4, y + 11, -1728053248);
        g.drawString(mc.font, label, x, y, seconds <= 300L ? -39322 : -11136, true);
    }
}

