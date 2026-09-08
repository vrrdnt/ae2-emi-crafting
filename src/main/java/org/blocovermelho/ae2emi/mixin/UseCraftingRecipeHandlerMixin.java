package org.blocovermelho.ae2emi.mixin;

import appeng.integration.modules.emi.EmiUseCraftingRecipeHandler;
import appeng.menu.me.items.CraftingTermMenu;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import org.blocovermelho.ae2emi.client.MachineRecipeTransfer;
import org.blocovermelho.ae2emi.network.Ae2EmiNetwork;
import org.blocovermelho.ae2emi.network.TerminalIngredientRequest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EmiUseCraftingRecipeHandler.class, remap = false)
public abstract class UseCraftingRecipeHandlerMixin {
    @Inject(method = "supportsRecipe", at = @At("HEAD"), cancellable = true)
    private void ae2emi$supportMachineTransfer(
            EmiRecipe recipe, CallbackInfoReturnable<Boolean> callback) {
        if (MachineRecipeTransfer.isSupported(recipe)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private void ae2emi$transferMachineIngredients(
            EmiRecipe recipe,
            EmiCraftContext<?> context,
            CallbackInfoReturnable<Boolean> callback) {
        if (!(context.getScreenHandler() instanceof CraftingTermMenu menu)
                || context.getDestination() != EmiCraftContext.Destination.INVENTORY
                || !MachineRecipeTransfer.isSupported(recipe)) {
            return;
        }

        var plan = MachineRecipeTransfer.createRequest(recipe, context.getInventory(), context.getAmount());
        MachineRecipeTransfer.reportShortfall(plan, context.getAmount());
        if (plan.requirements().isEmpty()) {
            callback.setReturnValue(false);
            return;
        }

        Ae2EmiNetwork.sendToServer(new TerminalIngredientRequest(menu.containerId, plan.batches(), plan.requirements()));
        callback.setReturnValue(true);
    }
}
