package com.warwa.elytraslot.neoforge.compat.curios;

import com.warwa.elytraslot.ElytraSlot;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHosts;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.common.NeoForgeMod;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The curio behavior registered for glider items. Right-click equips through
 * Curios' own pipeline, the equip sound comes from the item's vanilla EQUIPPABLE
 * component, and equipping is refused while a glider is worn on the vanilla chest
 * or this mod's BODY slot (exclusivity).
 *
 * <p>Curios queries slot eligibility with no wearer (a null entity in the slot
 * context) when building tooltips and default attribute modifiers, so every
 * entity dereference here is null-guarded.
 */
public final class GliderCurio implements ICurioItem {

    static final GliderCurio INSTANCE = new GliderCurio();

    private static final AttributeModifier GLIDING_FLIGHT_MODIFIER = new AttributeModifier(
        ElytraSlot.id("curios_glider_flight"), 1.0D, AttributeModifier.Operation.ADD_VALUE);

    private GliderCurio() {
    }

    /**
     * This is what actually makes the elytra fly from a Curios slot on NeoForge.
     *
     * <p>NeoForge does not decide gliding by scanning equipment: it reads the
     * {@code neoforge:gliding_flight} attribute, and its own handler only scopes an
     * elytra's modifier to the CHEST equipment slot — which a Curios slot never
     * fills. Supplying the modifier as a curio modifier lets Curios apply and remove
     * it with the item, and because the attribute is syncable the client agrees and
     * actually sends the start-gliding packet (the client gates that packet on its
     * own check, so a server-only fix would never start flight at all).
     *
     * <p>The broken-item gate mirrors NeoForge's own: a glider one hit from breaking
     * stops working rather than breaking, exactly like vanilla.
     */
    @Override
    public CurioAttributeModifiers getDefaultCurioAttributeModifiers(ItemStack stack) {
        if (stack.nextDamageWillBreak()) {
            return CurioAttributeModifiers.EMPTY;
        }
        return CurioAttributeModifiers.builder()
            .addModifier(NeoForgeMod.GLIDING_FLIGHT, GLIDING_FLIGHT_MODIFIER, CuriosHost.SLOT_ID)
            .build()
            .withTooltip(false);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (entity == null) {
            return true; // wearer-less eligibility query
        }
        // Only ever our own slot, and only while Curios actually hosts the elytra:
        // registering our slot type makes gliders eligible for Curios' generic
        // "curio" slot too, and in other host modes they do not belong in Curios
        // slots at all.
        if (ElytraHosts.forSide(entity) != CuriosHost.INSTANCE
            || !CuriosHost.SLOT_ID.equals(slotContext.identifier())) {
            return false;
        }
        return !Gliders.isGlider(entity.getItemBySlot(EquipmentSlot.CHEST))
            && !Gliders.isGlider(entity.getItemBySlot(EquipmentSlot.BODY));
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        return entity != null
            && ElytraHosts.forSide(entity) == CuriosHost.INSTANCE
            && canEquip(slotContext, stack);
    }

    /**
     * Curios' default equip-from-use path plays the sound through a stack-less
     * default instance, which always resolves to the generic armor sound, so the
     * sound is played here instead.
     */
    @Override
    public void onEquipFromUse(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (entity == null || entity.isSilent()) {
            return;
        }
        ICurio.SoundInfo sound = getEquipSound(slotContext, stack);
        entity.level().playSeededSound(null, entity.getX(), entity.getY(), entity.getZ(),
            sound.soundEvent(), entity.getSoundSource(), sound.volume(), sound.pitch(),
            entity.getRandom().nextLong());
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            return new ICurio.SoundInfo(equippable.equipSound().value(), 1.0F, 1.0F);
        }
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_ELYTRA.value(), 1.0F, 1.0F);
    }
}
