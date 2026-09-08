package org.blocovermelho.ae2emi.network;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.ToIntFunction;

/** Validates batch arithmetic and sizes complete transfers without touching live inventories. */
final class MachineTransferSizing {
    static final int MAX_REQUIREMENTS = 32;
    static final int MAX_TOTAL_ITEMS = 36 * 64;

    record Requirement<K>(K key, long perBatch, long catalyst) {
        long amount(int batches) {
            return perBatch * batches + catalyst;
        }
    }

    /** A null key denotes an empty slot; limit is the item's effective stack limit. */
    record Slot<K>(K key, int count, int limit) {
    }

    private MachineTransferSizing() {
    }

    static <K> List<Requirement<K>> validateAndCombine(List<Requirement<K>> requested, int batches) {
        if (batches < 1 || batches > MAX_TOTAL_ITEMS
                || requested.isEmpty() || requested.size() > MAX_REQUIREMENTS) {
            throw new IllegalArgumentException("Invalid machine transfer size");
        }
        var combined = new LinkedHashMap<K, Requirement<K>>();
        long total = 0;
        for (var requirement : requested) {
            if (requirement == null || requirement.key() == null
                    || requirement.perBatch() < 0 || requirement.catalyst() < 0
                    || requirement.catalyst() > MAX_TOTAL_ITEMS - total
                    || requirement.perBatch() > (MAX_TOTAL_ITEMS - total - requirement.catalyst()) / batches
                    || (requirement.perBatch() == 0 && requirement.catalyst() == 0)) {
                throw new IllegalArgumentException("Invalid machine transfer amount");
            }
            total += requirement.amount(batches);
            combined.merge(requirement.key(), requirement, (a, b) ->
                    new Requirement<>(a.key(), a.perBatch() + b.perBatch(), a.catalyst() + b.catalyst()));
        }
        return List.copyOf(combined.values());
    }

    /** Requirements must have passed validateAndCombine. Inventory slots are never mutated. */
    static <K> int maximumFitting(List<Requirement<K>> requirements, int requestedBatches,
            List<Slot<K>> inventory, ToIntFunction<K> stackLimit) {
        var counts = new HashMap<K, Long>();
        var room = new HashMap<K, Long>();
        int emptySlots = 0;
        for (var slot : inventory) {
            if (slot.key() == null || slot.count() == 0) {
                emptySlots++;
            } else {
                counts.merge(slot.key(), (long) slot.count(), Long::sum);
                room.merge(slot.key(), (long) Math.max(0, slot.limit() - slot.count()), Long::sum);
            }
        }
        var limits = new HashMap<K, Integer>();
        for (var requirement : requirements) {
            limits.put(requirement.key(), Math.max(1, stackLimit.applyAsInt(requirement.key())));
        }

        int low = 0;
        int high = requestedBatches;
        while (low < high) {
            int batches = low + (high - low + 1) / 2;
            long slotsNeeded = 0;
            for (var requirement : requirements) {
                long incoming = Math.max(0, requirement.amount(batches)
                        - counts.getOrDefault(requirement.key(), 0L));
                long needsEmptySlot = Math.max(0, incoming - room.getOrDefault(requirement.key(), 0L));
                int limit = limits.get(requirement.key());
                slotsNeeded += (needsEmptySlot + limit - 1) / limit;
            }
            if (slotsNeeded <= emptySlots) {
                low = batches;
            } else {
                high = batches - 1;
            }
        }
        return low;
    }
}
