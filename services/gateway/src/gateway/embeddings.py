"""Embedding providers for the gateway."""

from __future__ import annotations

import functools
import os
from typing import Protocol

from sentence_transformers import SentenceTransformer


class Embedder(Protocol):
    def embed(self, text: str) -> list[float]: ...


class SentenceTransformerEmbedder:
    def __init__(self, model_name: str) -> None:
        self._model = SentenceTransformer(model_name)

    def embed(self, text: str) -> list[float]:
        vector = self._model.encode(text)
        return vector.tolist()


@functools.lru_cache(maxsize=1)
def get_embedder() -> Embedder:
    model_name = os.getenv("EMBEDDER_MODEL_NAME", "all-MiniLM-L6-v2")
    return SentenceTransformerEmbedder(model_name)
