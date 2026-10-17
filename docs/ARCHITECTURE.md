# Architecture

## Data flow

```
image bytes
   │
   ▼
ImageLoader (BufferedImage, TYPE_INT_ARGB)
   │
   ▼
FaceFeatureExtractor.extract  ──►  FaceEmbedding  ──►  FaceMatcher.similarity
   │                                                          │
   │                                                          ▼
   │                                                   FaceMatch (accepted?)
   │                                                          │
   ▼                                                          ▼
FaceEngine.enroll / identify  ──►  repositories  ──►  SQLite
```

## Extension points

| Interface | Purpose | Default implementation |
|---|---|---|
| `FaceFeatureExtractor` | image -> embedding | `GridGradientFeatureExtractor` (HOG) |
| `FaceMatcher` | two embeddings -> score + decision | `CosineFaceMatcher` |
| `TimeProvider` | "now" for testability | `TimeProvider.system()` |

Swapping the recogniser means implementing `FaceFeatureExtractor` (and optionally
`FaceMatcher`) and passing the new instances into `FaceEngine`. Nothing else
changes: persistence, matching policy, attendance and the guard all consume
`FaceEngine` through its public API.

## Persistence

`Database` wraps a single SQLite connection. Every repository is a thin, declarative
layer over `query` / `update` / `insert`. Schema creation is idempotent, so opening
an existing file is always safe. Tests use `Database.inMemory()`.

## Composition

`AppContext` is the composition root. It is the only class that instantiates
concrete implementations; the CLI and GUI receive an `AppContext` and ask it for
services. This keeps the feature packages free of wiring concerns and makes the
whole graph trivially replaceable in tests.
