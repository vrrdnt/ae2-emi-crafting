package org.blocovermelho.ae2emi.platform.facade;

public class ModLoadedUtil {
    public static boolean ae2emi_isModLoaded(String id) {
        return org.blocovermelho.ae2emi.platform.fabric.ModLoadedUtil.ae2emi_isModLoaded_fabric(id);
    }
}
