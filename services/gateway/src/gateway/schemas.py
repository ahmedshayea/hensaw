"""Pydantic models for request/response payloads."""

from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, Field, model_validator


class HealthResponse(BaseModel):
    status: Literal["ok"]


class VectorMetadata(BaseModel):
    __root__: dict[str, str] = Field(default_factory=dict)


class UpsertVector(BaseModel):
    id: Optional[str] = None
    namespace: str = "default"
    text: Optional[str] = None
    vector: Optional[list[float]] = None
    metadata: dict[str, str] = Field(default_factory=dict)

    @model_validator(mode="after")
    def ensure_text_or_vector(self) -> "UpsertVector":
        if bool(self.text) == bool(self.vector):
            raise ValueError("Provide exactly one of 'text' or 'vector'")
        return self


class UpsertRequestModel(BaseModel):
    vectors: list[UpsertVector]


class SearchRequestModel(BaseModel):
    namespace: str = "default"
    text: Optional[str] = None
    vector: Optional[list[float]] = None
    top_k: int = Field(default=5, ge=1, le=100)
    include_values: bool = False
    include_metadata: bool = True

    @model_validator(mode="after")
    def ensure_text_or_vector(self) -> "SearchRequestModel":
        if bool(self.text) == bool(self.vector):
            raise ValueError("Provide exactly one of 'text' or 'vector'")
        return self


class MatchModel(BaseModel):
    id: str
    score: float
    values: Optional[list[float]] = None
    metadata: Optional[dict[str, str]] = None


class SearchResponseModel(BaseModel):
    matches: list[MatchModel]
