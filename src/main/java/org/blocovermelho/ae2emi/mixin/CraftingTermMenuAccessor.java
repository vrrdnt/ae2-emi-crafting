package org.blocovermelho.ae2emi.mixin;

import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.CraftingMatrixSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CraftingTermMenu.class)
public interface CraftingTermMenuAccessor {
    @Accessor("craftingSlots")
    CraftingMatrixSlot[] ae2emi$getCraftingSlots();
    @Invoker("isCraftable")
    boolean ae2emi$isCraftable(ItemStack stack);
}
