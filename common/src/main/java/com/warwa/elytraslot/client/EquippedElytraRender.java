package com.warwa.elytraslot.client;

import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHost;
import com.warwa.elytraslot.host.ElytraHosts;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Client-side resolution of the elytra this mod should render on a player's back. */
public final class EquippedElytraRender {

    private EquippedElytraRender() {
    }

    /**
     * The glider to render, or EMPTY. BODY equipment renders in every mode
     * (vanilla syncs it but has no player BODY render layer); an external host's
     * stack renders only when that host has no native elytra rendering (Curios —
     * Trinkets renders its own).
     */
    public static ItemStack resolve(Player player) {
        ItemStack body = player.getItemBySlot(EquipmentSlot.BODY);
        if (Gliders.isGlider(body)) {
            return body;
        }
        ElytraHost host = ElytraHosts.client();
        if (!host.isBuiltin()) {
            ItemStack hostStack = host.get(player);
            if (Gliders.isGlider(hostStack) && !host.managesRenderingOf(hostStack)) {
                return hostStack;
            }
        }
        return ItemStack.EMPTY;
    }
}
