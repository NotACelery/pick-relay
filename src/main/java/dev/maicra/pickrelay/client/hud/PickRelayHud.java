package dev.maicra.pickrelay.client.hud;

import dev.maicra.pickrelay.client.PickRelayController;
import dev.maicra.pickrelay.session.RelayEntry;
import dev.maicra.pickrelay.session.RelayMiningMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class PickRelayHud {
    private PickRelayHud() {
    }

    public static void render(GuiGraphics gui) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!PickRelayController.isActive() || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        RelayEntry entry = PickRelayController.activeEntry();
        if (entry == null) {
            return;
        }

        int current = PickRelayController.activeIndex() + 1;
        int total = PickRelayController.queue().size();
        Component text = switch (entry.workMode()) {
            case UNTIL_BROKEN -> Component.translatable("hud.pickrelay.until_broken", current, total);
            case DURABILITY -> Component.translatable(
                    "hud.pickrelay.durability",
                    current,
                    total,
                    entry.durabilityConsumed(),
                    entry.workTarget()
            );
            case BLOCKS -> Component.translatable(
                    "hud.pickrelay.blocks",
                    current,
                    total,
                    entry.blocksBroken(),
                    entry.workTarget()
            );
        };

        if (entry.preserveAtOne()) {
            text = Component.translatable("hud.pickrelay.preserved", text);
        }
        if (PickRelayController.isWaitingForWorkBlock()) {
            text = Component.translatable(
                    PickRelayController.miningMode() == RelayMiningMode.SINGLE_BLOCK
                            ? "hud.pickrelay.waiting_single"
                            : "hud.pickrelay.waiting_line",
                    text
            );
        }

        int x = gui.guiWidth() / 2;
        int y = gui.guiHeight() / 2 + 28;
        gui.drawCenteredString(minecraft.font, text, x, y, 0xFFFFFF);
    }
}
