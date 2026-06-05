package org.blocovermelho.ae2emi.platform.facade.recipe;

import appeng.api.config.CondenserOutput;
import appeng.api.features.P2PTunnelAttunementInternal;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.localization.ItemModText;
import appeng.recipes.entropy.EntropyRecipe;
import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.recipes.transform.TransformRecipe;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.blocovermelho.ae2emi.platform.facade.Ae2EmiMod;
import org.blocovermelho.ae2emi.platform.facade.recipe.category.Ae2Categories;

import java.util.List;
import java.util.function.Function;

public class RecipeUtil {
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends Recipe<?>> void registerAllByType(EmiRegistry registry, RecipeType type, Function<T, EmiRecipe> mapper) {
        List<T> recipes = (List<T>) registry.getRecipeManager().getAllRecipesFor(type);
        for (T recipe : recipes) {
            registry.addRecipe(mapper.apply(recipe));
        }
        Ae2EmiMod.LOGGER.info("Registered {} recipes for {}", recipes.size(), type.getClass().getName());
    }

    private static void registerAttunement(EmiRegistry registry) {
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

    public static void registerRecipes(EmiRegistry registry) {
        registry.addCategory(Ae2Categories.ATTUNEMENT);
        RecipeUtil.registerAttunement(registry);

        registry.addCategory(Ae2Categories.CHARGER);
        RecipeUtil.registerAllByType(registry, ChargerRecipe.TYPE, Ae2ChargerRecipe::new);
        registry.addWorkstation(Ae2Categories.CHARGER, EmiStack.of(AEBlocks.CHARGER));

        registry.addCategory(Ae2Categories.CONDENSER);
        registry.addWorkstation(Ae2Categories.CONDENSER, EmiStack.of(AEBlocks.CONDENSER));
        registry.addRecipe(new Ae2CondenserRecipe(CondenserOutput.MATTER_BALLS));
        registry.addRecipe(new Ae2CondenserRecipe(CondenserOutput.SINGULARITY));

        registry.addCategory(Ae2Categories.ENTROPY);
        registry.addWorkstation(Ae2Categories.ENTROPY, EmiStack.of(AEItems.ENTROPY_MANIPULATOR));
        RecipeUtil.registerAllByType(registry, EntropyRecipe.TYPE, Ae2EntropyManipulatorRecipe::new);

        registry.addCategory(Ae2Categories.INSCRIBER);
        RecipeUtil.registerAllByType(registry, InscriberRecipe.TYPE, Ae2InscriberRecipe::new);
        registry.addWorkstation(Ae2Categories.INSCRIBER, EmiStack.of(AEBlocks.INSCRIBER));

        registry.addCategory(Ae2Categories.WORLD_INTERACTION);
        RecipeUtil.registerAllByType(registry, TransformRecipe.TYPE, Ae2TransformRecipe::new);
    }
}
