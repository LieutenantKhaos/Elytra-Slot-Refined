package com.warwa.elytraslot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class ElytraSlotUtil {
    public static boolean isElytraLike(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.has(DataComponents.GLIDER);
    }
}
