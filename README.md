# Face Security Suite

A **privacy-first** desktop security suite that combines **offline face recognition**,
**attendance tracking** and a **signature + heuristic antivirus engine** with a **face-gated
access guard** — all in pure Java, with zero cloud calls and zero native dependencies.

> Built to be runnable on any machine with a JDK, testable in CI, and honest about
> what it does and does not do.

[![CI](https://img.shields.io/github/actions/workflow/status/shubh-3011/face-security-suite/ci.yml?branch=main&label=build)](https://github.com/shubh-3011/face-security-suite/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/build-Maven-C71A36.svg)](https://maven.apache.org/)

## Why this exists

Most "face attendance" and "antivirus" student projects are thin wrappers around
proprietary SDKs that cannot be built, tested or run by a reviewer. This project
takes the opposite approach:

- **Every algorithm is implemented in the repository.** The face descriptor is a
  HOG pipeline with a cosine matcher; the antivirus engine runs signature, hash
  and heuristic layers. Nothing is hidden behind a binary.
- **Everything is pluggable.** `FaceFeatureExtractor` and `FaceMatcher` are
  interfaces, so the HOG baseline can be swapped for an OpenCV/DNN embedding
  without touching the rest of the suite.
- **It is testable.** 33 unit and integration tests run on a plain JDK in CI.
- **It runs headless.** A full CLI mirrors every feature, so the suite works on
  servers and in pipelines, not only behind a desktop.

## Features

### Face recognition (offline)
- Pure-Java feature extractor: greyscale, bilinear resize, histogram equalisation,
  HOG over 8x8 cells with 2x2 block normalisation, Gaussian pre-smoothing and
  DC removal.
- Cosine-similarity matcher with a configurable threshold.
- Enrolment reuses an identity when the name already exists, accumulating samples.
- Ranked candidate list ("did you mean") for diagnostics.

### Attendance
- Daily check-in from a captured face, with **idempotency per person per day**.
- On-time vs. late classification driven by an `AttendancePolicy` (configurable
  work start and grace period).
- Manual override check-ins at the desk.
- Daily statistics and on-time rate.
- Export to **CSV** and **PDF** (OpenPDF).

### Antivirus
- Layered detection: **SHA-256 reputation**, **literal and regex content signatures**,
  and **heuristics** (high entropy / packing, double extensions, executables
  masquerading behind document extensions).
- A versioned, **data-driven signature database** (tab-separated, commentable)
  that ships with 18 rules and accepts user-supplied overlays.
- **Quarantine** that moves a threat out of harm's way and keeps a restorable
  audit record.
- Persistent scan history with verdict counts.

### Trusted Face Protection (the integration feature)
- Bind any file, folder or application to a single identity.
- A probe face is matched against the owner before access is granted.
- Demonstrates the two halves of the suite working together.

### Interfaces
- **JavaFX desktop UI** with a dashboard, face registry, attendance, antivirus and
  trusted-access screens, dark themed.
- **Headless CLI** with subcommands for every feature (see below).
- **Demo runner** that generates synthetic faces and exercises every subsystem end
  to end, usable as a regression test.

## Architecture

```
com.shubh3011.facesecurity
├── model/          pure data types (Person, FaceEmbedding, AttendanceRecord, ...)
├── face/           feature extraction + matching + FaceEngine
├── attendance/     policy, service, CSV/PDF exporters
├── antivirus/      entropy, signatures, scanner, quarantine
├── guard/          TrustedFaceGuardian (face-gated access)
├── db/             SQLite facade + repositories
├── app/            AppConfig, AppContext (composition root), DemoImages, DemoRunner
├── cli/            headless entry point
└── gui/            JavaFX screens
```

Dependencies flow **inwards**: `gui` and `cli` depend on `app`; `app` depends on the
feature packages; feature packages depend on `db` and `model`. `model` has no
dependencies. The composition root (`AppContext`) is the only place that knows
about every concrete implementation, which is what makes the pluggable parts
swappable.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the data flow and the
extension points.

## Quick start

Requirements: **JDK 21** and **Maven 3.9+**.

```bash
git clone https://github.com/shubh-3011/face-security-suite.git
cd face-security-suite

# run the test suite
mvn test

# end-to-end demo on synthetic data
mvn -q exec:java -Dexec.mainClass=com.shubh3011.facesecurity.cli.Main -Dexec.args="demo"

# desktop UI
mvn javafx:run
```

## CLI usage

```bash
# enrol a face
face-security-suite enroll --name "Ada Lovelace" --role STAFF --image ada.png

# identify a probe
face-security-suite identify --image probe.png

# list enrolled people
face-security-suite people

# attendance
face-security-suite attendance --today
face-security-suite attendance --date 2026-10-17
face-security-suite export --format pdf --out report.pdf --date 2026-10-17

# scan a file (exit code 1 if anything is MALICIOUS)
face-security-suite scan suspicious.exe --quarantine

# scan history
face-security-suite history --limit 20

# list loaded signatures
face-security-suite signatures

# face-gated access
face-security-suite protect --label "vault" --path /data/vault --person-id 1
face-security-suite access --entity-id 1 --image ada.png

# configuration / state
face-security-suite info
```

The demo creates its own temporary workspace and data directory, so it never
touches your real data.

## Design notes

### Face descriptor
The shipped descriptor is a HOG pipeline: greyscale, bilinear resize to 64x64,
histogram equalisation, Gaussian pre-smoothing (sigma 2.2), gradient orientation
histograms over 8x8 cells with 2x2 block L2-normalisation, then **DC removal** and
a final L2 normalisation. The cosine matcher's default threshold is 0.80.

Measured on 24 synthetic identities with brightness/exposure variation:
min same-identity similarity 0.93, max cross-identity similarity 0.71, 100%
identification accuracy. The descriptor is deliberately modest and documented as
such - it exists to make the architecture, matching and testing story concrete,
and the `FaceFeatureExtractor` interface is the seam for a production DNN.

### Antivirus verdict policy
A signature hit of `MEDIUM` or above is `MALICIOUS`; any other hit, or a
heuristic alone, is `SUSPICIOUS`. Heuristics cover packed/encrypted payloads
(Shannon entropy), double extensions and executables masquerading behind document
extensions. Nothing is ever deleted - quarantine is reversible.

### Persistence
SQLite via JDBC, single shared connection, WAL mode, foreign keys. Schema is
created idempotently on open. Tests use an in-memory database, so they never
touch the filesystem.

## Testing

```bash
mvn test          # 33 tests
mvn verify        # tests + JaCoCo coverage report in target/site/jacoco
```

The suite covers the embedding math, the descriptor (determinism, brightness
invariance, separation), enrolment and identification, attendance policy and
idempotency, CSV export, every antivirus layer, quarantine round-trips, the
face-gated guard, the repositories against a real SQLite database, and the
end-to-end demo.

## Building a runnable jar

```bash
mvn package
java -jar target/face-security-suite.jar --help
```

The shaded jar contains the CLI and all dependencies except JavaFX (which stays on
the module path for `mvn javafx:run`).

## Roadmap

- [ ] Drop-in DNN embedding via OpenCV/JavaCV behind `FaceFeatureExtractor`.
- [ ] Liveness / anti-spoofing check before a check-in is recorded.
- [ ] Role-based access control on top of the trusted-face guard.
- [ ] REST API (Javalin) so the suite can be driven from a web front-end.
- [ ] Signed, versioned signature packs downloaded from a trusted feed.

## Contributing

Issues and pull requests are welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

[MIT](LICENSE) - copyright (c) 2026 Shubh.
