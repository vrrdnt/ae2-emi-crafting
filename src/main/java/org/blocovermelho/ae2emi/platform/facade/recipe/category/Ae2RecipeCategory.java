package org.blocovermelho.ae2emi.platform.facade.recipe.category;

import appeng.core.localization.LocalizationEnum;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class Ae2RecipeCategory extends EmiRecipeCategory {
    private final MutableComponent localizedName;
    public Ae2RecipeCategory(ResourceLocation id, EmiRenderable icon, @NotNull LocalizationEnum locale) {
        super(id, icon);
        localizedName = locale.text();
    }

    @Override
    public Component getName() {
        return localizedName;
    }
}
