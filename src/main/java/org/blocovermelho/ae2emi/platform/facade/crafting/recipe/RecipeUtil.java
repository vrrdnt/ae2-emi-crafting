package org.blocovermelho.ae2emi.platform.facade.crafting.recipe;

import appeng.api.features.P2PTunnelAttunementInternal;
import appeng.core.localization.ItemModText;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

public class RecipeUtil {
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
