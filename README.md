# Tenpo Backend Challenge

REST API built with Java 21 and Spring Boot 3 for the Tenpo backend challenge.

The service receives two positive numbers, adds them, obtains a dynamic percentage
through a provider boundary, and returns the sum plus that percentage. It applies
a per-IP rate limit whose scope is determined by the effective policy for the
request, and exposes a paginated history of functional API calls persisted in
PostgreSQL.

Functional calls — including successful calculations, validation errors, provider
failures, and rate-limited requests — are recorded asynchronously on a best-effort
basis. Technical endpoints (Swagger, OpenAPI, actuator, call-history, favicon)
are excluded from recording. For the full design rationale, see
[Decisiones Técnicas](#decisiones-técnicas).

## Quick Start

```bash
# 1. Start the full stack (API + PostgreSQL)
make docker-up
```

If `make` is not available, use:

```bash
docker compose up --build
```

```bash
# 2. Call the calculation endpoint — the mock percentage provider returns 10 %
curl -s -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"num1":5,"num2":5}'
```

Expected response (sum = 10, percentage = 10, result = 11):

```json
{
  "num1": 5,
  "num2": 5,
  "sum": 10,
  "percentage": 10,
  "result": 11
}
```

Key URLs once the stack is running:

| Resource            | URL                                      |
|---------------------|------------------------------------------|
| Base URL            | `http://localhost:8080`                  |
| Swagger UI          | `http://localhost:8080/swagger-ui.html`   |
| OpenAPI spec        | `http://localhost:8080/v3/api-docs`       |
| Health check        | `http://localhost:8080/actuator/health`   |

Stop the stack:

```bash
make docker-down
```

## Stack

- Java 21
- Spring Boot 3.3
- Maven Wrapper
- Spring MVC + Bean Validation
- Spring Data JPA / Hibernate
- PostgreSQL 16 + Flyway
- Resilience4j Retry (exponential backoff)
- Bucket4j + Caffeine (in-memory rate limiting)
- springdoc-openapi
- JUnit 5, Mockito, MockMvc, Testcontainers, ArchUnit
- Docker and Docker Compose

## Prerequisites

- JDK 21
- Docker and Docker Compose
- `curl` and `unzip` for the Maven Wrapper bootstrap on macOS/Linux

The repository includes `./mvnw`, which downloads Maven 3.9.9 on first use.

## Run With Docker Compose

```bash
make docker-up
```

This builds the API image and runs the Compose stack in the foreground. The API
is exposed at `http://localhost:8080`.

```bash
make docker-down   # stop the stack
make docker-logs   # follow logs
make config-check  # validate Compose configuration
```

## Docker Hub Image

The API image is published at
[docker.io/marcev/tenpo-challenge](https://hub.docker.com/r/marcev/tenpo-challenge).

Pull and run the published image with PostgreSQL through the Hub Compose file:

```bash
docker compose -f docker-compose.hub.yml up
```

This starts the API container from the published image alongside a
`postgres:16-alpine` container with the required network, volume, healthcheck
and environment variables.

## Run Locally

Start only PostgreSQL with Docker:

```bash
docker compose up -d postgres
```

Run the Spring Boot application on the host:

```bash
make run
```

Default local database configuration:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tenpo_challenge
SPRING_DATASOURCE_USERNAME=tenpo
SPRING_DATASOURCE_PASSWORD=tenpo
```

Flyway runs automatically on startup and creates the `call_history` table.

By default, the percentage provider uses an in-memory HTTP transport that returns
`10`, so no external service is needed. Provider URL, path, and retry settings
are configurable through:

```text
PERCENTAGE_PROVIDER_BASE_URL
PERCENTAGE_PROVIDER_PATH
PERCENTAGE_PROVIDER_RETRY_MAX_ATTEMPTS
PERCENTAGE_PROVIDER_TRANSPORT_MODE   # "mock" (default) or "failure"
```

Rate limiting and async history persistence are also configurable:

```text
RATE_LIMIT_CAPACITY
RATE_LIMIT_REFILL_TOKENS
RATE_LIMIT_REFILL_PERIOD
CALL_HISTORY_ASYNC_CONCURRENCY_LIMIT
```

## API Usage

| Method | Endpoint                  | Description               |
|--------|---------------------------|---------------------------|
| POST   | `/api/v1/calculations`    | Calculate with percentage |
| GET    | `/api/v1/call-history`    | Paginated call history    |
| GET    | `/swagger-ui.html`        | Swagger UI                |
| GET    | `/v3/api-docs`            | OpenAPI specification     |
| GET    | `/actuator/health`        | Health check              |

### Calculate With Percentage

```bash
curl -s -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"num1":100,"num2":50}'
```

Successful response:

```json
{
  "num1": 100,
  "num2": 50,
  "sum": 150,
  "percentage": 10,
  "result": 165
}
```

Rules and errors:

- `num1` and `num2` are required and must be positive numbers.
- Invalid JSON or validation errors return `400 Bad Request`.
- If the percentage provider cannot be resolved after exhausting retries
  (initial attempt + 2 retries = 3 total attempts), the API returns
  `503 Service Unavailable`.

Shared error response shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Request body is invalid"
}
```

### Call History

```bash
curl -s 'http://localhost:8080/api/v1/call-history?page=0&size=20'
```

Response shape:

```json
{
  "content": [
    {
      "id": "2f75cfb0-8c11-4b8e-91b9-9dfdc21f90d7",
      "occurredAt": "2026-07-02T19:20:03.385224Z",
      "httpMethod": "POST",
      "endpoint": "/api/v1/calculations",
      "queryParams": null,
      "requestBody": "{\"num1\":100,\"num2\":50}",
      "responseBody": "{\"num1\":100,\"num2\":50,\"sum\":150,\"percentage\":10,\"result\":165}",
      "errorBody": null,
      "httpStatus": 200,
      "success": true,
      "durationMs": 58,
      "clientIp": "127.0.0.1"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

Pagination parameters:

| Parameter | Type    | Default | Constraints     |
|-----------|---------|---------|-----------------|
| `page`    | integer | `0`     | `>= 0`          |
| `size`    | integer | `20`    | `>= 1, <= 100`  |

Invalid pagination returns `400 Bad Request`. Records are ordered by
`occurredAt` descending, then `id` descending.

### Call History Recording Rules

| Call type              | Recorded? | `success` | `httpStatus` | Notes                       |
|------------------------|-----------|-----------|--------------|-----------------------------|
| Successful calculation | yes       | `true`    | 200          |                             |
| Validation error (400) | yes       | `false`   | 400          | `errorBody` populated       |
| Provider failure (503) | yes       | `false`   | 503          | `errorBody` populated       |
| Rate-limited (429)     | yes       | `false`   | 429          | `errorBody` populated       |

The following endpoints are **excluded** from call-history recording to avoid
noise and self-registration loops:

- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/api/v1/call-history`
- `/actuator/**`
- `/favicon.ico`

### Rate Limiting

The rate limit is enforced per source IP by a `OncePerRequestFilter`.
Responses include `X-RateLimit-Limit` and `X-RateLimit-Remaining` headers. When
the limit is exceeded the API returns `429 Too Many Requests` with the shared
error response body and a `Retry-After` header.

#### Policy Scope

Functional API calls (POST `/api/v1/calculations`) are limited to 3 requests per
minute. Technical endpoints use a separate, higher-limit policy so they never
consume the functional quota:

| Endpoint               | Policy     | Effective Quota |
|------------------------|------------|-----------------|
| `/api/v1/calculations` | default    | 3 RPM per IP    |
| `/api/v1/call-history` | default    | 3 RPM per IP    |
| `/swagger-ui/**`       | tech-policy| 60 RPM per IP   |
| `/v3/api-docs/**`      | tech-policy| 60 RPM per IP   |
| `/actuator/**`         | tech-policy| 60 RPM per IP   |

The bucket key is scoped by client IP and effective policy. All functional
endpoints that resolve to the same policy share the same quota instead of
getting a separate bucket per URI.

Client IP resolution uses `X-Forwarded-For` when available (leftmost IP in the
chain), falling back to `getRemoteAddr()`. Resolution is abstracted behind the
injectable `ClientIpResolver` interface.

Per-IP buckets are stored in a Caffeine in-memory cache with configurable TTL
eviction (`rate-limit.cache-ttl`, default 2 minutes). The cache sits behind
`RateLimiterPort`, ready to be swapped to Redis for multi-instance deployments
without changing application code.

### Error Handling

All error responses share a consistent format:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Request body is invalid"
}
```

| Code | Error                 | Scenario                                                    |
|------|-----------------------|-------------------------------------------------------------|
| 400  | Bad Request           | Malformed JSON, non-positive `num1`/`num2`, invalid pagination |
| 429  | Too Many Requests     | Rate limit exceeded; includes `Retry-After` header          |
| 503  | Service Unavailable   | Percentage provider unavailable after exhausting retries    |

History persistence failures are logged at warning level and **never** affect
the API response (best-effort recording).

### Asynchronous History Recording

History recording fires after the HTTP response is produced and does not block
the main API response. The recorder uses a dedicated virtual-thread executor with
bounded concurrency:

```text
CALL_HISTORY_ASYNC_CONCURRENCY_LIMIT   # default: 2
```

Request and response bodies are truncated to 4 KB as a defensive measure.

The recorder is configured **fail-fast**: if the `CallHistoryRepository` bean
cannot be created the application refuses to start rather than silently
discarding records. No external queue, outbox, or broker is required.

## Smoke Tests

Copy-paste commands to validate the main scenarios once the stack is running.

### 1. Happy Path — Successful Calculation

```bash
curl -s -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"num1":5,"num2":5}'
# → 200 OK
# → {"num1":5,"num2":5,"sum":10,"percentage":10,"result":11}
```

### 2. Validation Error — 400 Bad Request

```bash
# Negative number
curl -s -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"num1":-1,"num2":5}'
# → 400 {"status":400,"error":"Bad Request","message":"..."}

# Missing field
curl -s -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"num1":5}'
# → 400

# Malformed JSON
curl -s -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d 'not-json'
# → 400 {"status":400,"error":"Bad Request","message":"Request body is invalid"}
```

### 3. Rate Limit — 429 Too Many Requests

```bash
# Send 4 requests in quick succession (limit is 3 RPM per IP)
for i in $(seq 1 4); do
  curl -s -o /dev/null -w "Request $i: %{http_code}\n" \
    -X POST http://localhost:8080/api/v1/calculations \
    -H 'Content-Type: application/json' \
    -d '{"num1":5,"num2":5}'
done
# If you already consumed the quota with previous examples, wait 60 seconds or
# restart the stack before running this test. With a fresh bucket, requests 1–3
# return 200 and request 4 returns 429.

# Inspect rate-limit headers
curl -s -D - -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"num1":5,"num2":5}' | head -10
# → X-RateLimit-Limit: 3
# → X-RateLimit-Remaining: ...
# → Retry-After: ... (only when 429)
```

### 4. Call History — Paginated Query

```bash
# Basic query
curl -s 'http://localhost:8080/api/v1/call-history?page=0&size=20' | python3 -m json.tool

# Custom page size
curl -s 'http://localhost:8080/api/v1/call-history?page=0&size=5'

# Invalid pagination
curl -s 'http://localhost:8080/api/v1/call-history?page=-1&size=20'
# → 400
```

### 5. Provider Failure — 503 Service Unavailable

Activate the failure transport so the percentage provider returns 503 on every
attempt, exhausting all retries:

```bash
# Stop the running stack first
make docker-down

# Start with failure transport enabled
PERCENTAGE_PROVIDER_TRANSPORT_MODE=failure docker compose up --build

# Any calculation request will now return 503 after 3 failed attempts
curl -s -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"num1":5,"num2":5}'
# → 503 {"status":503,"error":"Service Unavailable","message":"Percentage provider is unavailable"}
```

The `PERCENTAGE_PROVIDER_TRANSPORT_MODE` property maps to
`percentage-provider.transport.mode` in `application.yml`. Supported values:

| Value     | Behavior                                                     |
|-----------|--------------------------------------------------------------|
| `mock`    | Returns `{"percentage":10}` on every call (default)          |
| `failure` | Returns `503 Service Unavailable` on every call              |

This mechanism is also used by the E2E test `CalculationProviderFailureE2ETest`
to validate retry exhaustion without depending on an external service failure.

## API Documentation

Swagger UI and the generated OpenAPI document are available after the
application starts:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

A Postman collection is included at:

```text
docs/postman/tenpo-challenge.postman_collection.json
```

Import it in Postman and use the default `baseUrl=http://localhost:8080`.

## Test Suite & Coverage

### Test Types

| Layer          | Category              | What Is Tested                                              |
|----------------|-----------------------|-------------------------------------------------------------|
| `domain`       | Unit tests            | `PercentageCalculator`, `CalculationInput`, `CalculationResult` |
| `application`  | Unit tests            | All use-case services, rate-limit decisions, pagination, commands |
| `api`          | MockMvc tests         | Controllers (calculation, call history, pagination validation) |
| `api`          | E2E tests             | Full HTTP → service → persistence path with Testcontainers PostgreSQL |
| `config`       | Unit tests            | Property binding, async executor, retry wiring              |
| `infra`        | Unit + integration    | Retry behavior, IP resolution, rate limiter adapter, call-history filter (exclusion, recording, rate-limit integration) |
| `persistence`  | Unit tests            | JPA repository, persistence mappers, query/command adapters |
| Root           | ArchUnit tests        | Hexagonal boundaries: domain purity, no cycles, ports are interfaces, services in `application.service` |

### E2E Tests (Testcontainers)

E2E tests run the full Spring Boot context against a real PostgreSQL container
and exercise the HTTP, service, persistence, and error-handling stack:

- **`CalculationPercentageE2ETest`**: POST `/api/v1/calculations` with the
  default mock transport (returns `10`) — asserts full response body.
- **`CalculationProviderFailureE2ETest`**: POST with `transport.mode=failure`
  — asserts 3 provider attempts are attempted and `503` is returned.
- **`CalculationE2ETest`**: POST then GET `/api/v1/call-history` — verifies
  the async record is persisted within a timeout.
- **`CallHistoryE2ETest`**: seed PostgreSQL directly, query via API, verify
  pagination metadata against the real database.

### Architecture Enforcement

Architecture boundaries are enforced at two levels:

- **ArchUnit tests** (`ArchitectureTest`): validate at bytecode level that
  `domain` has no Spring/JPA dependencies, `application` has no web/persistence
  imports, `api` never references `persistence`, ports are interfaces, services
  reside in `application.service`, and packages form no cycles.
- **Shell script** (`check-architecture-boundaries.sh`): fast `grep`-based
  scan of import statements, integrated into `make verify` for CI.

### Running Tests

```bash
make test       # run all unit + integration + E2E tests
make coverage   # run mvn verify (tests + JaCoCo + Checkstyle + PMD + SpotBugs + Enforcer)
                # JaCoCo configured with minimum 0.90 line coverage at bundle level
make verify     # mvn clean verify + architecture check + private-artifact check + README command check
make lint       # Checkstyle + PMD + SpotBugs
make build      # package the application
```

## Project Structure

```text
src/main/java/com/tenpo/challenge/
├── api
│   ├── calculation       # Calculation endpoint, request and response DTOs
│   ├── callhistory       # HTTP endpoints and DTOs for paginated history
│   └── ratelimit         # HTTP rate-limit filter and key resolution
├── application
│   ├── port
│   │   ├── in            # Use-case contracts
│   │   └── out           # Output ports
│   ├── service           # Use-case implementations
│   ├── callhistory       # Application models for history recording/querying
│   └── ratelimit         # Application models for rate limiting
├── config                # Composition root, Spring wiring and property binding
├── domain                # Pure domain model and business rules (no frameworks)
├── external
│   └── percentage        # Percentage provider adapter
├── infrastructure
│   ├── callhistory       # Async history recorder and HTTP filter
│   ├── ratelimit         # Bucket4j-backed rate-limiting adapter
│   └── ...               # Other technical adapters
└── persistence
    └── callhistory       # PostgreSQL/JPA persistence adapter
```

Application and domain code are kept independent from Spring Web, JPA,
PostgreSQL, Bucket4j, and HTTP client details.

## Decisiones Técnicas

Justificación de las principales decisiones de arquitectura y diseño.

### Mock HTTP Transport — Simulación cercana a la red

El adaptador de porcentaje está mockeado a nivel de `ClientHttpRequestFactory`
(`MockPercentageHttpTransport`), la capa más baja del stack HTTP de Spring. Esto
permite que los tests E2E ejerciten el recorrido completo — `RestClient`,
Resilience4j `Retry`, deserialización de `PercentageResponse` — de forma
idéntica a como lo harían contra un servicio externo real.

Si el mock se hiciera a nivel de `PercentageFetcher` o `PercentageProviderPort`,
los tests no validarían el comportamiento del `RestClient`, la configuración de
retry, ni el manejo de timeouts. Al interceptar en la capa de transporte HTTP,
el único componente que se reemplaza es la conexión de red; todo lo demás corre
con su implementación real.

El transporte `FailingPercentageHttpTransport` complementa esta estrategia:
devuelve `503` en cada intento, permitiendo probar el agotamiento de retries y
la respuesta de error de la API sin depender de un servicio externo caído.

La selección del transporte se controla con la propiedad
`percentage-provider.transport.mode` (por defecto `mock`).

### Exponential Backoff en Retry con Resilience4j

El mecanismo de retry utiliza `IntervalFunction.ofExponentialBackoff()` de
Resilience4j con 3 intentos totales (intento inicial + 2 reintentos) y
multiplicador 2.0. Los intervalos entre reintentos siguen la secuencia
**100ms → 200ms** (el intento inicial no tiene delay).

Se eligió backoff exponencial sobre delay fijo por dos razones:

1. **Evitar el thundering herd**: si el servicio externo está saturado o
   reiniciándose, reintentos a intervalos fijos lo golpean con la misma
   intensidad. El backoff exponencial le da tiempo creciente para recuperarse.

2. **Reducir contención en degradación parcial**: si varias instancias de la
   API comparten el mismo provider, delays fijos sincronizan los reintentos.
   El factor aleatorio implícito del backoff exponencial dispersa naturalmente
   los reintentos.

Solo se reintentan fallos transitorios (`IOException` y
`RestClientException`). Errores permanentes como `IllegalArgumentException`
o `NullPointerException` no se reintentan.

La configuración es externalizada: `maxAttempts`, `initialBackoff` y
`backoffMultiplier` se ajustan desde `application.yml` sin recompilar.

### Historial de Llamadas — Captura HTTP, Persistencia Asíncrona y Virtual Threads

El registro de llamadas se apoya en tres decisiones que se refuerzan entre sí:

**¿Por qué un `Filter` y no un interceptor?**

El historial debe capturar la totalidad del intercambio HTTP: método, URI,
query string, body del request, body de la respuesta, status code, duración
e IP del cliente. Los interceptores de Spring (`HandlerInterceptor`) operan
a nivel de controller y no tienen acceso al cuerpo de la respuesta ni al
status HTTP real. Un `OncePerRequestFilter` con `ContentCachingRequestWrapper`
y `ContentCachingResponseWrapper` envuelve el request/response en la frontera
HTTP, permitiendo leer los cuerpos después de que el controller los haya
consumido y escrito.

Corre con `Ordered.HIGHEST_PRECEDENCE` para envolver toda la cadena de filtros,
incluyendo el rate limit, antes de que cualquier otro componente toque el
request.

**¿Por qué asíncrono (fire-and-forget)?**

La respuesta al cliente no debe esperar la escritura en PostgreSQL. El método
`record()` está anotado con `@Async("callHistoryExecutor")`; Spring lo despacha
a un executor dedicado y el `CallHistoryFilter` retorna inmediatamente después
de disparar la tarea. Si la persistencia falla, el error se loguea en warning
y la respuesta HTTP original no se altera. Esto mantiene la latencia de la API
independiente del estado de la base de datos.

**¿Por qué virtual threads con límite de concurrencia?**

Java 21 introduce virtual threads (Project Loom), que son órdenes de magnitud
más ligeros que los threads de plataforma: se pueden lanzar millones sin agotar
el scheduler del SO. Spring's `SimpleAsyncTaskExecutor` con
`setVirtualThreads(true)` ejecuta cada tarea de historial en un virtual thread.

Sin embargo, sin un límite de concurrencia, una ráfaga de requests dispararía
cientos de escrituras concurrentes a PostgreSQL, compitiendo por conexiones del
pool. El `concurrencyLimit` (por defecto `2`) hace que Spring encole las tareas
excedentes, protegiendo la base de datos. Es virtual threads para liviandad
de scheduling + límite de concurrencia para backpressure.

**Endpoints excluidos:** Swagger UI, OpenAPI docs, actuator, el propio endpoint
de call-history y `/favicon.ico` se excluyen del historial para no generar ruido
ni loops de auto-registro.

**Consideración para producción:** el modelo fire-and-forget es best-effort.
Para un sistema que requiera auditoría garantizada, un **outbox pattern**
(escribir el registro en una tabla de outbox dentro de la misma transacción,
con un publicador que lo entregue a un message broker) reemplazaría el enfoque
actual.

### Rate Limit — Filtro HTTP, Políticas por Path/IP y Escalabilidad

**¿Por qué un `Filter` y no un interceptor?**

El rate limit debe rechazar requests antes de que consuman recursos de
aplicación. Un `OncePerRequestFilter` con orden `HIGHEST_PRECEDENCE + 1`
(solo detrás del `CallHistoryFilter`) inspecciona el request apenas entra
al container y devuelve `429 Too Many Requests` sin que el `DispatcherServlet`
ni los controllers lleguen a ejecutarse. Un interceptor correría después del
mapeo de ruta y la validación, desperdiciando CPU en requests que iban a ser
rechazadas igual.

**¿Por qué rate limiting por IP?**

Es la forma más simple y efectiva de fair usage para una API sin autenticación.
La resolución de IP está abstraída detrás de `ClientIpResolver`: la
implementación por defecto (`ForwardedForClientIpResolver`) lee el header
`X-Forwarded-For` cuando la API está detrás de un reverse proxy, y usa
`getRemoteAddr()` como fallback. Cambiar a API keys o JWT claims solo requiere
una implementación alternativa de la interfaz.

**¿Por qué políticas por path?**

Los endpoints técnicos (Swagger, OpenAPI, actuator) tienen un límite de 60 RPM
para que nunca consuman la cuota funcional de 3 RPM. El `PathBasedRateLimitPolicyResolver`
resuelve la política por URI del request usando Ant-style matching. La clave del
bucket incluye tanto la IP como la política, por lo que todos los endpoints
funcionales comparten la misma cuota.

**¿Por qué Caffeine cache (y no `ConcurrentHashMap` o Redis)?**

Un `ConcurrentHashMap` no expira entradas: IPs que hacen 3 requests y nunca
vuelven acumularían buckets indefinidamente, generando un memory leak.
Caffeine con `expireAfterAccess` (TTL por defecto 2 minutos) evicta
automáticamente los buckets inactivos.

Redis no se introdujo porque el scope actual es single-instance y agregar
infraestructura externa para una cache de rate limit iba en contra del
principio de simplicidad. La abstracción `RateLimiterPort` permite swapear
Caffeine por Redis sin tocar el filtro, el servicio ni el policy resolver.

**Escalabilidad a producción multi-instancia:**

Con Caffeine, cada instancia mantiene sus propios buckets. Un cliente que
pega 3 requests a la instancia A y 3 a la instancia B consume 6 RPM en total
en lugar de 3. Para corregir esto en un despliegue horizontal, se implementa
un `RateLimiterPort` backed by Redis (Bucket4j tiene soporte nativo para
Redis y Hazelcast). El resto de la arquitectura — `RateLimitFilter`,
`CheckRateLimitUseCase`, `PathBasedRateLimitPolicyResolver`, `ClientIpResolver`
— no requiere cambios. Las políticas por path, la resolución de IP y los
headers de respuesta funcionan idéntico.

## AI-Assisted Development

AI tools were used during the development of this challenge as support for
brainstorming, documentation review, test-case ideation, and architectural
discussion.

The final implementation, design decisions, code validation, and delivery
criteria were reviewed and verified by the author. All documented commands
and tests are intended to be reproducible locally by the evaluator.

## Health Checks

The API exposes a health endpoint via Spring Boot Actuator at
`/actuator/health`. Docker Compose uses it with:

- **interval**: 10 seconds between checks
- **timeout**: 5 seconds per check attempt
- **retries**: 5 consecutive failures before marking unhealthy
- **start_period**: 30 seconds grace for Flyway migrations and connection pool warmup

PostgreSQL has its own healthcheck (`pg_isready`), and the API container waits
for PostgreSQL to be healthy before starting.

## Non-Root Container User

The Docker image runs the application as a dedicated `app` system user instead
of `root`. The user is created in the Dockerfile with no shell (`/bin/false`)
and no home directory, and the application jar is owned by that user.
