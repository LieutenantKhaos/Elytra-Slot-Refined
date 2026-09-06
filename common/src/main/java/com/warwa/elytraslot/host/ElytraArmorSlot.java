package com.warwa.elytraslot.host;

import com.warwa.elytraslot.ElytraSlot;
import com.warwa.elytraslot.Gliders;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The elytra slot in the inventory menu: a real armor slot over the BODY
 * equipment slot (Inventory container index 41), appended to InventoryMenu at
 * index 46 on both sides. Extending vanilla's ArmorSlot inherits the exact
 * vanilla slot semantics: setByPlayer fires onEquipItem (game event + equip
 * sound via BodyEquipSoundMixin), max stack size 1, and Curse of Binding
 * blocking pickup.
 *
 * <p>All lookups find this slot by class identity, never by index, so other
 * mods appending menu slots cannot break it.
 */
public final class ElytraArmorSlot extends ArmorSlot {

    private static final Identifier EMPTY_ICON = ElytraSlot.resourceId("container/slot/elytra");

    private final Player owner;

    public ElytraArmorSlot(Inventory inventory, Player owner, int x, int y) {
        super(inventory, owner, EquipmentSlot.BODY, Inventory.SLOT_BODY_ARMOR, x, y, EMPTY_ICON);
        this.owner = owner;
    }

    /**
     * Moves the slot with its panel (the recipe book pushes the panel to the other
     * side of the GUI). Client-side only: slot positions are never networked, and
     * the server's own menu instance keeps the default position.
     */
    public void elytraslot$moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!Gliders.isChestGlider(stack)) {
            return false;
        }
        // Vanilla ArmorSlot.mayPlace routes through isEquippableInSlot, which also
        // honours the item's allowed-entities restriction; keep that check.
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || !equippable.canBeEquippedBy(owner.typeHolder())) {
            return false;
        }
        // Exclusivity: never a second glider next to the vanilla chest slot or an
        // active external (Trinkets/Curios) slot.
        if (Gliders.isGlider(owner.getItemBySlot(EquipmentSlot.CHEST))) {
            return false;
        }
        ElytraHost host = ElytraHosts.forSide(owner);
        return host.isBuiltin() || !Gliders.isGlider(host.get(owner));
    }

    @Override
    public boolean isActive() {
        return ElytraHosts.panelVisible(owner.level().isClientSide());
    }
}
