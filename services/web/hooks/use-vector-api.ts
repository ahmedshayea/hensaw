import { useState } from "react";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8000";

interface VectorMatch {
  id: string;
  score: number;
  values?: number[];
  metadata?: Record<string, string>;
}

type UpsertPayload = {
  vectors: Array<{
    namespace: string;
    metadata: Record<string, unknown>;
    text?: string;
    vector?: number[];
    id?: string;
  }>;
};

type SearchPayload = {
  namespace: string;
  top_k: number;
  include_values: boolean;
  include_metadata: boolean;
  text?: string;
  vector?: number[];
};

interface SearchResponse {
  matches: VectorMatch[];
}

export function useVectorApi() {
  const [results, setResults] = useState<VectorMatch[]>([]);
  const [loading, setLoading] = useState({ insert: false, search: false });

  const upsertVectors = async (payload: UpsertPayload) => {
    setLoading((prev) => ({ ...prev, insert: true }));
    try {
      await fetch(`${API_URL}/vectors/upsert`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
    } finally {
      setLoading((prev) => ({ ...prev, insert: false }));
    }
  };

  const searchVectors = async (payload: SearchPayload) => {
    setLoading((prev) => ({ ...prev, search: true }));
    try {
      const response = await fetch(`${API_URL}/vectors/search`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        throw new Error(`Search failed with status ${response.status}`);
      }
      const data: SearchResponse = await response.json();
      setResults(data.matches ?? []);
    } finally {
      setLoading((prev) => ({ ...prev, search: false }));
    }
  };

  return {
    upsertVectors,
    searchVectors,
    loading,
    results,
  };
}
