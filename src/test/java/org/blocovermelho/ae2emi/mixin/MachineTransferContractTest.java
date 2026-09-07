package org.blocovermelho.ae2emi.mixin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class MachineTransferContractTest {
    private static final String ABSTRACT_MIXIN = "org/blocovermelho/ae2emi/mixin/AbstractRecipeHandlerMixin";
    private static final String MIXIN = "org/blocovermelho/ae2emi/mixin/UseCraftingRecipeHandlerMixin";
    private static final String TRANSFER = "org/blocovermelho/ae2emi/client/MachineRecipeTransfer";
    private static final String EMI_INGREDIENT = "dev/emi/emi/api/stack/EmiIngredient";
    private static final String EMI_CONTEXT = "dev/emi/emi/api/recipe/handler/EmiCraftContext";
    private static final String EMI_RECIPE = "dev/emi/emi/api/recipe/EmiRecipe";
    private static final String CRAFTING_MENU = "appeng/menu/me/items/CraftingTermMenu";
    private static final String NETWORK = "org/blocovermelho/ae2emi/network/Ae2EmiNetwork";
    private static final String REQUEST = "org/blocovermelho/ae2emi/network/TerminalIngredientRequest";

    @Test
    void nonCraftingRecipesAreAdvertisedThroughTheExistingAe2Handler() throws IOException {
        MethodNode supports = method(MIXIN, "ae2emi$supportMachineTransfer");
        assertTrue(calls(supports, TRANSFER, "isSupported"));
    }

    @Test
    void machineRecipeClassificationDoesNotInspectIngredients() throws IOException {
        MethodNode isSupported = method(TRANSFER, "isSupported");
        assertFalse(calls(isSupported, EMI_RECIPE, "getInputs"));
        assertFalse(calls(isSupported, EMI_RECIPE, "getCatalysts"));
    }

    @Test
    void machineCraftActionBuildsAndSendsAnIngredientRequest() throws IOException {
        MethodNode craft = method(MIXIN, "ae2emi$transferMachineIngredients");
        assertTrue(calls(craft, TRANSFER, "createRequest"));
        assertTrue(calls(craft, EMI_CONTEXT, "getDestination"));
        assertTrue(calls(craft, EMI_CONTEXT, "getAmount"));
        assertFalse(calls(craft, CRAFTING_MENU, "getCarried"));
        assertTrue(calls(craft, NETWORK, "sendToServer"));
    }

    @Test
    void backgroundCraftableScanChecksTheDestinationBeforeSelectingItems() throws IOException {
        MethodNode canCraft = method(ABSTRACT_MIXIN, "ae2emi$checkAvailableBatch");
        int destinationCheck = callIndex(canCraft, EMI_CONTEXT, "getDestination");
        int supportCheck = callIndex(canCraft, TRANSFER, "isSupported");
        int requestBuild = callIndex(canCraft, TRANSFER, "createRequest");

        assertTrue(destinationCheck >= 0);
        assertTrue(supportCheck > destinationCheck);
        assertTrue(requestBuild > destinationCheck);
    }

    @Test
    void requestUsesTheIngredientCountForTagAlternatives() throws IOException {
        MethodNode ingredients = method(TRANSFER, "addItemIngredients");
        assertTrue(calls(ingredients, EMI_INGREDIENT, "getAmount"));
    }

    @Test
    void serverChannelRegistersTheIngredientRequestPacket() throws IOException {
        MethodNode initialize = method(NETWORK, "initialize");
        for (var instruction : initialize.instructions) {
            if (instruction instanceof LdcInsnNode constant
                    && constant.cst instanceof Type type
                    && type.getInternalName().equals(REQUEST)) {
                return;
            }
        }
        throw new AssertionError("Ingredient request packet is not registered");
    }

    private static boolean calls(MethodNode method, String owner, String name) {
        return callIndex(method, owner, name) >= 0;
    }

    private static int callIndex(MethodNode method, String owner, String name) {
        int index = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static MethodNode method(String className, String methodName) throws IOException {
        try (var stream = MachineTransferContractTest.class.getResourceAsStream("/" + className + ".class")) {
            assertNotNull(stream, "Missing compiled class: " + className);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node.methods.stream()
                    .filter(method -> method.name.equals(methodName))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
