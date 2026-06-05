package org.blocovermelho.ae2emi.platform.facade;

import org.blocovermelho.ae2emi.config.BaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ae2EmiMod {
    public static Logger LOGGER = LoggerFactory.getLogger(Ae2EmiMod.class);
    public static final BaseConfig BASE_CONFIG = BaseConfig.createToml(ModPathUtil.ae2emi_GetModConfigFolder(), "", "ae2emi-common", BaseConfig.class);
    public static void onInitialize() {
        // Cross-platform initialization code.
    }
}
