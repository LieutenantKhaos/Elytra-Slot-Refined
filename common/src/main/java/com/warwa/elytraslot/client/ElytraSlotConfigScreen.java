package com.warwa.elytraslot.client;

import com.warwa.elytraslot.config.ElytraSlotConfig;
import com.warwa.elytraslot.host.BuiltinHost;
import com.warwa.elytraslot.host.ElytraHosts;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Minimal config screen, reached via ModMenu (Fabric) or the mods list (NeoForge).
 * Edits the local config file; on a dedicated server the server's own config is
 * authoritative for panel visibility and host selection.
 */
public final class ElytraSlotConfigScreen extends Screen {

    private final Screen parent;

    public ElytraSlotConfigScreen(Screen parent) {
        super(Component.translatable("elytraslot.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 36;

        CycleButton<Boolean> panelToggle = CycleButton.onOffBuilder(ElytraSlotConfig.get().showInventoryPanel)
            .create(centerX - 150, y, 300, 20,
                Component.translatable("elytraslot.config.showPanel"),
                (button, value) -> ElytraSlotConfig.get().showInventoryPanel = value);
        panelToggle.setTooltip(Tooltip.create(Component.translatable("elytraslot.config.showPanel.tooltip")));
        panelToggle.active = ElytraHosts.hasExternalInstalled();
        addRenderableWidget(panelToggle);

        List<String> providers = List.of("auto", BuiltinHost.ID, "trinkets", "curios");
        CycleButton<String> providerButton = CycleButton.<String>builder(
                value -> Component.translatable("elytraslot.config.provider." + value),
                normalizeProvider(providers))
            .withValues(providers)
            .create(centerX - 150, y + 24, 300, 20,
                Component.translatable("elytraslot.config.provider"),
                (button, value) -> ElytraSlotConfig.get().slotProvider = value);
        providerButton.setTooltip(Tooltip.create(Component.translatable("elytraslot.config.provider.tooltip")));
        addRenderableWidget(providerButton);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
            .bounds(centerX - 100, y + 60, 200, 20)
            .build());
    }

    private static String normalizeProvider(List<String> providers) {
        String current = ElytraSlotConfig.get().slotProvider;
        return providers.contains(current) ? current : "auto";
    }

    @Override
    public void onClose() {
        ElytraSlotConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }
}
