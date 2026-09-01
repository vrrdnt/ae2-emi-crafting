package org.blocovermelho.ae2emi.network;

import java.util.function.Supplier;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.helpers.InventoryAction;
import appeng.items.storage.ViewCellItem;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.CraftingTermSlot;
import appeng.util.inv.PlayerInternalInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record TerminalCraftRequest(int menuId, Destination destination, int amount) {
    private static final int MAX_BATCHES_PER_ACTION = 64;

    public enum Destination {
        NONE,
        CURSOR,
        INVENTORY
    }

    static void encode(TerminalCraftRequest request, FriendlyByteBuf buffer) {
        buffer.writeVarInt(request.menuId);
        buffer.writeEnum(request.destination);
        buffer.writeVarInt(request.amount);
    }

    static TerminalCraftRequest decode(FriendlyByteBuf buffer) {
        return new TerminalCraftRequest(buffer.readVarInt(), buffer.readEnum(Destination.class), buffer.readVarInt());
    }

    static void handle(TerminalCraftRequest request, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.setPacketHandled(true);

        ServerPlayer player = context.getSender();
        if (player == null || !(player.containerMenu instanceof CraftingTermMenu menu)) {
            return;
        }
        if (request.menuId != menu.containerId || !menu.stillValid(player) || !isValidAmount(request.amount)) {
            return;
        }

        int batches = request.amount == Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : Math.min(request.amount, MAX_BATCHES_PER_ACTION);

        if (request.destination == Destination.NONE) {
            fillGrid(menu, player, batches);
        } else {
            craftOutput(menu, player, request.destination, batches);
        }
        menu.broadcastChanges();
    }

    private static boolean isValidAmount(int amount) {
        return amount > 0;
    }

    private static void fillGrid(CraftingTermMenu menu, ServerPlayer player, int batches) {
        var node = menu.getNetworkNode();
        if (node == null || node.getGrid() == null) {
            return;
        }

        var grid = node.getGrid();
        var storage = grid.getStorageService().getInventory();
        var energy = grid.getEnergyService();
        var viewCellFilter = ViewCellItem.createItemFilter(menu.getViewCells());
        InternalInventory matrix = menu.getCraftingMatrix();
        boolean changed = false;

        for (int slot = 0; slot < matrix.size(); slot++) {
            ItemStack stack = matrix.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int target = batches == Integer.MAX_VALUE
                    ? stack.getMaxStackSize()
                    : Math.min(batches, stack.getMaxStackSize());
            int needed = target - stack.getCount();
            if (needed <= 0) {
                continue;
            }

            AEItemKey key = AEItemKey.of(stack);
            if (key != null && (viewCellFilter == null || viewCellFilter.isListed(key))) {
                long extracted = StorageHelper.poweredExtraction(
                        energy, storage, key, needed, menu.getActionSource());
                int extractedItems = (int) Math.min(extracted, needed);
                stack.grow(extractedItems);
                needed -= extractedItems;
            }

            if (needed > 0) {
                takeMatchingItemsFromPlayer(menu, player, stack, needed);
            }

            matrix.setItemDirect(slot, stack);
            changed = true;
        }

        if (changed) {
            player.getInventory().setChanged();
            menu.slotsChanged(matrix.toContainer());
        }
    }

    private static void takeMatchingItemsFromPlayer(
            CraftingTermMenu menu, ServerPlayer player, ItemStack target, int requested) {
        var playerInventory = player.getInventory();
        int remaining = requested;

        for (int slot = 0; slot < playerInventory.items.size() && remaining > 0; slot++) {
            if (menu.isPlayerInventorySlotLocked(slot)) {
                continue;
            }

            ItemStack source = playerInventory.items.get(slot);
            if (!ItemStack.isSameItemSameTags(source, target)) {
                continue;
            }

            int moved = Math.min(source.getCount(), remaining);
            source.shrink(moved);
            target.grow(moved);
            remaining -= moved;
        }
    }

    private static void craftOutput(
            CraftingTermMenu menu, ServerPlayer player, Destination destination, int batches) {
        CraftingTermSlot output = findOutputSlot(menu);
        if (output == null || output.getItem().isEmpty()) {
            return;
        }

        if (batches == Integer.MAX_VALUE) {
            InventoryAction action = destination == Destination.CURSOR
                    ? InventoryAction.CRAFT_STACK
                    : InventoryAction.CRAFT_SHIFT;
            output.doClick(action, player);
            return;
        }

        if (destination == Destination.CURSOR) {
            craftToCursor(output, menu, player, batches);
        } else {
            craftToInventory(output, menu, player, batches);
        }
    }

    private static CraftingTermSlot findOutputSlot(CraftingTermMenu menu) {
        for (var slot : menu.getSlots(SlotSemantics.CRAFTING_RESULT)) {
            if (slot instanceof CraftingTermSlot output) {
                return output;
            }
        }
        return null;
    }

    private static void craftToCursor(
            CraftingTermSlot output, CraftingTermMenu menu, ServerPlayer player, int batches) {
        for (int batch = 0; batch < batches; batch++) {
            ItemStack before = menu.getCarried().copy();
            output.doClick(InventoryAction.CRAFT_ITEM, player);
            if (ItemStack.matches(before, menu.getCarried())) {
                return;
            }
        }
    }

    private static void craftToInventory(
            CraftingTermSlot output, CraftingTermMenu menu, ServerPlayer player, int batches) {
        var playerInventory = new PlayerInternalInventory(player.getInventory());

        for (int batch = 0; batch < batches; batch++) {
            ItemStack expected = output.getItem().copy();
            if (expected.isEmpty() || !playerInventory.simulateAdd(expected).isEmpty()) {
                return;
            }

            ItemStack originalCursor = menu.getCarried().copy();
            menu.setCarried(ItemStack.EMPTY);
            output.doClick(InventoryAction.CRAFT_ITEM, player);
            ItemStack crafted = menu.getCarried().copy();
            menu.setCarried(originalCursor);

            if (crafted.isEmpty()) {
                return;
            }

            ItemStack remainder = playerInventory.addItems(crafted);
            if (!remainder.isEmpty()) {
                player.getInventory().placeItemBackInInventory(remainder);
                return;
            }
        }
    }
}
