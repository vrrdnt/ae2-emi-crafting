package org.blocovermelho.ae2emi.platform.fabric;

import net.fabricmc.api.ModInitializer;
public class Ae2EmiMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Calls cross-platform initializer
        org.blocovermelho.ae2emi.platform.facade.Ae2EmiMod.onInitialize();
        // Fabric-specific initalization code
    }
}
