package org.hnsw.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hnsw.VectorRecord;
import org.hnsw.hnsw.HnswIndex;
import org.hnsw.hnsw.HnswIndex.SearchResult;
import org.hnsw.store.NamespaceIndexRegistry;
import vector_service.VectorServiceGrpc;
import vector_service.VectorServiceOuterClass.Match;
import vector_service.VectorServiceOuterClass.QueryRequest;
import vector_service.VectorServiceOuterClass.QueryResponse;
import vector_service.VectorServiceOuterClass.UpsertRequest;
import vector_service.VectorServiceOuterClass.UpsertResponse;
import vector_service.VectorServiceOuterClass.Vector;

public class VectorServiceImpl extends VectorServiceGrpc.VectorServiceImplBase {
    private final NamespaceIndexRegistry registry;

    public VectorServiceImpl(NamespaceIndexRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void upsert(UpsertRequest request, StreamObserver<UpsertResponse> responseObserver) {
        String namespace = request.getNamespace().isBlank() ? "default" : request.getNamespace();
        List<VectorRecord> records;
        try {
            records = request.getVectorsList().stream()
                    .map(VectorServiceImpl::toRecord)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
            return;
        }

        if (records.isEmpty()) {
            responseObserver.onNext(UpsertResponse.newBuilder().setUpsertedCount(0).build());
            responseObserver.onCompleted();
            return;
        }

        try {
            HnswIndex index = registry.getOrCreate(namespace, records.get(0).dimension());
            for (VectorRecord record : records) {
                if (record.dimension() != index.getDimension()) {
                    throw new IllegalArgumentException("All vectors must match dimension " + index.getDimension());
                }
                index.upsert(record);
            }
            responseObserver.onNext(UpsertResponse.newBuilder().setUpsertedCount(records.size()).build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void query(QueryRequest request, StreamObserver<QueryResponse> responseObserver) {
        String namespace = request.getNamespace().isBlank() ? "default" : request.getNamespace();
        HnswIndex index = registry.get(namespace);
        if (index == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Namespace not found").asRuntimeException());
            return;
        }

        float[] queryVector = toArray(request.getVectorList());
        int topK = request.getTopK() > 0 ? request.getTopK() : 5;

        try {
            List<SearchResult> matches = index.search(queryVector, topK);
            QueryResponse.Builder builder = QueryResponse.newBuilder();
            for (SearchResult result : matches) {
                builder.addMatches(toMatch(result, request.getIncludeValues(), request.getIncludeMetadata()));
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    private static VectorRecord toRecord(Vector vector) {
        float[] values = toArray(vector.getValuesList());
        Map<String, String> metadata = vector.getMetadataMap();
        return new VectorRecord(vector.getId(), values, metadata);
    }

    private static float[] toArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private static Match toMatch(SearchResult result, boolean includeValues, boolean includeMetadata) {
        Match.Builder builder = Match.newBuilder()
                .setId(result.record().id())
                .setScore(1.0 - result.distance());
        if (includeValues) {
            for (float value : result.record().values()) {
                builder.addValues(value);
            }
        }
        if (includeMetadata) {
            builder.putAllMetadata(result.record().metadata());
        }
        return builder.build();
    }
}
