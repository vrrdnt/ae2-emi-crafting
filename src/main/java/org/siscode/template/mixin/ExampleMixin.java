package org.siscode.template.mixin;

import net.minecraft.server.dedicated.DedicatedServer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class ExampleMixin {
    @Shadow
    @Final
    static Logger LOGGER;

    @Inject(method = "initServer", at = @At("HEAD"))
    void templateMod$sayHiFromServer (CallbackInfoReturnable<Boolean> cir) {
        LOGGER.info("Hello from ExampleMixin~");
    }
}
