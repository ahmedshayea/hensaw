package org.hnsw;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.hnsw.grpc.VectorServiceImpl;
import org.hnsw.store.NamespaceIndexRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EngineApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineApplication.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        EngineConfig config = EngineConfig.fromEnv();
        NamespaceIndexRegistry registry = new NamespaceIndexRegistry(
                config.M(), config.efConstruction(), config.efSearch());

        Server server = NettyServerBuilder.forPort(config.port())
                .addService(new VectorServiceImpl(registry))
                .build()
                .start();

        LOGGER.info("Vector engine started on port {}", config.port());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down vector engine...");
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        server.awaitTermination();
    }
}
