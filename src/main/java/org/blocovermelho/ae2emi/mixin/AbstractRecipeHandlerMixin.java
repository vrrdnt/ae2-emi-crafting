package org.blocovermelho.ae2emi.mixin;

import java.util.ArrayList;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.emi.EmiStackHelper;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.CraftingTermMenu;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.blocovermelho.ae2emi.network.Ae2EmiNetwork;
import org.blocovermelho.ae2emi.network.TerminalCraftRequest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.integration.modules.emi.AbstractRecipeHandler", remap = false)
public abstract class AbstractRecipeHandlerMixin {
    @Inject(method = "getInventory", at = @At("HEAD"), cancellable = true)
    private void ae2emi$includeNetworkInventory(
            AbstractContainerScreen<?> screen, CallbackInfoReturnable<EmiPlayerInventory> callback) {
        if (!(screen.getMenu() instanceof CraftingTermMenu menu)) {
            return;
        }

        var stacks = new ArrayList<EmiStack>();
        menu.getSlots(SlotSemantics.PLAYER_INVENTORY)
                .forEach(slot -> stacks.add(EmiStack.of(slot.getItem())));
        menu.getSlots(SlotSemantics.PLAYER_HOTBAR)
                .forEach(slot -> stacks.add(EmiStack.of(slot.getItem())));
        menu.getSlots(SlotSemantics.CRAFTING_GRID)
                .forEach(slot -> stacks.add(EmiStack.of(slot.getItem())));

        var repository = menu.getClientRepo();
        if (repository != null) {
            for (var entry : repository.getAllEntries()) {
                if (!(entry.getWhat() instanceof AEItemKey) || entry.getStoredAmount() <= 0) {
                    continue;
                }

                EmiStack stack = EmiStackHelper.toEmiStack(
                        new GenericStack(entry.getWhat(), entry.getStoredAmount()));
                if (stack != null) {
                    stacks.add(stack);
                }
            }
        }

        callback.setReturnValue(new EmiPlayerInventory(stacks));
    }

    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true)
    private void ae2emi$checkAvailableBatch(
            EmiRecipe recipe,
            EmiCraftContext<?> context,
            CallbackInfoReturnable<Boolean> callback) {
        if (!(context.getScreenHandler() instanceof CraftingTermMenu)
                || context.getType() != EmiCraftContext.Type.CRAFTABLE) {
            return;
        }

        // Synthetic favorites turn "craft all" into a finite batch count. That count is
        // an upper bound, not a requirement: EMI allows partially craftable favorites.
        // Check one batch here; the server stops at the request limit or when supplies run out.
        callback.setReturnValue(context.getInventory().canCraft(recipe));
    }

    @Inject(method = "craft", at = @At("RETURN"))
    private void ae2emi$finishRequestedAction(
            EmiRecipe recipe,
            EmiCraftContext<?> context,
            CallbackInfoReturnable<Boolean> callback) {
        if (!Boolean.TRUE.equals(callback.getReturnValue())
                || !(context.getScreenHandler() instanceof CraftingTermMenu menu)) {
            return;
        }

        TerminalCraftRequest.Destination destination = switch (context.getDestination()) {
            case NONE -> TerminalCraftRequest.Destination.NONE;
            case CURSOR -> TerminalCraftRequest.Destination.CURSOR;
            case INVENTORY -> TerminalCraftRequest.Destination.INVENTORY;
        };

        if (destination != TerminalCraftRequest.Destination.NONE || context.getAmount() > 1) {
            Ae2EmiNetwork.sendToServer(
                    new TerminalCraftRequest(menu.containerId, destination, context.getAmount()));
        }
    }
}
