package org.blocovermelho.ae2emi.platform.facade;

import appeng.api.config.CondenserOutput;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.recipes.entropy.EntropyRecipe;
import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.recipes.transform.TransformRecipe;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import org.blocovermelho.ae2emi.platform.facade.crafting.recipe.*;
import org.blocovermelho.ae2emi.platform.facade.crafting.recipe.category.Ae2Categories;

public class Ae2EmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
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
