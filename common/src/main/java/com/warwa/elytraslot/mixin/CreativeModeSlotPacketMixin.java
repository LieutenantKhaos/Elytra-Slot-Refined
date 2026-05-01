package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotContainer;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the creative-mode set-slot packet target our custom elytra slot (index 46),
 * which vanilla rejects because its inline range check is {@code slotNum >= 1 && slotNum <= 45}.
 *
 * <p><b>Why not {@code @ModifyConstant} on the {@code 45}?</b> Other mods that add custom
 * inventory slots (e.g. <i>Trinkets Updated</i>'s {@code modifyCreativeSlotMax}) already
 * use {@code @ModifyConstant} to bump that constant. Two {@code @ModifyConstant}s targeting
 * the same literal are mutually destructive: the first one rewrites the constant, the
 * second sees no matching {@code 45} in the bytecode anymore and fails its injection check
 * with {@code "Scanned 0 target(s)"} — a hard crash at class load. Stacking happily on
 * the same constant is not how that injector works.
 *
 * <p><b>The non-conflicting alternative.</b> {@code @Inject} at HEAD with
 * {@code cancellable=true}, gated on {@code packet.slotNum() == ELYTRA_SLOT_INDEX}, and
 * replicating vanilla's slot-set body for that one case. Vanilla's bytecode (including the
 * {@code 45} literal) stays untouched, so any other mod's {@code @ModifyConstant} on the
 * upper bound still finds its target and applies cleanly.
 *
 * <p>The replicated body mirrors vanilla exactly:
 * <pre>
 *   ensureRunningOnSameThread → creative gate → feature-flag gate → stack-size gate →
 *   slot.setByPlayer → setRemoteSlot → broadcastChanges
 * </pre>
 * which routes the write through {@link com.warwa.elytraslot.ElytraSlotContainer#setItem}
 * (firing equip side effects + dispatcher broadcast) — same outcome as if vanilla had
 * accepted the packet itself.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeModeSlotPacketMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void elytraslot$handleElytraSlotPacket(ServerboundSetCreativeModeSlotPacket packet,
                                                   CallbackInfo ci) {
        int slotNum = packet.slotNum();

        // Bounds check first — packet.slotNum() can be negative (drop-item path) or out of
        // range. Skip those entirely; they're vanilla's job, not ours.
        if (slotNum < 0 || slotNum >= this.player.inventoryMenu.slots.size()) return;

        // Identify our slot by container identity (not index). Other mods that add slots
        // to InventoryMenu.<init> at @Inject(at=RETURN) — e.g. Trinkets Updated — can
        // shift our slot off index 46 depending on mixin order, so the constant is
        // unreliable in mod packs. The ElytraSlotContainer instance is unique to us.
        Slot slot = this.player.inventoryMenu.getSlot(slotNum);
        if (slot == null || !(slot.container instanceof ElytraSlotContainer)) return;

        // Vanilla's first line. Without it we'd be mutating off-thread on a chat/IO event.
        // The packet is Packet<ServerGamePacketListener>, so the listener cast must match
        // that exact bound for the generic ensureRunningOnSameThread to type-check.
        // ServerPlayer.level() returns ServerLevel in 26.1 (no separate serverLevel() method).
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListener) (Object) this, this.player.level());

        if (!this.player.hasInfiniteMaterials()) {
            // Non-creative players cannot use this packet — silently drop, mirroring vanilla.
            ci.cancel();
            return;
        }

        ItemStack stack = packet.itemStack();
        // .level() in 26.1 returns ServerLevel; .enabledFeatures() lives on ServerLevel/Level.
        if (!stack.isItemEnabled(this.player.level().enabledFeatures())) {
            // Feature-flag gated item (e.g. experimental); vanilla returns silently.
            ci.cancel();
            return;
        }

        boolean stackOk = stack.isEmpty() || stack.getCount() <= stack.getMaxStackSize();
        if (!stackOk) {
            // Vanilla also rejects oversize stacks for in-range slots.
            ci.cancel();
            return;
        }

        // Set the slot, update the remote-snapshot mirror, and flush. Identical to vanilla's
        // in-range branch — just for our elytra slot instead of indices 1..45.
        slot.setByPlayer(stack);
        this.player.inventoryMenu.setRemoteSlot(slotNum, stack);
        this.player.inventoryMenu.broadcastChanges();

        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] handleSetCreativeModeSlot stored stack={} into slot={} player={}",
            stack, slotNum, this.player.getName().getString()
        );

        ci.cancel();
    }
}
