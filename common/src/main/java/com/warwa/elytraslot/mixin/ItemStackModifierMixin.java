package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.Gliders;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A chest-equippable glider keeps its chest-scoped attribute modifiers while
 * sitting in the BODY slot.
 *
 * <p>Modifier entries are filtered by {@code EquipmentSlotGroup.test(slot)}, and
 * the single-slot CHEST group does not match BODY — so a modded elytra that
 * declares its armor or toughness modifiers for the chest (the natural choice for
 * a chest-equippable item) would otherwise contribute nothing in our slot.
 *
 * <p>The modifiers are re-keyed as well as re-scoped. Modifier ids are commonly
 * slot-derived rather than item-derived ({@code minecraft:armor.chestplate} for
 * every chestplate, {@code <enchantment>/chest} for enchantment effects), and
 * attributes are stored by id alone — so handing them through unchanged would let
 * a glider in BODY evict the real chestplate's modifiers, and then delete them
 * when the glider is removed. The suffix keeps the two sets distinct.
 *
 * <p>Vanilla applies and strips modifiers through this same method, so the
 * re-keying is symmetric and nothing can leak on unequip. Modifiers scoped
 * exclusively to BODY on a chest glider are not carried over (an "any" scope
 * covers both and is included).
 */
@Mixin(ItemStack.class)
public abstract class ItemStackModifierMixin {

    @Unique
    private static final String ELYTRASLOT$ID_SUFFIX = "/elytraslot_body";

    @Inject(
        method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void elytraslot$bodyGliderKeepsChestModifiers(
        EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
        CallbackInfo ci) {
        if (slot != EquipmentSlot.BODY) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (!Gliders.isChestGlider(self)) {
            return;
        }
        ci.cancel();
        // Re-entrant with slot == CHEST, which returns immediately above.
        self.forEachModifier(EquipmentSlot.CHEST, (attribute, modifier) ->
            consumer.accept(attribute, new AttributeModifier(
                modifier.id().withSuffix(ELYTRASLOT$ID_SUFFIX),
                modifier.amount(),
                modifier.operation())));
    }
}
