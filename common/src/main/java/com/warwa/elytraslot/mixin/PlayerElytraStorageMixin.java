package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotContainer;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a persistent 1-slot container to {@link Player} for elytra storage and wires
 * it into vanilla's equipment save/load/drop lifecycle:
 *
 * <ul>
 *   <li>Save / load — via {@code addAdditionalSaveData} / {@code readAdditionalSaveData}
 *       using the MC 26.1 {@code ValueOutput}/{@code ValueInput} API with key
 *       {@code "elytraslot_item"}. Load uses the silent container setter.</li>
 *   <li>Death drop — mirrors vanilla {@code Player.dropEquipment}: respects the
 *       {@link GameRules#RULE_KEEPINVENTORY} gamerule, and honors
 *       {@link EnchantmentEffectComponents#PREVENT_EQUIPMENT_DROP} (Curse of Vanishing)
 *       by destroying the stack instead of dropping it.</li>
 * </ul>
 */
@Mixin(Player.class)
public class PlayerElytraStorageMixin implements IElytraSlotPlayer {

    @Unique
    private final ElytraSlotContainer elytraslot_container =
        new ElytraSlotContainer((Player) (Object) this);

    @Override
    public ElytraSlotContainer elytraslot_getElytraContainer() {
        return this.elytraslot_container;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void elytraslot$saveData(ValueOutput output, CallbackInfo ci) {
        ItemStack stack = this.elytraslot_container.getItem(0);
        if (!stack.isEmpty()) {
            output.store("elytraslot_item", ItemStack.CODEC, stack);
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] saveData wrote stack={}", stack
            );
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void elytraslot$loadData(ValueInput input, CallbackInfo ci) {
        input.read("elytraslot_item", ItemStack.CODEC).ifPresent(stack -> {
            // Silent — we don't want an equip sound when the player logs in and their
            // saved elytra stack is restored from disk.
            this.elytraslot_container.setItemSilent(0, stack);
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] loadData restored stack={}", stack
            );
        });
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void elytraslot$dropOnDeath(ServerLevel level, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        ItemStack stack = this.elytraslot_container.getItem(0);
        if (stack.isEmpty()) return;

        // D1 fix: mirror vanilla Player.dropEquipment — do nothing if keepInventory is on.
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] dropOnDeath skipped (keepInventory) player={} stack={}",
                player.getName().getString(), stack
            );
            return;
        }

        // D2 fix: honor Curse of Vanishing (PREVENT_EQUIPMENT_DROP enchantment). Vanilla's
        // Player.destroyVanishingCursedItems destroys cursed items rather than dropping
        // them. Do the same for the custom slot.
        if (EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] dropOnDeath destroyed (vanishing curse) player={} stack={}",
                player.getName().getString(), stack
            );
            this.elytraslot_container.setItemSilent(0, ItemStack.EMPTY);
            return;
        }

        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] dropOnDeath dropping player={} stack={}",
            player.getName().getString(), stack
        );
        player.drop(stack, true, false);
        // Silent — dropping to the world is not an equip/unequip interaction.
        this.elytraslot_container.setItemSilent(0, ItemStack.EMPTY);
    }
}
