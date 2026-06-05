package org.blocovermelho.ae2emi.platform.facade;

import appeng.menu.me.items.CraftingTermMenu;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import org.blocovermelho.ae2emi.platform.facade.recipe.RecipeUtil;
import org.blocovermelho.ae2emi.platform.facade.recipe.handler.CraftLikeRecipeHandler;

public class Ae2EmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        if (Ae2EmiMod.BASE_CONFIG.compat.disableExMIAe2Integration) {
            RecipeUtil.registerRecipes(registry);
        }

        registry.addRecipeHandler(CraftingTermMenu.TYPE, new CraftLikeRecipeHandler());
    }
}
