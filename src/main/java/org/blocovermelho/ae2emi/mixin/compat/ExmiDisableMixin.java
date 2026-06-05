package org.blocovermelho.ae2emi.mixin.compat;

import com.kneelawk.extramodintegrations.ExMIPlugin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.EmiRegistry;
import org.blocovermelho.ae2emi.platform.facade.Ae2EmiMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ExMIPlugin.class, remap = false)
public class ExmiDisableMixin {
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lcom/kneelawk/extramodintegrations/AbstractAE2Integration;register(Ldev/emi/emi/api/EmiRegistry;)V"))
    public void a (EmiRegistry registry, Operation<Void> original) {
        if(Ae2EmiMod.BASE_CONFIG.compat.disableExMIAe2Integration) {
            Ae2EmiMod.LOGGER.warn("[Mod Compatibility] Disabled ExMI AppEng integration.");
        } else {
            Ae2EmiMod.LOGGER.warn("[Mod Compatibility] ExMI AppEng integration is preferred. Enabling it.");
            original.call(registry);
        }
    }
}
