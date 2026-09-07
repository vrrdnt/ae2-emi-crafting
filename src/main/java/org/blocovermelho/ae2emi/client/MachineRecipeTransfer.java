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
    private record ItemIngredient(EmiIngredient ingredient, boolean catalyst) {
    }

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
        for (var itemIngredient : getItemIngredients(recipe)) {
            long requiredAmount = Math.max(1, itemIngredient.ingredient.getAmount());
            var alternatives = itemIngredient.ingredient.getEmiStacks().stream()
                    .filter(stack -> !stack.getItemStack().isEmpty())
                    .map(stack -> new MachineIngredientSelector.Alternative<>(stack, requiredAmount))
                    .toList();
            ingredients.add(new MachineIngredientSelector.Ingredient<>(alternatives, itemIngredient.catalyst));
        }

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

    private static List<ItemIngredient> getItemIngredients(EmiRecipe recipe) {
        var result = new ArrayList<ItemIngredient>();
        addItemIngredients(result, recipe.getInputs(), false);
        addItemIngredients(result, recipe.getCatalysts(), true);
        return result;
    }

    private static void addItemIngredients(
            List<ItemIngredient> result, List<EmiIngredient> ingredients, boolean catalyst) {
        for (var ingredient : ingredients) {
            if (!ingredient.isEmpty()
                    && ingredient.getEmiStacks().stream().anyMatch(stack -> !stack.getItemStack().isEmpty())) {
                result.add(new ItemIngredient(ingredient, catalyst));
            }
        }
    }
}
