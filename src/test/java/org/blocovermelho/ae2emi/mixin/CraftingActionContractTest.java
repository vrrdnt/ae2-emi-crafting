package org.blocovermelho.ae2emi.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Checks the compiled integration contract without loading Minecraft, AE2, or EMI.
 * The regular test JVM cannot instantiate their menus/inventories without a game bootstrap.
 */
class CraftingActionContractTest {
    private static final String MIXIN = "org/blocovermelho/ae2emi/mixin/AbstractRecipeHandlerMixin";
    private static final String REQUEST = "org/blocovermelho/ae2emi/network/TerminalCraftRequest";
    private static final String CONTEXT = "dev/emi/emi/api/recipe/handler/EmiCraftContext";
    private static final String INVENTORY = "dev/emi/emi/api/recipe/EmiPlayerInventory";

    @Test
    void craftabilityChecksOneBatchRegardlessOfTheSyntheticFavoriteAmount() throws IOException {
        MethodNode gate = method(MIXIN, "ae2emi$checkAvailableBatch");
        var checks = calls(gate).stream()
                .filter(call -> call.owner.equals(INVENTORY) && call.name.equals("canCraft"))
                .toList();

        assertEquals(1, checks.size());
        // EMI's one-argument overload checks exactly one batch. The old (recipe, amount)
        // overload rejected a 16-batch favorite even when three batches were available.
        assertEquals("(Ldev/emi/emi/api/recipe/EmiRecipe;)Z", checks.get(0).desc);
        assertTrue(calls(gate).stream().noneMatch(call ->
                call.owner.equals(CONTEXT) && call.name.equals("getAmount")));
    }

    @Test
    void requestedBatchLimitIsForwardedUnchangedToTheServer() throws IOException {
        MethodNode finish = method(MIXIN, "ae2emi$finishRequestedAction");
        var constructors = calls(finish).stream()
                .filter(call -> call.owner.equals(REQUEST) && call.name.equals("<init>"))
                .toList();
        assertEquals(1, constructors.size());
        MethodInsnNode constructor = constructors.get(0);
        assertEquals("(IL" + REQUEST + "$Destination;I)V", constructor.desc);

        AbstractInsnNode amount = previousInstruction(constructor);
        assertTrue(amount instanceof MethodInsnNode);
        MethodInsnNode getter = (MethodInsnNode) amount;
        assertEquals(CONTEXT, getter.owner);
        assertEquals("getAmount", getter.name);
        assertEquals("()I", getter.desc);
    }

    @ParameterizedTest
    @ValueSource(strings = { "craftToCursor", "craftToInventory" })
    void finiteCraftingChecksOutputBeforeTakingTheNextBatch(String methodName) throws IOException {
        MethodNode craft = method(REQUEST, methodName);
        var calls = calls(craft);
        MethodInsnNode outputCheck = calls.stream()
                .filter(call -> call.owner.equals("net/minecraft/world/item/ItemStack")
                        && call.name.equals("matches"))
                .findFirst().orElseThrow();
        MethodInsnNode click = calls.stream()
                .filter(call -> call.owner.equals("appeng/menu/slot/CraftingTermSlot")
                        && call.name.equals("doClick"))
                .findFirst().orElseThrow();
        assertTrue(craft.instructions.indexOf(outputCheck) < craft.instructions.indexOf(click),
                "Check the original output before crafting, not just cursor progress afterward");
        // A mismatch must return before any craft can happen.
        AbstractInsnNode branch = nextInstruction(outputCheck);
        assertEquals(org.objectweb.asm.Opcodes.IFNE, branch.getOpcode());
        assertEquals(org.objectweb.asm.Opcodes.RETURN, nextInstruction(branch).getOpcode());
    }

    private static MethodNode method(String className, String methodName) throws IOException {
        try (var stream = CraftingActionContractTest.class.getResourceAsStream("/" + className + ".class")) {
            assertNotNull(stream, "Missing compiled class: " + className);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node.methods.stream().filter(method -> method.name.equals(methodName))
                    .findFirst().orElseThrow();
        }
    }

    private static List<MethodInsnNode> calls(MethodNode method) {
        var calls = new ArrayList<MethodInsnNode>();
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                calls.add(call);
            }
        }
        return calls;
    }

    private static AbstractInsnNode previousInstruction(AbstractInsnNode instruction) {
        do {
            instruction = instruction.getPrevious();
        } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }

    private static AbstractInsnNode nextInstruction(AbstractInsnNode instruction) {
        do {
            instruction = instruction.getNext();
        } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }
}
