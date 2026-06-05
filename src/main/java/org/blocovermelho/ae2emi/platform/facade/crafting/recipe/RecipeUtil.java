package org.blocovermelho.ae2emi.platform.facade.crafting.recipe;

import appeng.api.features.P2PTunnelAttunementInternal;
import appeng.core.localization.ItemModText;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.blocovermelho.ae2emi.platform.facade.Ae2EmiMod;

import java.util.List;
import java.util.function.Function;

public class RecipeUtil {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends Recipe<?>> void registerAllByType(EmiRegistry registry, RecipeType type, Function<T, EmiRecipe> mapper) {
        List<T> recipes = (List<T>) registry.getRecipeManager().getAllRecipesFor(type);
        for (T recipe : recipes) {
            registry.addRecipe(mapper.apply(recipe));
        }
        Ae2EmiMod.LOGGER.info("Registered {} recipes for {}", recipes.size(), type.getClass().getName());
    }

    public static void registerAttunement(EmiRegistry registry) {
        for (var entry : P2PTunnelAttunementInternal.getApiTunnels()) {
            var inputs = EmiApi.getIndexStacks().stream().filter(x -> entry.stackPredicate().test(x.getItemStack())).toList();
            if (inputs.isEmpty()) {
                continue;
            }
            registry.addRecipe(new Ae2AttunementRecipe(EmiIngredient.of(inputs), EmiStack.of(entry.tunnelType()), ItemModText.P2P_API_ATTUNEMENT.text()));
        }

        for (var entry : P2PTunnelAttunementInternal.getTagTunnels().entrySet()) {
            registry.addRecipe(new Ae2AttunementRecipe(EmiIngredient.of(entry.getKey()), EmiStack.of(entry.getValue()), ItemModText.P2P_TAG_ATTUNEMENT.text()));
        }
    }
}
