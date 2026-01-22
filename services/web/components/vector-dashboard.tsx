"use client";

import { useMemo, useState } from "react";
import { useDebounce } from "@/hooks/use-debounce";
import { useVectorApi } from "@/hooks/use-vector-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const VECTOR_MODE = [
  { value: "text", label: "Text" },
  { value: "vector", label: "Vector" },
];

export function VectorDashboard() {
  const [insertMode, setInsertMode] = useState<"text" | "vector">("text");
  const [queryMode, setQueryMode] = useState<"text" | "vector">("text");
  const [insertPayload, setInsertPayload] = useState({
    namespace: "default",
    text: "",
    vector: "",
    metadata: "",
    id: "",
  });
  const [searchPayload, setSearchPayload] = useState({
    namespace: "default",
    text: "",
    vector: "",
    topK: 5,
    includeValues: false,
    includeMetadata: true,
  });
  const debounceMetadata = useDebounce(insertPayload.metadata, 300);
  const api = useVectorApi();

  const parsedMetadata = useMemo(() => {
    if (!debounceMetadata.trim()) return {};
    try {
      return JSON.parse(debounceMetadata);
    } catch {
      return {};
    }
  }, [debounceMetadata]);

  const handleInsert = async () => {
    const payload: {
      vectors: Array<{
        namespace: string;
        metadata: Record<string, unknown>;
        text?: string;
        vector?: number[];
        id?: string;
      }>;
    } = {
      vectors: [
        {
          namespace: insertPayload.namespace,
          metadata: parsedMetadata,
        },
      ],
    };
    if (insertMode === "text") {
      payload.vectors[0].text = insertPayload.text;
    } else {
      payload.vectors[0].vector = parseVector(insertPayload.vector);
    }
    if (insertPayload.id) {
      payload.vectors[0].id = insertPayload.id;
    }
    await api.upsertVectors(payload);
    setInsertPayload({ ...insertPayload, text: "", vector: "", metadata: "", id: "" });
  };

  const handleSearch = async () => {
    const payload: {
      namespace: string;
      top_k: number;
      include_values: boolean;
      include_metadata: boolean;
      text?: string;
      vector?: number[];
    } = {
      namespace: searchPayload.namespace,
      top_k: searchPayload.topK,
      include_values: searchPayload.includeValues,
      include_metadata: searchPayload.includeMetadata,
    };
    if (queryMode === "text") {
      payload.text = searchPayload.text;
    } else {
      payload.vector = parseVector(searchPayload.vector);
    }
    await api.searchVectors(payload);
  };

  return (
    <div className="container mx-auto py-8 space-y-6">
      <header className="space-y-2">
        <p className="text-sm uppercase tracking-wide text-muted-foreground">
          Vector Playground
        </p>
        <h1 className="text-3xl font-semibold">HNSW Gateway Console</h1>
        <p className="text-muted-foreground">
          Insert custom vectors or text to embed via LangChain, then search across namespaces.
        </p>
      </header>

      <section className="grid gap-6 md:grid-cols-2">
        <Card className="border-primary/20">
          <CardHeader>
            <CardTitle>Insert Vector</CardTitle>
            <CardDescription>Provide text for automatic embedding or paste raw embeddings.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-2">
              <Label>Namespace</Label>
              <Input
                value={insertPayload.namespace}
                onChange={(e) => setInsertPayload({ ...insertPayload, namespace: e.target.value })}
              />
            </div>

            <div className="grid gap-2">
              <Label>ID (optional)</Label>
              <Input
                value={insertPayload.id}
                onChange={(e) => setInsertPayload({ ...insertPayload, id: e.target.value })}
              />
            </div>

            <div className="flex items-center gap-4">
              <Label>Mode</Label>
              <Select value={insertMode} onValueChange={(value: "text" | "vector") => setInsertMode(value)}>
                <SelectTrigger className="w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {VECTOR_MODE.map((mode) => (
                    <SelectItem key={mode.value} value={mode.value}>
                      {mode.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {insertMode === "text" ? (
              <div className="grid gap-2">
                <Label>Text</Label>
                <Textarea
                  rows={4}
                  placeholder="This will be embedded via Sentence Transformers"
                  value={insertPayload.text}
                  onChange={(e) => setInsertPayload({ ...insertPayload, text: e.target.value })}
                />
              </div>
            ) : (
              <div className="grid gap-2">
                <Label>Vector</Label>
                <Textarea
                  rows={4}
                  placeholder="Comma-separated floats"
                  value={insertPayload.vector}
                  onChange={(e) => setInsertPayload({ ...insertPayload, vector: e.target.value })}
                />
              </div>
            )}

            <div className="grid gap-2">
              <Label>Metadata (JSON)</Label>
              <Textarea
                rows={3}
                placeholder='e.g. {"source": "manual"}'
                value={insertPayload.metadata}
                onChange={(e) => setInsertPayload({ ...insertPayload, metadata: e.target.value })}
              />
              <span className="text-xs text-muted-foreground">
                {insertPayload.metadata && Object.keys(parsedMetadata).length === 0
                  ? "Invalid JSON"
                  : "Metadata stored in index"}
              </span>
            </div>

            <Button className="w-full" onClick={handleInsert} disabled={api.loading.insert}>
              {api.loading.insert ? "Inserting..." : "Upsert Vector"}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Search Index</CardTitle>
            <CardDescription>Embed a query or supply an embedding to find nearest neighbors.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-2">
              <Label>Namespace</Label>
              <Input
                value={searchPayload.namespace}
                onChange={(e) => setSearchPayload({ ...searchPayload, namespace: e.target.value })}
              />
            </div>

            <div className="flex items-center gap-4">
              <Label>Mode</Label>
              <Select value={queryMode} onValueChange={(value: "text" | "vector") => setQueryMode(value)}>
                <SelectTrigger className="w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {VECTOR_MODE.map((mode) => (
                    <SelectItem key={mode.value} value={mode.value}>
                      {mode.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {queryMode === "text" ? (
              <div className="grid gap-2">
                <Label>Query Text</Label>
                <Textarea
                  rows={4}
                  placeholder="Describe what you are looking for"
                  value={searchPayload.text}
                  onChange={(e) => setSearchPayload({ ...searchPayload, text: e.target.value })}
                />
              </div>
            ) : (
              <div className="grid gap-2">
                <Label>Query Vector</Label>
                <Textarea
                  rows={4}
                  placeholder="Comma-separated floats"
                  value={searchPayload.vector}
                  onChange={(e) => setSearchPayload({ ...searchPayload, vector: e.target.value })}
                />
              </div>
            )}

            <div className="grid gap-2">
              <Label>Top K</Label>
              <Input
                type="number"
                min={1}
                max={50}
                value={searchPayload.topK}
                onChange={(e) => setSearchPayload({ ...searchPayload, topK: Number(e.target.value) })}
              />
            </div>

            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <Switch
                  checked={searchPayload.includeValues}
                  onCheckedChange={(checked) => setSearchPayload({ ...searchPayload, includeValues: checked })}
                />
                <Label>Include Values</Label>
              </div>
              <div className="flex items-center gap-2">
                <Switch
                  checked={searchPayload.includeMetadata}
                  onCheckedChange={(checked) => setSearchPayload({ ...searchPayload, includeMetadata: checked })}
                />
                <Label>Include Metadata</Label>
              </div>
            </div>

            <Button className="w-full" onClick={handleSearch} disabled={api.loading.search}>
              {api.loading.search ? "Searching..." : "Search"}
            </Button>

            <div className="space-y-2">
              <h3 className="text-sm font-medium">Results</h3>
              <div className="space-y-3 max-h-64 overflow-y-auto pr-2">
                {api.results.map((match) => (
                  <Card key={`${match.id}-${match.score}`} className="bg-secondary/30">
                    <CardContent className="py-3 space-y-2">
                      <div className="flex items-center justify-between">
                        <p className="font-medium">{match.id}</p>
                        <span className="text-xs">score: {match.score.toFixed(3)}</span>
                      </div>
                      {match.metadata && (
                        <pre className="text-xs bg-background/80 p-2 rounded">
                          {JSON.stringify(match.metadata, null, 2)}
                        </pre>
                      )}
                    </CardContent>
                  </Card>
                ))}
                {api.results.length === 0 && (
                  <p className="text-sm text-muted-foreground">No matches yet.</p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}

function parseVector(raw: string): number[] {
  return raw
    .split(/[,\s]+/)
    .map((value) => Number(value.trim()))
    .filter((value) => !Number.isNaN(value));
}
