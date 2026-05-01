package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds our custom elytra slot to the player's {@link InventoryMenu}. The slot is
 * appended to the end of the menu's slot list — its index is therefore
 * {@code menu.slots.size() - 1} at the time it's added (46 in vanilla).
 *
 * <p>Overrides:
 * <ul>
 *   <li>{@link Slot#mayPlace} — only elytra-like items, and only when the vanilla
 *       chest slot doesn't also have an elytra-like item.</li>
 *   <li>{@link Slot#mayPickup} — honors {@code PREVENT_ARMOR_CHANGE} (Curse of
 *       Binding), matching vanilla {@code ArmorSlot.mayPickup} (UI2 fix).</li>
 *   <li>{@link Slot#getNoItemIcon} — points at our empty-slot elytra sprite.</li>
 * </ul>
 *
 * <p>The slot's index in the menu is exposed as
 * {@link com.warwa.elytraslot.ElytraSlotConstants#ELYTRA_SLOT_INDEX} (46).
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void elytraslot$addElytraSlot(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        InventoryMenu menu = (InventoryMenu) (Object) this;
        var container = ((IElytraSlotPlayer) player).elytraslot_getElytraContainer();
        int insertedAtIndex = menu.slots.size(); // this is the index this addSlot will land at
        menu.addSlot(new Slot(container, 0, -25, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!ElytraSlotUtil.isElytraLike(stack)) {
                    ElytraSlotConstants.LOGGER.info(
                        "[elytraslot] elytraSlot.mayPlace DENY not-elytra-like stack={}", stack
                    );
                    return false;
                }
                ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
                boolean chestHasElytra = ElytraSlotUtil.isElytraLike(chestItem);
                if (chestHasElytra) {
                    ElytraSlotConstants.LOGGER.info(
                        "[elytraslot] elytraSlot.mayPlace DENY vanilla chest already has elytra chestItem={}",
                        chestItem
                    );
                    return false;
                }
                return true;
            }

            /**
             * UI2 fix: mirror vanilla {@code ArmorSlot.mayPickup}. Block non-creative
             * pickup when the stack carries the {@code PREVENT_ARMOR_CHANGE} component
             * (Curse of Binding). Without this, Curse of Binding is ineffective on the
             * custom slot.
             */
            @Override
            public boolean mayPickup(Player p) {
                ItemStack inSlot = this.getItem();
                if (!inSlot.isEmpty()
                    && !p.isCreative()
                    && EnchantmentHelper.has(inSlot, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                    ElytraSlotConstants.LOGGER.info(
                        "[elytraslot] elytraSlot.mayPickup DENY binding-curse player={} stack={}",
                        p.getName().getString(), inSlot
                    );
                    return false;
                }
                return super.mayPickup(p);
            }

            @Override
            public Identifier getNoItemIcon() {
                return Identifier.fromNamespaceAndPath("elytraslot", "container/slot/elytra");
            }
        });
        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] elytraSlot added to InventoryMenu at index={} player={}",
            insertedAtIndex, player.getName().getString()
        );
    }
}
