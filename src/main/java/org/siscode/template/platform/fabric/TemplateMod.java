package org.siscode.template.platform.fabric;

import net.fabricmc.api.ModInitializer;
public class TemplateMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Calls cross-platform initializer
        org.siscode.template.platform.facade.TemplateMod.onInitialize();
        // Fabric-specific initalization code
    }
}
