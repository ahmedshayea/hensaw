"""API routers for vector operations."""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from .clients import EngineClient, get_engine_client
from .embeddings import Embedder, get_embedder
from .schemas import (
    MatchModel,
    SearchRequestModel,
    SearchResponseModel,
    UpsertRequestModel,
)

router = APIRouter(prefix="/vectors", tags=["vectors"])


@router.post("/upsert")
async def upsert_vectors(
    payload: UpsertRequestModel,
    engine: EngineClient = Depends(get_engine_client),
    embedder: Embedder = Depends(get_embedder),
) -> dict[str, int]:
    if not payload.vectors:
        return {"upserted_count": 0}

    namespace = payload.vectors[0].namespace
    vector_messages = []
    for item in payload.vectors:
        if item.namespace != namespace:
            raise HTTPException(
                status_code=400, detail="All vectors must share namespace"
            )
        values = item.vector or embedder.embed(item.text or "")
        vector_id = item.id or str(hash((item.namespace, item.text or str(values))))
        vector_messages.append(
            engine.build_vector(
                vector_id=vector_id, values=values, metadata=item.metadata
            )
        )
    response = engine.upsert(namespace, vector_messages)
    return {"upserted_count": getattr(response, "upserted_count", 0)}


@router.post("/search", response_model=SearchResponseModel)
async def search_vectors(
    payload: SearchRequestModel,
    engine: EngineClient = Depends(get_engine_client),
    embedder: Embedder = Depends(get_embedder),
) -> SearchResponseModel:
    vector = payload.vector or embedder.embed(payload.text or "")
    response = engine.query(
        namespace=payload.namespace,
        vector=vector,
        top_k=payload.top_k,
        include_values=payload.include_values,
        include_metadata=payload.include_metadata,
    )
    matches = []
    for match in response.matches:
        matches.append(
            MatchModel(
                id=match.id,
                score=match.score,
                values=list(match.values) if payload.include_values else None,
                metadata=dict(match.metadata) if payload.include_metadata else None,
            )
        )
    return SearchResponseModel(matches=matches)
