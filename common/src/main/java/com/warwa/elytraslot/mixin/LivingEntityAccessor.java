package com.warwa.elytraslot.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes protected members of {@link Entity} needed for vanilla-parity behavior:
 * <ul>
 *   <li>{@code setSharedFlag(int, boolean)} — used to clear the fall-flying flag (7)
 *       without the double-toggle that {@code LivingEntity.stopFallFlying()} would
 *       introduce.</li>
 *   <li>{@code firstTick} — used to match vanilla's {@code onEquipItem} guard that
 *       suppresses equip side effects on the very first tick after entity spawn.</li>
 * </ul>
 */
@Mixin(Entity.class)
public interface LivingEntityAccessor {

    @Invoker("setSharedFlag")
    void elytraslot$setSharedFlag(int flag, boolean value);

    @Accessor("firstTick")
    boolean elytraslot$isFirstTick();
}
