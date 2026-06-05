package org.blocovermelho.ae2emi.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class ModPathUtil {
    public static Path ae2emi_GetModConfigFolder_fabric() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
