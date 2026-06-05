package org.blocovermelho.ae2emi.platform.facade;

import appeng.core.definitions.AEBlocks;
import appeng.recipes.handlers.ChargerRecipe;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import org.blocovermelho.ae2emi.platform.facade.crafting.recipe.Ae2ChargerRecipe;
import org.blocovermelho.ae2emi.platform.facade.crafting.recipe.RecipeUtil;
import org.blocovermelho.ae2emi.platform.facade.crafting.recipe.category.Ae2Categories;

public class Ae2EmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(Ae2Categories.ATTUNEMENT);
        RecipeUtil.registerAttunement(registry);

        registry.addCategory(Ae2Categories.CHARGER);
        RecipeUtil.registerAllByType(registry, ChargerRecipe.TYPE, Ae2ChargerRecipe::new);
        registry.addWorkstation(Ae2Categories.CHARGER, EmiStack.of(AEBlocks.CHARGER));
    }
}
