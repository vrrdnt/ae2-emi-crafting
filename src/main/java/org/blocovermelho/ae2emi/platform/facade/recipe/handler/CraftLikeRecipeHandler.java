package org.blocovermelho.ae2emi.platform.facade.recipe.handler;

import appeng.integration.modules.jeirei.CraftingHelper;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.CraftingTermMenu;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.blocovermelho.ae2emi.platform.facade.Ae2EmiMod;
import org.blocovermelho.ae2emi.platform.facade.recipe.TransferUtil;

import java.util.List;

/**
 * EMI Recipe handler for screens that are like {@link appeng.client.gui.me.items.CraftingTermScreen}.</br>
 * Examples include: {@link appeng.menu.me.items.CraftingTermMenu} and {@link appeng.menu.me.items.WirelessCraftingTermMenu}.
 */
public class CraftLikeRecipeHandler<C extends CraftingTermMenu> implements StandardRecipeHandler<C> {
    @Override
    public List<Slot> getInputSources(C menu) {
        /* Problem with giving out all the ME system's slots as input sources is that since ME systems update their
        * contents frequently as the gameplay progresses, EMI will notice those changes and dispatch recalculations
        * for the "craftables" section. The best way to handle this is to lie to EMI saying that "no sources" exist
        * and handle "pulling" items to the ME system using the AE2 APIs.
        */
        var slots = menu.getSlots(SlotSemantics.PLAYER_INVENTORY);
        slots.addAll(menu.getSlots(SlotSemantics.PLAYER_HOTBAR));

        return slots;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<C> context) {
        /* This is where the second part of the hack lives. We will ask for AE2 to do all the craftability evaluation
        * since it will be done server-side and if items can be pulled we handle them accourdingly.
        */

        C menu = context.getScreenHandler();
        EmiPlayerInventory playerInventory = context.getInventory();

        for (var ingredient: recipe.getInputs()) {
            if (ingredient.isEmpty()) { continue; }

            //FIXME: Accumulate the actual results in a way where we know from which "inventory" said item came from.
            //FIXME: Actually count all of the required resources to see if the user has the required items to craft it.
            boolean found = ingredient.getEmiStacks().stream()
                    .anyMatch(x -> TransferUtil.checkPlayerInventoryContains(playerInventory, x)
                            || TransferUtil.checkTerminalInventoryContains(menu, x)
                            || TransferUtil.checkTerminalPatternsContains(menu, x));

            if (!found) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<C> context) {
        //HACK: This is levels of jank that are never before seen.
        //HACK: I *really* despise having to do this, but it seems to be the only way.
        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            Ae2EmiMod.LOGGER.warn("Could not get ClientLevel for obtaining the vanilla recipe.");
            return false;
        }

        C menu = context.getScreenHandler();

        var maybeRecipe = level.getRecipeManager().byKey(recipe.getId());

        if (maybeRecipe.isPresent()) {
            CraftingHelper.performTransfer(menu, maybeRecipe.get(), true);
            forceCloseRecipeScreen();
            return true;
        }

        return false;
    }

    @Override
    public List<Slot> getCraftingSlots(C menu) {
        return menu.getSlots(SlotSemantics.CRAFTING_GRID);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING;
    }

    private void forceCloseRecipeScreen() {
        var screen = Minecraft.getInstance().screen;
        if (screen != null && screen.getTitle().equals(Component.translatable("screen.emi.recipe"))) {
            screen.onClose();
        }
    }
}
