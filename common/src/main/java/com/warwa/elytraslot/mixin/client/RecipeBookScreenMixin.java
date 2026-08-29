package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.client.ElytraSlotClickGuard;
import com.warwa.elytraslot.client.RecipeBookAware;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops clicks on or beside the elytra slot from being treated as clicks outside
 * the GUI, which would throw a held item on the ground.
 *
 * <p>The injection targets {@link AbstractRecipeBookScreen} rather than
 * AbstractContainerScreen: its override of hasClickedOutside inlines the whole
 * rectangle test without calling super, so an AbstractContainerScreen-level
 * injection would never run for the screens that matter. Both the vanilla
 * inventory (where our panel and any Trinkets slots live) and the Curios screen
 * extend it, so one hook covers every elytra slot.
 *
 * <p>This mixin also exposes the book's open state, which decides which side of
 * the GUI our own panel is drawn on.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookScreenMixin implements RecipeBookAware {

    @Shadow
    @Final
    private RecipeBookComponent<?> recipeBookComponent;

    @Override
    public boolean elytraslot$isRecipeBookOpen() {
        return this.recipeBookComponent != null && this.recipeBookComponent.isVisible();
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void elytraslot$nearElytraSlotIsInside(double mouseX, double mouseY,
                                                   int guiLeft, int guiTop,
                                                   CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (ElytraSlotClickGuard.nearElytraSlot(screen.getMenu(), guiLeft, guiTop, mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }
}
