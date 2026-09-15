# Netflix Clone — Video Streaming Platform

A microservices-based video streaming backend built with Spring Boot. Handles movie metadata, raw video upload to S3, asynchronous HLS encoding via FFmpeg, and adaptive-bitrate streaming with pre-signed S3 URLs cached in Redis.

---

## Architecture

```
                                          ┌─────────────────┐
                                          │   encoding-     │ ──► AWS S3
                          video.upload    │   service       │     (encoded HLS)
                        ┌──────────────►  │   :8083         │
                        │                 └────────┬────────┘
                        │                          │ video.encode
  User ──► API Gateway  │                          ▼
          (load         │  ┌─────────────┐   ┌─────────────┐
          balancer)  ───┼─►│movie-service│◄──│ video.encode│
                        │  │  :8081      │   │  (Kafka)    │
                        │  └─────────────┘   └─────────────┘
                        │         │ PostgreSQL (Neon)
                        │
                        │  ┌──────────────┐
                        ├─►│video-service │ ──► AWS S3 (raw upload)
                        │  │  :8082       │ ──► video.upload (Kafka)
                        │  └──────────────┘
                        │
                        │  ┌──────────────────┐
                        └─►│streaming-service │ ──► AWS S3 (pre-signed URLs)
                           │  :8084           │ ──► Redis (URL cache)
                           └──────────────────┘
```

---

## Services

### `movie-service` — Port 8081
Owns movie metadata in PostgreSQL. All other services reference movies by `movieId` (UUID).

**Endpoints**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/movies` | Create a new movie entry |
| `GET` | `/api/v1/movies/{movieId}` | Get movie by ID |
| `GET` | `/api/v1/movies/search?genre=` | Search movies by genre |
| `PUT` | `/api/v1/movies/{movieId}` | Update movie metadata |
| `DELETE` | `/api/v1/movies/{movieId}` | Delete a movie |

**Movie entity**

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Auto-generated primary key |
| `title` | String | Movie title |
| `genre` | Enum | Genre |
| `durationMinutes` | Integer | Runtime |
| `videoKey` | String | S3 key of the raw upload (set after upload) |
| `hlsUrl` | String | S3 URL of the HLS master playlist (set after encoding) |
| `videoStatus` | Enum | `PENDING → UPLOADED → ENCODED → READY / FAILED` |
| `createdAt` | LocalDateTime | Auto-set on create |
| `updatedAt` | LocalDateTime | Auto-set on update |

**Kafka consumers**
- `video.upload` → sets `videoKey`, status → `UPLOADED`
- `video.encode` → sets `hlsUrl`, status → `ENCODED` (or `FAILED`)

---

### `video-service` — Port 8082
Accepts raw video file uploads from the client, streams them directly to S3, and publishes a `VideoUploadEvent` to Kafka.

**Endpoints**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/video/upload/{movieId}` | Upload raw video file (`multipart/form-data`) |

**Flow**
1. Client POSTs a video file with the `movieId`
2. File is streamed to AWS S3 at key `raw/{movieId}/{filename}`
3. `VideoUploadEvent` published to `video.upload` Kafka topic

**`VideoUploadEvent` payload**
```json
{
  "movieId": "uuid",
  "bucketName": "...",
  "videoKey": "raw/movieId/filename.mp4",
  "originalFileName": "filename.mp4",
  "fileSizeInBytes": 123456789
}
```

---

### `encoding-service` — Port 8083
Consumes `VideoUploadEvent` from Kafka, downloads the raw video from S3, encodes it to HLS at 4 quality levels using FFmpeg, uploads the encoded segments back to S3, and publishes a `VideoEncodeEvent`.

**Kafka consumers**
- `video.upload` → triggers the encoding pipeline

**Encoding pipeline**
```
Download raw video from S3
        │
        ▼
Encode to 4 HLS quality levels (FFmpeg)
  ├── 1080p  (5000 kbps, 1920×1080)
  ├── 720p   (2800 kbps, 1280×720)
  ├── 480p   (1400 kbps, 854×480)
  └── 360p   (800 kbps,  640×360)
        │
        ▼
Generate master.m3u8 (adaptive bitrate manifest)
        │
        ▼
Upload all HLS files to S3 at encoded/{movieId}/
        │
        ▼
Publish VideoEncodeEvent to video.encode
        │
        ▼
Cleanup local temp files
```

**Temp directory layout**
```
/temp_files/encoding/{movieId}/
├── raw_video.mp4
└── encoded/
    ├── master.m3u8
    ├── 1080p/
    │   ├── playlist.m3u8
    │   └── segment_000.ts ...
    ├── 720p/ ...
    ├── 480p/ ...
    └── 360p/ ...
```

**`VideoEncodeEvent` payload**
```json
{
  "movieId": "uuid",
  "hlsUrl": "https://s3.../encoded/movieId/master.m3u8",
  "masterPlaylistKey": "encoded/movieId/master.m3u8",
  "success": true,
  "errorMessage": null
}
```

---

### `streaming-service` — Port 8084
Serves HLS streaming URLs to clients. Generates pre-signed S3 URLs for the master playlist and each segment playlist, with Redis caching to avoid redundant S3 calls.

**Endpoints**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/streaming/{movieId}` | Get pre-signed master playlist URL |
| `GET` | `/api/v1/streaming/{movieId}/playlist?path=` | Get signed content for a specific playlist path |

**Flow**
1. Client requests streaming URL for a `movieId`
2. Service checks Redis cache for the pre-signed URL
3. On cache miss: generates a pre-signed S3 URL (60s expiry) and caches it in Redis
4. Client uses the master playlist URL with an HLS player

**Kafka consumers**
- `video.encode` → caches the `hlsUrl` in Redis on successful encoding

---

## Kafka Topics

| Topic | Producer | Consumers | Purpose |
|-------|----------|-----------|---------|
| `video.upload` | `video-service` | `encoding-service`, `movie-service` | Signals a raw video has been uploaded to S3 |
| `video.encode` | `encoding-service` | `movie-service`, `streaming-service` | Signals encoding is complete (or failed) |

---

## Data Flow (End to End)

```
1. POST /api/v1/movies                   → movie-service creates record (status: PENDING)
2. POST /api/v1/video/upload/{movieId}   → video-service uploads raw file to S3
                                         → publishes VideoUploadEvent to video.upload
3. movie-service consumes video.upload   → updates videoKey, status: UPLOADED
4. encoding-service consumes video.upload→ downloads raw video, runs FFmpeg pipeline
                                         → uploads HLS segments to S3
                                         → publishes VideoEncodeEvent to video.encode
5. movie-service consumes video.encode   → updates hlsUrl, status: ENCODED (or FAILED)
6. streaming-service consumes video.encode→ caches hlsUrl in Redis
7. GET /api/v1/streaming/{movieId}       → streaming-service returns pre-signed master URL
8. HLS player fetches master.m3u8        → auto-selects quality, fetches segments
```

---

## Tech Stack

| Concern | Technology |
|---------|-----------|
| Framework | Spring Boot 4.0 |
| Language | Java 21 |
| Message broker | Apache Kafka 4.0 (KRaft mode) |
| Object storage | AWS S3 |
| Database | PostgreSQL (Neon serverless) |
| Cache | Redis 8 |
| Video encoding | FFmpeg (HLS, libx264, AAC) |
| Service discovery | Spring Cloud |
| Build | Maven (multi-module) |
| Containerisation | Docker / Docker Compose |
| CI pipeline | GitHub Actions → GHCR |

---

## Running Locally

### Prerequisites
- Docker & Docker Compose
- AWS S3 bucket with credentials

### 1. Clone the repo
```bash
git clone https://github.com/NeelisHere/netflix.git
cd netflix
```

### 2. Create `infrastructure/.env`
```env
POSTGRES_USERNAME=your_neon_db_username
POSTGRES_PASSWORD=your_neon_db_password

AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
AWS_REGION=us-east-1
AWS_BUCKET_NAME=your_s3_bucket_name
```

### 3. Pull images and start all services
```bash
cd infrastructure
docker compose --env-file .env up -d
```

This starts: Kafka, Redis, movie-service, video-service, encoding-service, streaming-service.

### 4. Running services individually (dev mode)
Each service uses `spring.profiles.active=dev` by default, connecting to `localhost:9092` for Kafka and `localhost:6379` for Redis.

```bash
# Start infrastructure only
docker compose up -d kafka redis

# Run a service
cd movie-service
mvn spring-boot:run
```

---

## CI / CD

Each service has its own GitHub Actions workflow under [`.github/workflows/`](.github/workflows/). On every push to `main`, all four workflows run in parallel:

| Workflow | Image pushed |
|----------|-------------|
| `movie-service-ci.yaml` | `ghcr.io/neelishere/netflix/movie-service:latest` |
| `video-service-ci.yaml` | `ghcr.io/neelishere/netflix/video-service:latest` |
| `encoding-service-ci.yaml` | `ghcr.io/neelishere/netflix/encoding-service:latest` |
| `streaming-service-ci.yaml` | `ghcr.io/neelishere/netflix/streaming-service:latest` |

Each workflow: checks out code → sets up JDK 21 → builds the Maven module (`-pl <service> -am`) → builds Docker image → pushes both `:latest` and `:<short-sha>` tags to GHCR.

---

## Project Structure

```
netflix/
├── common-lib/              # Shared DTOs, Kafka topic constants, enums
├── movie-service/           # Movie metadata CRUD + Kafka consumers
├── video-service/           # Raw video upload → S3 → Kafka producer
├── encoding-service/        # FFmpeg HLS encoding pipeline
├── streaming-service/       # Pre-signed URL generation + Redis cache
├── integration-tests/       # Cross-service integration tests
├── infrastructure/
│   └── docker-compose.yaml  # Kafka, Redis, all 4 services
└── .github/
    └── workflows/           # Per-service CI pipelines
```
