package com.warwa.elytraslot.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {

    /** Shared flag 7 is the fall-flying flag; cleared directly to mirror vanilla updateFallFlying. */
    @Invoker("setSharedFlag")
    void elytraslot$setSharedFlag(int flag, boolean value);
}
