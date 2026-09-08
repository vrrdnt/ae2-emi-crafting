package org.blocovermelho.ae2emi.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

import appeng.api.stacks.AEItemKey;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import org.blocovermelho.ae2emi.network.TerminalIngredientRequest;

public final class MachineRecipeTransfer {
    private static final ResourceLocation PROGRAMMED_CIRCUIT =
            ResourceLocation.fromNamespaceAndPath("gtceu", "programmed_circuit");
    public record Plan(int batches, List<TerminalIngredientRequest.ItemRequirement> requirements,
            List<MachineIngredientSelector.Selection<EmiStack>> missing) {
    }
    private MachineRecipeTransfer() {
    }

    public static boolean isSupported(EmiRecipe recipe) {
        return !VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory())
                && recipe.supportsRecipeTree();
    }

    public static Plan createRequest(
            EmiRecipe recipe, EmiPlayerInventory inventory, int requestedBatches) {
        if (!isSupported(recipe) || requestedBatches <= 0) {
            return new Plan(0, List.of(), List.of());
        }

        var ingredients = new ArrayList<MachineIngredientSelector.Ingredient<EmiStack>>();
        addItemIngredients(ingredients, recipe.getInputs(), false);
        addItemIngredients(ingredients, recipe.getCatalysts(), true);

        ToLongFunction<EmiStack> available = stack -> {
            EmiStack stored = inventory.inventory.get(stack);
            return stored == null ? 0 : stored.getAmount();
        };
        var selected = MachineIngredientSelector.selectMaximum(
                        ingredients, available,
                        requestedBatches,
                        TerminalIngredientRequest.MAX_TOTAL_ITEMS)
                .filter(batchSelection -> batchSelection.selections().size() <= TerminalIngredientRequest.MAX_REQUIREMENTS);
        var requirements = selected.map(batchSelection -> batchSelection.selections().stream()
                        .map(selection -> new TerminalIngredientRequest.ItemRequirement(
                                AEItemKey.of(selection.key().getItemStack()),
                                (selection.amount() - selection.catalystAmount()) / batchSelection.batches(),
                                selection.catalystAmount()))
                        .toList())
                .orElse(List.of());
        int batches = selected.map(MachineIngredientSelector.BatchSelection::batches).orElse(0);
        int target = requestedBatches == Integer.MAX_VALUE ? batches + 1 : requestedBatches;
        var missing = batches < target ? MachineIngredientSelector.shortfall(ingredients, available, target) : List.<MachineIngredientSelector.Selection<EmiStack>>of();
        return new Plan(batches, requirements, missing);
    }

    public static void reportShortfall(Plan plan, int requestedBatches) {
        var player = Minecraft.getInstance().player;
        if (player == null || plan.missing().isEmpty()) {
            return;
        }
        var items = Component.empty();
        for (var missing : plan.missing()) {
            if (!items.getSiblings().isEmpty()) {
                items.append(", ");
            }
            items.append(Component.literal(missing.amount() + " × "))
                    .append(missing.key().getItemStack().getHoverName());
        }
        var target = requestedBatches == Integer.MAX_VALUE
                ? Component.translatable("message.ae2emi.machine_transfer.more")
                : Component.literal(Integer.toString(requestedBatches));
        player.displayClientMessage(Component.translatable("message.ae2emi.machine_transfer.shortfall",
                plan.batches(), target, items), false);
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
                var item = stack.getItemStack();
                // GregTech's programmed circuit is a ghost machine setting, not a physical catalyst to collect.
                if (catalyst && PROGRAMMED_CIRCUIT.equals(ForgeRegistries.ITEMS.getKey(item.getItem()))) {
                    continue;
                }
                if (!item.isEmpty()) {
                    alternatives.add(new MachineIngredientSelector.Alternative<>(stack, requiredAmount));
                }
            }
            if (!alternatives.isEmpty()) {
                result.add(new MachineIngredientSelector.Ingredient<>(alternatives, catalyst));
            }
        }
    }
}
