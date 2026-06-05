package org.blocovermelho.ae2emi.mixin;

import appeng.client.gui.me.common.TerminalSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = TerminalSettingsScreen.class, remap = false)
public class TerminalSettingsScreenMixin {
    @ModifyVariable(method = "<init>(Lappeng/client/gui/me/common/MEStorageScreen;)V", at = @At(value = "STORE"), name = "hasExternalSearch")
    private boolean ae2emi$setSearchAvailable(boolean hasExternalSearch) {
        return true;
    }

    @ModifyVariable(method = "<init>(Lappeng/client/gui/me/common/MEStorageScreen;)V", at = @At("STORE"), name = "externalSearchMod")
    private Component ae2emi$setSearchModName(Component externalSearchMod) {
        return Component.literal("EMI");
    }
}
