package org.blocovermelho.ae2emi;

import net.minecraftforge.fml.common.Mod;

import org.blocovermelho.ae2emi.network.Ae2EmiNetwork;

@Mod(Ae2EmiMod.MOD_ID)
public final class Ae2EmiMod {
    public static final String MOD_ID = "ae2emi";

    public Ae2EmiMod() {
        Ae2EmiNetwork.initialize();
    }
}
