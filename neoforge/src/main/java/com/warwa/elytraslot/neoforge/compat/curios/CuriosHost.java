package com.warwa.elytraslot.neoforge.compat.curios;

import com.warwa.elytraslot.host.ElytraHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Stores the elytra in the Curios {@code elytra} slot (registered by this mod's
 * data files). Curios natively handles storage, persistence, sync, GUI, Mending
 * (via its XP-pickup handler), right-click equip (via the registered curio's
 * canEquipFromUse), and death drops — but provides no gliding, no enchantment
 * equipment iteration, and no elytra rendering, so this mod supplies flight,
 * durability, enchantment parity, and wings rendering while this host is active.
 *
 * <p>Only classloaded when Curios is installed (NeoForge only).
 */
public final class CuriosHost implements ElytraHost {

    public static final CuriosHost INSTANCE = new CuriosHost();
    public static final String ID = "curios";
    public static final String SLOT_ID = "elytra";

    private CuriosHost() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ItemStack get(Player player) {
        ICurioStacksHandler stacks = stacks(player);
        return stacks != null && stacks.getSlots() > 0
            ? stacks.getStacks().getStackInSlot(0)
            : ItemStack.EMPTY;
    }

    @Override
    public boolean set(Player player, ItemStack stack, boolean silent) {
        ICurioStacksHandler stacks = stacks(player);
        if (stacks == null || stacks.getSlots() == 0) {
            return false;
        }
        stacks.getStacks().setStackInSlot(0, stack);
        return true;
    }

    @Override
    public boolean isHostSlot(Slot slot) {
        return slot instanceof CurioSlot curioSlot && SLOT_ID.equals(curioSlot.getIdentifier());
    }

    @Override
    public boolean canAccept(Player player, ItemStack stack) {
        ICurioStacksHandler stacks = stacks(player);
        // isItemValid runs the slot validator, the curio's own canEquip veto and
        // Curios' can-equip event — the same gate as dragging it in by hand.
        return stacks != null && stacks.getSlots() > 0 && stacks.getStacks().isItemValid(0, stack);
    }

    @Override
    public void hurt(Player player, int amount) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ICuriosItemHandler curios = CuriosApi.getCuriosInventoryOrNull(player);
        if (curios == null) {
            return;
        }
        curios.findCurio(SLOT_ID, 0).ifPresent(result -> {
            ItemStack stack = result.stack();
            SlotContext context = result.slotContext();
            stack.hurtAndBreak(amount, serverLevel,
                player instanceof ServerPlayer serverPlayer ? serverPlayer : null,
                brokenItem -> CuriosApi.broadcastCurioBreakEvent(context));
        });
    }

    @Override
    public boolean managesXpRepair() {
        return true;
    }

    @Override
    public boolean managesUseEquip() {
        return true;
    }

    @Override
    public boolean managesDeathDrops() {
        return true;
    }

    @Nullable
    private static ICurioStacksHandler stacks(Player player) {
        ICuriosItemHandler curios = CuriosApi.getCuriosInventoryOrNull(player);
        return curios != null ? curios.getStacksHandler(SLOT_ID).orElse(null) : null;
    }
}
