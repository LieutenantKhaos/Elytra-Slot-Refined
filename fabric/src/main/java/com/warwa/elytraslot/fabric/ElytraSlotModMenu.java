package com.warwa.elytraslot.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.warwa.elytraslot.client.ElytraSlotConfigScreen;

/** Only classloaded when ModMenu is installed. */
public final class ElytraSlotModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ElytraSlotConfigScreen::new;
    }
}
