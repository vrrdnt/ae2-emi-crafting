package org.blocovermelho.ae2emi.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class ModLoadedUtil {
    public static boolean ae2emi_isModLoaded_fabric(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
