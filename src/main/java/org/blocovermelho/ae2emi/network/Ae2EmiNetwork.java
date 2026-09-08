package org.blocovermelho.ae2emi.network;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import org.blocovermelho.ae2emi.Ae2EmiMod;

public final class Ae2EmiNetwork {
    private static final String PROTOCOL_VERSION = "3";
    // Allow normal rapid clicks, but reject sustained floods before queueing game-thread work.
    private static final RequestRateLimiter<Connection> REQUESTS =
            new RequestRateLimiter<>(8, 50_000_000L, System::nanoTime);

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
                .consumerNetworkThread((request, context) -> {
                    enqueueRequest(request, context, TerminalCraftRequest::handle);
                })
                .add();
        CHANNEL.messageBuilder(TerminalIngredientRequest.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TerminalIngredientRequest::encode)
                .decoder(TerminalIngredientRequest::decode)
                .consumerNetworkThread((request, context) -> {
                    enqueueRequest(request, context, TerminalIngredientRequest::handle);
                })
                .add();
    }

    private static <T> void enqueueRequest(T request, Supplier<NetworkEvent.Context> contextSupplier,
            BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        var context = contextSupplier.get();
        context.setPacketHandled(true);
        if (REQUESTS.tryAcquire(context.getNetworkManager())) {
            // Menus, inventory, and player permissions must only be read on the game thread.
            context.enqueueWork(() -> handler.accept(request, () -> context));
        }
    }

    public static void sendToServer(TerminalCraftRequest request) {
        CHANNEL.sendToServer(request);
    }

    public static void sendToServer(TerminalIngredientRequest request) {
        CHANNEL.sendToServer(request);
    }
}
