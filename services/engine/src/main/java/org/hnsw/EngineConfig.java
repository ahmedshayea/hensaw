package org.hnsw;

public record EngineConfig(
        int port,
        int M,
        int efConstruction,
        int efSearch
) {

    private static final int DEFAULT_PORT = 50051;
    private static final int DEFAULT_M = 16;
    private static final int DEFAULT_EF_CONSTRUCTION = 200;
    private static final int DEFAULT_EF_SEARCH = 64;

    public static EngineConfig fromEnv() {
        return new EngineConfig(
                readEnv("ENGINE_PORT", DEFAULT_PORT),
                readEnv("ENGINE_M", DEFAULT_M),
                readEnv("ENGINE_EF_CONSTRUCTION", DEFAULT_EF_CONSTRUCTION),
                readEnv("ENGINE_EF_SEARCH", DEFAULT_EF_SEARCH)
        );
    }

    private static int readEnv(String key, int fallback) {
        String raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
