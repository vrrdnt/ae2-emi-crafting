package org.blocovermelho.ae2emi.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import appeng.api.stacks.AEItemKey;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import org.blocovermelho.ae2emi.network.TerminalIngredientRequest;

public final class MachineRecipeTransfer {
    private MachineRecipeTransfer() {
    }

    public static boolean isSupported(EmiRecipe recipe) {
        return !VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory())
                && recipe.supportsRecipeTree();
    }

    public static Optional<List<TerminalIngredientRequest.ItemRequirement>> createRequest(
            EmiRecipe recipe, EmiPlayerInventory inventory, int requestedBatches) {
        if (!isSupported(recipe) || requestedBatches <= 0) {
            return Optional.empty();
        }

        var ingredients = new ArrayList<MachineIngredientSelector.Ingredient<EmiStack>>();
        addItemIngredients(ingredients, recipe.getInputs(), false);
        addItemIngredients(ingredients, recipe.getCatalysts(), true);

        return MachineIngredientSelector.selectMaximum(
                        ingredients,
                        stack -> {
                            EmiStack available = inventory.inventory.get(stack);
                            return available == null ? 0 : available.getAmount();
                        },
                        requestedBatches,
                        TerminalIngredientRequest.MAX_TOTAL_ITEMS)
                .map(batchSelection -> batchSelection.selections().stream()
                        .map(selection -> new TerminalIngredientRequest.ItemRequirement(
                                AEItemKey.of(selection.key().getItemStack()), selection.amount()))
                        .toList())
                .filter(requirements -> requirements.size() <= TerminalIngredientRequest.MAX_REQUIREMENTS);
    }

    private static void addItemIngredients(
            List<MachineIngredientSelector.Ingredient<EmiStack>> result,
            List<EmiIngredient> ingredients, boolean catalyst) {
        for (var ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            long requiredAmount = Math.max(1, ingredient.getAmount());
            var alternatives = new ArrayList<MachineIngredientSelector.Alternative<EmiStack>>();
            for (var stack : ingredient.getEmiStacks()) {
                if (!stack.getItemStack().isEmpty()) {
                    alternatives.add(new MachineIngredientSelector.Alternative<>(stack, requiredAmount));
                }
            }
            if (!alternatives.isEmpty()) {
                result.add(new MachineIngredientSelector.Ingredient<>(alternatives, catalyst));
            }
        }
    }
}
