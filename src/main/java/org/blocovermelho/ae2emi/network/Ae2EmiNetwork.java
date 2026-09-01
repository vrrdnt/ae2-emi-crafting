package org.blocovermelho.ae2emi.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import org.blocovermelho.ae2emi.Ae2EmiMod;

public final class Ae2EmiNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(Ae2EmiMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private Ae2EmiNetwork() {
    }

    public static void initialize() {
        CHANNEL.messageBuilder(TerminalCraftRequest.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TerminalCraftRequest::encode)
                .decoder(TerminalCraftRequest::decode)
                .consumerMainThread(TerminalCraftRequest::handle)
                .add();
    }

    public static void sendToServer(TerminalCraftRequest request) {
        CHANNEL.sendToServer(request);
    }
}
