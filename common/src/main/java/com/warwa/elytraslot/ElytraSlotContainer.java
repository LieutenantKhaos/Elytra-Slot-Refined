package com.warwa.elytraslot;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Single-slot container backing the custom elytra slot.
 *
 * Extends {@link SimpleContainer} with two behaviours the vanilla class lacks:
 *  1. {@code setItem} fires {@link ElytraEquipEffects#onSlotChanged(Player, ItemStack, ItemStack)}
 *     so any interactive mutation (shift-click, drag-drop, right-click equip path) produces
 *     the same equip sound + {@link net.minecraft.world.level.gameevent.GameEvent#EQUIP} /
 *     {@link net.minecraft.world.level.gameevent.GameEvent#UNEQUIP} side effects that
 *     {@code LivingEntity.onEquipItem} would have produced if the write had gone through
 *     {@code Player.setItemSlot}.
 *  2. {@link #setItemSilent(int, ItemStack)} writes without firing those side effects, for
 *     non-interactive paths: loading from NBT on world load, clearing on death-drop, and
 *     carrying over the stack on {@code ServerPlayer.restoreFrom}.
 */
public class ElytraSlotContainer extends SimpleContainer {

    private final Player owner;
    private boolean silent = false;

    public ElytraSlotContainer(Player owner) {
        super(1);
        this.owner = owner;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        ItemStack old = this.getItem(index).copy();
        super.setItem(index, stack);
        if (!silent && owner != null) {
            ElytraEquipEffects.onSlotChanged(owner, old, stack);
        }
    }

    /**
     * Writes without firing {@link ElytraEquipEffects#onSlotChanged}. Use for NBT load,
     * death drop clearing, and respawn carry-over — paths where equip sound / game event
     * would be wrong.
     */
    public void setItemSilent(int index, ItemStack stack) {
        this.silent = true;
        try {
            super.setItem(index, stack);
        } finally {
            this.silent = false;
        }
    }
}
