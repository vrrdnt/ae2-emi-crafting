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
import net.minecraft.world.item.ItemStack;
import org.blocovermelho.ae2emi.network.TerminalIngredientRequest;

public final class MachineRecipeTransfer {
    private MachineRecipeTransfer() {
    }

    public static boolean isSupported(EmiRecipe recipe) {
        return !VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory())
                && recipe.supportsRecipeTree()
                && !getItemIngredients(recipe).isEmpty();
    }

    public static Optional<List<TerminalIngredientRequest.ItemRequirement>> createRequest(
            EmiRecipe recipe, EmiPlayerInventory inventory, ItemStack carried) {
        if (VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory()) || !recipe.supportsRecipeTree()) {
            return Optional.empty();
        }

        List<EmiIngredient> itemIngredients = getItemIngredients(recipe);
        if (itemIngredients.isEmpty()) {
            return Optional.empty();
        }
        var alternatives = new ArrayList<List<MachineIngredientSelector.Alternative<EmiStack>>>();
        for (var ingredient : itemIngredients) {
            long requiredAmount = Math.max(1, ingredient.getAmount());
            var itemAlternatives = ingredient.getEmiStacks().stream()
                    .filter(stack -> !stack.getItemStack().isEmpty())
                    .map(stack -> new MachineIngredientSelector.Alternative<>(stack, requiredAmount))
                    .toList();
            alternatives.add(itemAlternatives);
        }

        return MachineIngredientSelector.select(
                        alternatives,
                        stack -> {
                            EmiStack available = inventory.inventory.get(stack);
                            long amount = available == null ? 0 : available.getAmount();
                            AEItemKey key = AEItemKey.of(stack.getItemStack());
                            if (key != null && key.matches(carried)) {
                                amount += carried.getCount();
                            }
                            return amount;
                        },
                        TerminalIngredientRequest.MAX_TOTAL_ITEMS)
                .map(selections -> selections.stream()
                        .map(selection -> new TerminalIngredientRequest.ItemRequirement(
                                AEItemKey.of(selection.key().getItemStack()), selection.amount()))
                        .toList())
                .filter(requirements -> requirements.size() <= TerminalIngredientRequest.MAX_REQUIREMENTS);
    }

    private static List<EmiIngredient> getItemIngredients(EmiRecipe recipe) {
        var result = new ArrayList<EmiIngredient>();
        addItemIngredients(result, recipe.getInputs());
        addItemIngredients(result, recipe.getCatalysts());
        return result;
    }

    private static void addItemIngredients(List<EmiIngredient> result, List<EmiIngredient> ingredients) {
        for (var ingredient : ingredients) {
            if (!ingredient.isEmpty()
                    && ingredient.getEmiStacks().stream().anyMatch(stack -> !stack.getItemStack().isEmpty())) {
                result.add(ingredient);
            }
        }
    }
}
