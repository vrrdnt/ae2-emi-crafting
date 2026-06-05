package org.blocovermelho.ae2emi.platform.facade.recipe;

import appeng.menu.me.items.CraftingTermMenu;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.stack.EmiStack;
import org.blocovermelho.ae2emi.mixin.CraftingTermMenuAccessor;

public class TransferUtil {
    public static boolean checkPlayerInventoryContains(EmiPlayerInventory inventory, EmiStack stack) {
      return inventory.inventory.containsKey(stack);
    }

    public static boolean checkTerminalInventoryContains(CraftingTermMenu menu, EmiStack stack) {
        return menu.hasItemType(stack.getItemStack(), (int) stack.getAmount());
    }

    public static boolean checkTerminalPatternsContains(CraftingTermMenu menu, EmiStack stack) {
        return ((CraftingTermMenuAccessor) menu).ae2emi$isCraftable(stack.getItemStack());
    }
}
