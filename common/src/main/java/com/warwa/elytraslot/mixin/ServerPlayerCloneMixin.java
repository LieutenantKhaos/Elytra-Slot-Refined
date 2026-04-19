package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mirrors vanilla {@code ServerPlayer.restoreFrom} inventory-carry rule.
 *
 * Vanilla 26.1.2 carries the old player's inventory over to the new player when:
 * <pre>
 *     restoreAll || oldPlayer.isSpectator() || level.gameRules.KEEP_INVENTORY
 * </pre>
 * The {@code restoreAll} flag is {@code true} for non-death clones (end-portal return,
 * dimension change); {@code false} for post-death respawn, where the gamerule /
 * spectator status decides.
 *
 * We apply the same rule to the custom elytra slot. When the rule doesn't apply, the
 * old player's slot has already been drained by {@link PlayerElytraStorageMixin}'s
 * {@code dropOnDeath} (with its own keepInventory gate), so there's nothing to carry.
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerCloneMixin {

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void elytraslot$carryOver(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        boolean keepInventory = this.elytraslot$shouldCarry(oldPlayer, restoreAll);
        if (!keepInventory) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] restoreFrom skipping carry (restoreAll={}, spec={}, keepInv={}, player={})",
                restoreAll,
                oldPlayer.isSpectator(),
                this.elytraslot$keepInventoryRule(),
                oldPlayer.getName().getString()
            );
            return;
        }
        ItemStack oldStack = ((IElytraSlotPlayer) oldPlayer).elytraslot_getElytraStack();
        if (!oldStack.isEmpty()) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] restoreFrom carrying stack={} player={}",
                oldStack, oldPlayer.getName().getString()
            );
            // Silent — respawn carry-over is not an equip interaction; no sound / game event.
            ((IElytraSlotPlayer) (Object) this).elytraslot_setElytraStackSilent(oldStack.copy());
        }
    }

    private boolean elytraslot$shouldCarry(ServerPlayer oldPlayer, boolean restoreAll) {
        if (restoreAll) return true;                 // end-portal return / dimension change
        if (oldPlayer.isSpectator()) return true;    // spectator death (vanilla carries inv)
        return this.elytraslot$keepInventoryRule();  // rest: keepInventory gamerule
    }

    private boolean elytraslot$keepInventoryRule() {
        ServerPlayer self = (ServerPlayer) (Object) this;
        // ServerPlayer.level() overrides Entity.level() to return ServerLevel directly.
        return self.level().getGameRules().get(GameRules.KEEP_INVENTORY);
    }
}
