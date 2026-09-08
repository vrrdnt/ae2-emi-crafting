package org.blocovermelho.ae2emi.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MachineTransferSizingTest {
    private static MachineTransferSizing.Requirement<String> need(String key, long perBatch, long catalyst) {
        return new MachineTransferSizing.Requirement<>(key, perBatch, catalyst);
    }

    private static MachineTransferSizing.Slot<String> slot(String key, int count, int limit) {
        return new MachineTransferSizing.Slot<>(key, count, limit);
    }

    @Test
    void oneEmptySlotFitsOnlyCompleteBatches() {
        assertEquals(21, MachineTransferSizing.maximumFitting(List.of(need("plate", 3, 0)), 64,
                List.of(slot(null, 0, 64)), key -> 64));
    }

    @Test
    void partialStacksAndInventoryItemsCountTowardTheOrder() {
        // 60 plates already present, room for four more, three plates per batch.
        assertEquals(21, MachineTransferSizing.maximumFitting(List.of(need("plate", 3, 0)), 40,
                List.of(slot("plate", 60, 64), slot("other", 64, 64)), key -> 64));
    }

    @Test
    void differentIngredientsCompeteForEmptySlots() {
        var requirements = List.of(need("plate", 2, 0), need("dust", 1, 0));
        assertEquals(0, MachineTransferSizing.maximumFitting(requirements, 64,
                List.of(slot(null, 0, 64)), key -> 64));
        assertEquals(32, MachineTransferSizing.maximumFitting(requirements, 64,
                List.of(slot(null, 0, 64), slot(null, 0, 64)), key -> 64));
    }

    @Test
    void nonStackableCatalystOccupiesOneSlotForAnyNumberOfBatches() {
        var requirements = List.of(need("wafer", 1, 0), need("lens", 0, 1));
        assertEquals(64, MachineTransferSizing.maximumFitting(requirements, 100,
                List.of(slot(null, 0, 64), slot(null, 0, 64)), key -> key.equals("lens") ? 1 : 64));
        assertEquals(64, MachineTransferSizing.maximumFitting(requirements, 100,
                List.of(slot("lens", 1, 1), slot(null, 0, 64)), key -> key.equals("lens") ? 1 : 64));
    }

    @Test
    void repeatedItemCanBeBothConsumedAndKeptAsCatalyst() {
        var combined = MachineTransferSizing.validateAndCombine(
                List.of(need("plate", 2, 0), need("plate", 1, 0), need("plate", 0, 2)), 64);
        assertEquals(List.of(need("plate", 3, 2)), combined);
        assertEquals(20, MachineTransferSizing.maximumFitting(combined, 64,
                List.of(slot(null, 0, 64)), key -> 64));
    }

    @Test
    void rejectsMalformedAndOverflowingRequestsBeforeMultiplication() {
        for (long perBatch : new long[] {-1, Long.MIN_VALUE, Long.MAX_VALUE, 2305}) {
            assertThrows(IllegalArgumentException.class, () ->
                    MachineTransferSizing.validateAndCombine(List.of(need("a", perBatch, 0)), 1));
        }
        for (long catalyst : new long[] {-1, Long.MIN_VALUE, Long.MAX_VALUE, 2305}) {
            assertThrows(IllegalArgumentException.class, () ->
                    MachineTransferSizing.validateAndCombine(List.of(need("a", 0, catalyst)), 1));
        }
        for (int batches : new int[] {-1, 0, 2305, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () ->
                    MachineTransferSizing.validateAndCombine(List.of(need("a", 1, 0)), batches));
        }
        assertThrows(IllegalArgumentException.class, () ->
                MachineTransferSizing.validateAndCombine(List.of(need("a", 0, 0)), 1));
        assertThrows(IllegalArgumentException.class, () ->
                MachineTransferSizing.validateAndCombine(List.of(need("a", 1153, 0), need("a", 1152, 0)), 1));
        assertThrows(IllegalArgumentException.class, () ->
                MachineTransferSizing.validateAndCombine(java.util.Collections.nCopies(33, need("a", 1, 0)), 1));
        assertEquals(List.of(need("a", 1, 0)),
                MachineTransferSizing.validateAndCombine(List.of(need("a", 1, 0)), 2304));
    }

    @Test
    void capacityMatchesExhaustiveInventoryInsertionForRandomOrders() {
        var random = new Random(8723);
        for (int trial = 0; trial < 2000; trial++) {
            var requirements = List.of(need("a", random.nextInt(4) + 1, random.nextInt(2)),
                    need("b", random.nextInt(4) + 1, random.nextInt(2)));
            var inventory = new ArrayList<MachineTransferSizing.Slot<String>>();
            for (int i = 0; i < 5; i++) {
                int kind = random.nextInt(4);
                String key = kind == 0 ? null : kind == 1 ? "a" : kind == 2 ? "b" : "other";
                int limit = "b".equals(key) ? 16 : 64;
                inventory.add(slot(key, key == null ? 0 : random.nextInt(limit) + 1, limit));
            }
            int expected = 0;
            for (int batches = 1; batches <= 40; batches++) {
                if (insertAll(requirements, batches, inventory)) {
                    expected = batches;
                }
            }
            assertEquals(expected, MachineTransferSizing.maximumFitting(requirements, 40, inventory,
                    key -> key.equals("b") ? 16 : 64));
        }
    }

    // Independent insertion simulation: fill matching stacks, then allocate empty slots.
    private static boolean insertAll(List<MachineTransferSizing.Requirement<String>> requirements, int batches,
            List<MachineTransferSizing.Slot<String>> original) {
        var inventory = new ArrayList<>(original);
        for (var requirement : requirements) {
            long present = original.stream().filter(slot -> requirement.key().equals(slot.key()))
                    .mapToLong(MachineTransferSizing.Slot::count).sum();
            long remaining = Math.max(0, requirement.amount(batches) - present);
            for (int i = 0; i < inventory.size() && remaining > 0; i++) {
                var slot = inventory.get(i);
                if (requirement.key().equals(slot.key())) {
                    int moved = (int) Math.min(remaining, slot.limit() - slot.count());
                    inventory.set(i, slot(slot.key(), slot.count() + moved, slot.limit()));
                    remaining -= moved;
                }
            }
            for (int i = 0; i < inventory.size() && remaining > 0; i++) {
                if (inventory.get(i).key() == null) {
                    int limit = requirement.key().equals("b") ? 16 : 64;
                    int moved = (int) Math.min(remaining, limit);
                    inventory.set(i, slot(requirement.key(), moved, limit));
                    remaining -= moved;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }
}
