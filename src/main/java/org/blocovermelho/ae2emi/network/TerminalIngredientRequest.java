package org.blocovermelho.ae2emi.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.items.storage.ViewCellItem;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record TerminalIngredientRequest(int menuId, List<ItemRequirement> requirements) {
    public static final int MAX_REQUIREMENTS = 32;
    public static final long MAX_TOTAL_ITEMS = 36L * 64;

    public record ItemRequirement(AEItemKey what, long amount) {
        public ItemRequirement {
            Objects.requireNonNull(what, "what");
        }
    }

    private record Requirement(AEItemKey what, int amount) {
    }

    private record TransferPlan(Requirement requirement, int fromMatrix, int fromCursor, int fromNetwork) {
        int incoming() {
            return fromMatrix + fromCursor + fromNetwork;
        }
    }

    private record Withdrawal(AEItemKey what, int amount) {
    }

    public TerminalIngredientRequest {
        requirements = List.copyOf(requirements);
    }

    static void encode(TerminalIngredientRequest request, FriendlyByteBuf buffer) {
        buffer.writeVarInt(request.menuId);
        buffer.writeVarInt(request.requirements.size());
        for (var requirement : request.requirements) {
            requirement.what.writeToPacket(buffer);
            buffer.writeVarLong(requirement.amount);
        }
    }

    static TerminalIngredientRequest decode(FriendlyByteBuf buffer) {
        int menuId = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 1 || size > MAX_REQUIREMENTS) {
            throw new IllegalArgumentException("Invalid machine recipe ingredient count: " + size);
        }

        var requirements = new ArrayList<ItemRequirement>(size);
        for (int i = 0; i < size; i++) {
            requirements.add(new ItemRequirement(AEItemKey.fromPacket(buffer), buffer.readVarLong()));
        }
        return new TerminalIngredientRequest(menuId, requirements);
    }

    static void handle(TerminalIngredientRequest request, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.setPacketHandled(true);

        ServerPlayer player = context.getSender();
        if (player == null || !(player.containerMenu instanceof CraftingTermMenu menu)) {
            return;
        }
        if (request.menuId != menu.containerId || !menu.stillValid(player)) {
            return;
        }

        List<Requirement> requirements = validateAndCombine(request.requirements);
        if (requirements == null) {
            return;
        }

        var node = menu.getNetworkNode();
        if (node == null || node.getGrid() == null) {
            show(player, "message.ae2emi.machine_transfer.missing");
            return;
        }

        var grid = node.getGrid();
        var storage = grid.getStorageService().getInventory();
        var energy = grid.getEnergyService();
        List<TransferPlan> plans = plan(menu, player, requirements, storage, energy);
        if (plans == null) {
            show(player, "message.ae2emi.machine_transfer.missing");
            return;
        }
        if (!hasInventorySpace(player, plans)) {
            show(player, "message.ae2emi.machine_transfer.full");
            return;
        }

        int incoming = plans.stream().mapToInt(TransferPlan::incoming).sum();
        if (incoming == 0) {
            show(player, "message.ae2emi.machine_transfer.already_present");
            return;
        }
        if (!extractFromNetwork(menu, player, plans, storage, energy)) {
            show(player, "message.ae2emi.machine_transfer.missing");
            return;
        }

        InternalInventory matrix = menu.getCraftingMatrix();
        boolean matrixChanged = false;
        for (var transfer : plans) {
            if (transfer.fromMatrix > 0) {
                removeMatching(matrix, transfer.requirement.what, transfer.fromMatrix);
                matrixChanged = true;
            }
            if (transfer.fromCursor > 0) {
                ItemStack carried = menu.getCarried().copy();
                carried.shrink(transfer.fromCursor);
                menu.setCarried(carried);
            }
        }

        boolean deliveredToInventory = deliverIncoming(player, plans);

        player.getInventory().setChanged();
        if (matrixChanged) {
            menu.slotsChanged(matrix.toContainer());
        }
        menu.broadcastChanges();
        show(player, deliveredToInventory
                ? "message.ae2emi.machine_transfer.success"
                : "message.ae2emi.machine_transfer.full");
    }

    private static List<Requirement> validateAndCombine(List<ItemRequirement> requested) {
        if (requested.isEmpty() || requested.size() > MAX_REQUIREMENTS) {
            return null;
        }

        var combined = new LinkedHashMap<AEItemKey, Long>();
        long total = 0;
        for (var requirement : requested) {
            if (requirement == null || requirement.what == null || requirement.amount <= 0
                    || requirement.amount > MAX_TOTAL_ITEMS - total) {
                return null;
            }
            total += requirement.amount;
            combined.merge(requirement.what, requirement.amount, Long::sum);
        }

        var result = new ArrayList<Requirement>(combined.size());
        for (Map.Entry<AEItemKey, Long> entry : combined.entrySet()) {
            result.add(new Requirement(entry.getKey(), Math.toIntExact(entry.getValue())));
        }
        return result;
    }

    private static List<TransferPlan> plan(
            CraftingTermMenu menu,
            ServerPlayer player,
            List<Requirement> requirements,
            MEStorage storage,
            IEnergySource energy) {
        InternalInventory matrix = menu.getCraftingMatrix();
        ItemStack cursor = menu.getCarried();
        var viewCellFilter = ViewCellItem.createItemFilter(menu.getViewCells());
        var plans = new ArrayList<TransferPlan>(requirements.size());

        for (var requirement : requirements) {
            int remaining = requirement.amount - Math.min(
                    requirement.amount,
                    countMatching(player.getInventory().items, requirement.what));
            int fromMatrix = Math.min(remaining, countMatching(matrix, requirement.what));
            remaining -= fromMatrix;
            int fromCursor = Math.min(remaining, requirement.what.matches(cursor) ? cursor.getCount() : 0);
            remaining -= fromCursor;
            int fromNetwork = remaining;

            if (fromNetwork > 0) {
                if (viewCellFilter != null && !viewCellFilter.isListed(requirement.what)) {
                    return null;
                }
                long available = StorageHelper.poweredExtraction(
                        energy,
                        storage,
                        requirement.what,
                        fromNetwork,
                        menu.getActionSource(),
                        Actionable.SIMULATE);
                if (available < fromNetwork) {
                    return null;
                }
            }
            plans.add(new TransferPlan(requirement, fromMatrix, fromCursor, fromNetwork));
        }
        return plans;
    }

    private static boolean hasInventorySpace(ServerPlayer player, List<TransferPlan> plans) {
        var simulated = new AppEngInternalInventory(player.getInventory().items.size());
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            simulated.setItemDirect(slot, player.getInventory().items.get(slot).copy());
        }
        return addIncoming(simulated, plans);
    }

    private static boolean extractFromNetwork(
            CraftingTermMenu menu,
            ServerPlayer player,
            List<TransferPlan> plans,
            MEStorage storage,
            IEnergySource energy) {
        var extracted = new ArrayList<Withdrawal>();
        for (var transfer : plans) {
            if (transfer.fromNetwork == 0) {
                continue;
            }
            int amount = (int) StorageHelper.poweredExtraction(
                    energy,
                    storage,
                    transfer.requirement.what,
                    transfer.fromNetwork,
                    menu.getActionSource(),
                    Actionable.MODULATE);
            if (amount > 0) {
                extracted.add(new Withdrawal(transfer.requirement.what, amount));
            }
            if (amount != transfer.fromNetwork) {
                restoreNetworkWithdrawals(menu, player, storage, extracted);
                return false;
            }
        }
        return true;
    }

    private static void restoreNetworkWithdrawals(
            CraftingTermMenu menu,
            ServerPlayer player,
            MEStorage storage,
            List<Withdrawal> withdrawals) {
        for (var withdrawal : withdrawals) {
            long restored = storage.insert(
                    withdrawal.what,
                    withdrawal.amount,
                    Actionable.MODULATE,
                    menu.getActionSource());
            giveToPlayer(player, withdrawal.what, withdrawal.amount - restored);
        }
    }

    private static int countMatching(Iterable<ItemStack> stacks, AEItemKey what) {
        int count = 0;
        for (ItemStack stack : stacks) {
            if (what.matches(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeMatching(InternalInventory inventory, AEItemKey what, int requested) {
        int remaining = requested;
        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!what.matches(stack)) {
                continue;
            }
            ItemStack updated = stack.copy();
            int moved = Math.min(updated.getCount(), remaining);
            updated.shrink(moved);
            inventory.setItemDirect(slot, updated);
            remaining -= moved;
        }
        if (remaining != 0) {
            throw new IllegalStateException("Machine recipe source changed during transfer");
        }
    }

    private static boolean addIncoming(InternalInventory inventory, List<TransferPlan> plans) {
        for (var transfer : plans) {
            int remaining = transfer.incoming();
            int stackLimit = Math.max(1, transfer.requirement.what.getMaxStackSize());
            while (remaining > 0) {
                int amount = Math.min(remaining, stackLimit);
                if (!inventory.addItems(transfer.requirement.what.toStack(amount)).isEmpty()) {
                    return false;
                }
                remaining -= amount;
            }
        }
        return true;
    }

    private static boolean deliverIncoming(ServerPlayer player, List<TransferPlan> plans) {
        var inventory = new PlayerInternalInventory(player.getInventory());
        boolean allInInventory = true;
        for (var transfer : plans) {
            int remaining = transfer.incoming();
            int stackLimit = Math.max(1, transfer.requirement.what.getMaxStackSize());
            while (remaining > 0) {
                int amount = Math.min(remaining, stackLimit);
                ItemStack remainder = inventory.addItems(transfer.requirement.what.toStack(amount));
                if (!remainder.isEmpty()) {
                    allInInventory = false;
                    player.getInventory().placeItemBackInInventory(remainder);
                }
                remaining -= amount;
            }
        }
        return allInInventory;
    }

    private static void giveToPlayer(ServerPlayer player, AEItemKey what, long amount) {
        int stackLimit = Math.max(1, what.getMaxStackSize());
        while (amount > 0) {
            int stackSize = (int) Math.min(amount, stackLimit);
            player.getInventory().placeItemBackInInventory(what.toStack(stackSize));
            amount -= stackSize;
        }
    }

    private static void show(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }
}
