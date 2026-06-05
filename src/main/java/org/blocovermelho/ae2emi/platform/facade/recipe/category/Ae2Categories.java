package org.blocovermelho.ae2emi.platform.facade.recipe.category;

import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.recipes.entropy.EntropyRecipe;
import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.recipes.transform.TransformRecipe;
import dev.emi.emi.api.stack.EmiStack;

public class Ae2Categories {
    public static Ae2RecipeCategory WORLD_INTERACTION = new Ae2RecipeCategory(TransformRecipe.TYPE_ID, EmiStack.of(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED), TranslationKeys.TRANSFORM);
    public static Ae2RecipeCategory INSCRIBER = new Ae2RecipeCategory(InscriberRecipe.TYPE_ID, EmiStack.of(AEBlocks.INSCRIBER), TranslationKeys.INSCRIBER);
    public static Ae2RecipeCategory CHARGER = new Ae2RecipeCategory(ChargerRecipe.TYPE_ID, EmiStack.of(AEBlocks.CHARGER), TranslationKeys.CHARGER);
    public static Ae2RecipeCategory ATTUNEMENT = new Ae2RecipeCategory(AppEng.makeId("attunement"), EmiStack.of(AEParts.ME_P2P_TUNNEL), TranslationKeys.ATTUNEMENT);
    public static Ae2RecipeCategory CONDENSER = new Ae2RecipeCategory(AppEng.makeId("condenser"), EmiStack.of(AEBlocks.CONDENSER), TranslationKeys.CONDENSER);
    public static Ae2RecipeCategory ENTROPY = new Ae2RecipeCategory(EntropyRecipe.TYPE_ID, EmiStack.of(AEItems.ENTROPY_MANIPULATOR), TranslationKeys.ENTROPY_MANIPULATOR);
}
