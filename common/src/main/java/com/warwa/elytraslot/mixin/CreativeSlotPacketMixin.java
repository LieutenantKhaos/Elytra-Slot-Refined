package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.host.ElytraArmorSlot;
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
 * Vanilla's creative set-slot handler hard-rejects slot numbers above 45, which
 * excludes the appended elytra slot. This replicates the handler's exact body
 * (verified against the 26.2 bytecode, including running the thread hop first)
 * for that one slot, identified by class — vanilla bytecode stays untouched, so
 * other mods' modifications to the 45 literal still apply.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeSlotPacketMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void elytraslot$handleElytraSlot(ServerboundSetCreativeModeSlotPacket packet,
                                             CallbackInfo ci) {
        int slotNum = packet.slotNum();
        if (slotNum <= 45) {
            return; // vanilla's range; the drop path (negative) is also vanilla's job
        }
        PacketUtils.ensureRunningOnSameThread(packet,
            (ServerGamePacketListener) (Object) this, this.player.level());
        if (slotNum >= this.player.inventoryMenu.slots.size()) {
            return;
        }
        Slot slot = this.player.inventoryMenu.getSlot(slotNum);
        if (!(slot instanceof ElytraArmorSlot)) {
            return;
        }
        ci.cancel();
        if (!this.player.hasInfiniteMaterials()) {
            return;
        }
        ItemStack stack = packet.itemStack();
        if (!stack.isItemEnabled(this.player.level().enabledFeatures())) {
            return;
        }
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize()) {
            return;
        }
        slot.setByPlayer(stack);
        this.player.inventoryMenu.setRemoteSlot(slotNum, stack);
        this.player.inventoryMenu.broadcastChanges();
    }
}
