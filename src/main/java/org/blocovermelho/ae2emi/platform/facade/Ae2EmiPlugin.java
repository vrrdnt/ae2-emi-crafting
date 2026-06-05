package org.blocovermelho.ae2emi.platform.facade;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import org.blocovermelho.ae2emi.platform.facade.crafting.recipe.RecipeUtil;
import org.blocovermelho.ae2emi.platform.facade.crafting.recipe.category.Ae2Categories;

public class Ae2EmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(Ae2Categories.ATTUNEMENT);
        RecipeUtil.registerAttunement(registry);
    }
}
