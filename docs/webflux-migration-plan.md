# WebFlux Migration Plan (v2)

## Reglas de ejecución

- **NO commitear** sin aprobación explícita. Los cambios quedan en working tree para revisión.
- Detenerse al final de cada fase para validar antes de continuar.
- `mvn verify` verde antes de considerar una fase completada.

---

## Objective

Migrar el servicio a Spring WebFlux en `feature/webflux-migration`, manteniendo `main` intacto y moviendo un caso de uso a la vez. La migración preserva el comportamiento funcional actual, el modelo hexagonal, y las abstracciones existentes.

---

## Estrategia: Aditiva, no Sustractiva

- **Phase 1-4:** WebFlux se agrega **junto a** MVC. Ambos conviven. Cada ruta se migra y verifica antes de remover su contraparte servlet.
- **Phase 5:** Se remueve MVC, JPA, HikariCP y todo residual del stack bloqueante.

---

## Architecture Rules

1. `domain` — puro, sin frameworks, sin `Mono`/`Flux`
2. `application` — solo use cases y ports, sin dependencias a `jakarta.servlet`, `reactor.core.publisher`, ni web/persistence
3. `infrastructure` — adapters WebFlux, `WebClient`, `WebFilter`, R2DBC. No debe depender de `..api..`
4. `api` — controllers, DTOs, error mapping. No debe depender de `..persistence..`
5. Las abstracciones existentes (ports, use cases, factories, resolvers) se **preservan, adaptan o reubican**. Nunca se eliminan sin reemplazo equivalente.
6. `Mono`/`Flux` solo aparecen en la frontera reactiva (ports que involucran I/O, controllers, adapters)
7. Tipos servlet (`HttpServletRequest`, `HttpServletResponse`, `OncePerRequestFilter`, `MockMvc`) desaparecen de las rutas migradas
8. JPA desaparece de rutas migradas a R2DBC

---

## Abstractions Preservation Contract

Esta sección es de cumplimiento obligatorio. Debe validarse al finalizar **cada phase** y en la verificación final.

### 1. Ports y Use Cases — checklist de preservación

| Abstracción | Package | Preservada | Cambio | Verificado |
|---|---|---|---|---|
| `CalculateWithPercentageUseCase` | `application.port.in` | ✓ | `Mono<CalculationResult>` | ☐ |
| `CheckRateLimitUseCase` | `application.port.in` | ✓ | Sin cambios (CPU-bound) | ☐ |
| `RecordCallHistoryUseCase` | `application.port.in` | ✓ | `Mono<Void>` | ☐ |
| `GetCallHistoryUseCase` | `application.port.in` | ✓ | `Mono<CallHistoryPage>` | ☐ |
| `PercentageProviderPort` | `application.port.out` | ✓ | `Mono<BigDecimal>` | ☐ |
| `RateLimiterPort` | `application.port.out` | ✓ | Sin cambios | ☐ |
| `RateLimitPolicyResolver` | `application.port.out` | ✓ | Sin cambios | ☐ |
| `CallHistoryPersistencePort` | `application.port.out` | ✓ | `Mono<Void>` | ☐ |
| `CallHistoryQueryPort` | `application.port.out` | ✓ | `Mono<CallHistoryPage>` | ☐ |

### 2. Abstracciones reubicadas (ganan neutralidad HTTP)

| Abstracción | De | A | Firma final | Verificado |
|---|---|---|---|---|
| `ClientIpResolver` | `api.ratelimit` | `application.port.out` | `String resolve(ServerHttpRequest)` | ☐ |
| `RateLimitKeyResolver` | `api.ratelimit` | `application.port.out` | `RateLimitKey resolve(ServerHttpRequest)` | ☐ |

**Regla:** Ambas interfaces dejan de depender de `jakarta.servlet.HttpServletRequest`. Las implementaciones (`ForwardedForClientIpResolver`, `IpRateLimitKeyResolver`) se adaptan a `ServerHttpRequest`.

### 3. Factories → Beans inyectables con estrategia intercambiable

| Factory original | Abstracción migrada | Implementación base | Estrategia reemplazable |
|---|---|---|---|
| `RetryFactory` (static) | `RetryStrategy` (interface) | `ExponentialBackoffRetryStrategy` | ✓ — `FixedDelayRetryStrategy`, `NoRetryStrategy`, etc. |
| `RestClientFactory` (static) | `HttpClientFactory` (interface) | `WebClientFactory` | ✓ — mockeable, codecs/timeouts configurables |

### 4. Abstracciones de infraestructura preservadas

| Abstracción | Firma actual | Firma migrada | Verificado |
|---|---|---|---|
| `PercentageFetcher` (`@FunctionalInterface`) | `PercentageResponse fetch()` | `Mono<PercentageResponse> fetch()` | ☐ |
| `CallHistoryRecorder` | `void record(Command)` | `Mono<Void> record(Command)` | ☐ |

### 5. Implementaciones — cambio interno, abstracción intacta

| Implementación | Cambio interno | Puerto preservado | Verificado |
|---|---|---|---|
| `RetryingPercentageFetcher` | `Retry.decorateSupplier` → `retryStrategy.apply(mono)` | `PercentageFetcher` | ☐ |
| `ExternalPercentageProvider` | `fetch()` → `fetch().map(...)` | `PercentageProviderPort` | ☐ |
| `CalculateWithPercentageService` | Síncrono → `.flatMap().map()` | `CalculateWithPercentageUseCase` | ☐ |
| `Bucket4jRateLimiterAdapter` | Sin cambios | `RateLimiterPort` | ☐ |
| `PathBasedRateLimitPolicyResolver` | Sin cambios | `RateLimitPolicyResolver` | ☐ |
| `AsyncCallHistoryRecorder` | `@Async` → `subscribeOn(Schedulers.boundedElastic())` | `CallHistoryRecorder` | ☐ |
| `RateLimitFilter` | `OncePerRequestFilter` → `WebFilter` | Bean con `@Order` | ☐ |
| `CallHistoryFilter` | `OncePerRequestFilter` → `WebFilter` | Bean con `@Order` | ☐ |
| `CallHistoryCommandAdapter` | `saveAndFlush` → `save().then()` | `CallHistoryPersistencePort` | ☐ |
| `CallHistoryQueryAdapter` | `findAll(Pageable)` → `count()` + `select().page()` | `CallHistoryQueryPort` | ☐ |

### 6. Domain — cero cambios

| Clase | Tipo | Cambios | Verificado |
|---|---|---|---|
| `CalculationInput` | Record | Ninguno | ☐ |
| `CalculationResult` | Record | Ninguno | ☐ |
| `PercentageCalculator` | Class | Ninguno | ☐ |
| `RateLimitDecision` | Record | Ninguno | ☐ |
| `RateLimitKey` | Record | Ninguno | ☐ |
| `RateLimitPolicy` | Record | Ninguno | ☐ |
| `CallHistoryEntry` | Record | Ninguno | ☐ |
| `CallHistoryPage` | Record | Ninguno | ☐ |
| `PaginationRequest` | Record | Ninguno | ☐ |
| `RecordCallHistoryCommand` | Record | Ninguno | ☐ |
| `InvalidPaginationException` | Exception | Ninguno | ☐ |

### 7. API — DTOs, mappers y error handler sin cambios

| Clase | Cambios | Verificado |
|---|---|---|
| `CalculationRequest` | Ninguno | ☐ |
| `CalculationResponse` | Ninguno | ☐ |
| `CallHistoryResponse` | Ninguno | ☐ |
| `CallHistoryPageResponse` | Ninguno | ☐ |
| `ErrorResponse` | Ninguno | ☐ |
| `CallHistoryWebMapper` | Ninguno | ☐ |
| `GlobalExceptionHandler` | Compatible nativamente con WebFlux | ☐ |

### 8. Controller output

Los controllers devuelven `Mono<ResponseEntity<T>>` para JSON estándar. **No se usa SSE** (`text/event-stream`) porque la API es REST puntual, no streaming en tiempo real.

---

## Branch Strategy

- Crear `feature/webflux-migration` desde `main`
- Mantener `main` intacto durante toda la migración
- Hacer merge solo cuando todas las fases estén completas y verificadas

---

## Testing and Coverage Rules

- No borrar tests sin agregar reemplazo equivalente o superior
- `MockMvc` → `WebTestClient`, tests de filtro servlet → tests de `WebFilter`
- Coverage baseline se registra en Phase 0
- Coverage final ≥ baseline
- `StepVerifier` para tests de puertos reactivos

---

## Phase 0: Safety Net

### Objetivo

Congelar el comportamiento actual, fortalecer los tests de regresión, y establecer la línea base de cobertura.

### Tasks

1. Identificar módulos que dependen de servlet, JPA, o infraestructura web bloqueante
2. Fortalecer tests de arquitectura (ArchUnit) para que el código de aplicación no pueda depender de clases web o de persistencia
3. Inventariar interfaces y puertos actuales como contratos a preservar
4. Ejecutar suite completa y registrar coverage baseline con JaCoCo
5. Inventariar escenarios de test actuales por área: cálculo, validación, fallo de provider, retry, rate limit, call history, persistencia, OpenAPI/endpoints técnicos, y arquitectura

### Exit criteria

- [ ] Comportamiento actual documentado por tests
- [ ] Violaciones de límites prevenidas por tests o checks
- [ ] Coverage baseline registrada antes de migración
- [ ] Interfaces y puertos listados antes de cualquier cambio de firma

---

## Phase 0.5: Pre-Migration Structural Cleanup

### Objetivo

Desacoplar el código del modelo servlet y refactorizar static factories **antes** de introducir WebFlux, preservando todas las abstracciones existentes.

### Tasks

1. **Reubicar `ClientIpResolver`**: de `api.ratelimit` → `application.port.out`. Cambiar firma para que no dependa de `HttpServletRequest`. Crear adapter intermedio temporal si es necesario.

2. **Reubicar `RateLimitKeyResolver`**: de `api.ratelimit` → `application.port.out`. Mismo tratamiento.

3. **Refactorizar `RestClientFactory`**: de static utility a interfaz `HttpClientFactory` + implementación bean. En esta fase sigue produciendo `RestClient`.

4. **Refactorizar `RetryFactory`**: de static utility a interfaz `RetryStrategy` + implementación `ExponentialBackoffRetryStrategy`. La estrategia es reemplazable.

```java
// application/port/out/RetryStrategy.java
@FunctionalInterface
public interface RetryStrategy {
    <T> Mono<T> apply(Mono<T> source);
}
```

```java
// infrastructure/ExponentialBackoffRetryStrategy.java
public class ExponentialBackoffRetryStrategy implements RetryStrategy {
    private final int maxAttempts;
    private final Duration initialBackoff;
    private final double multiplier;

    @Override
    public <T> Mono<T> apply(Mono<T> source) {
        return source.retryWhen(
            Retry.backoff(maxAttempts, initialBackoff)
                .maxBackoff(initialBackoff.multipliedBy((long) Math.pow(multiplier, maxAttempts)))
                .filter(throwable -> throwable instanceof IOException
                    || throwable instanceof WebClientResponseException));
    }
}
```

5. **Fortalecer ArchUnit**:
   - `domain` no depende de `reactor.core.publisher`
   - `application` no depende de `reactor.core.publisher`
   - `infrastructure` no depende de `..api..`

### Exit criteria

- [ ] `ClientIpResolver` y `RateLimitKeyResolver` no importan `jakarta.servlet`
- [ ] `RestClientFactory` es interfaz + bean, no static
- [ ] `RetryFactory` es interfaz `RetryStrategy` + implementación inyectable
- [ ] ArchUnit tests pasan con nuevas reglas
- [ ] Tests existentes verdes

### Abstractions check

Validar secciones 2, 3 y 4 del Preservation Contract.

---

## Phase 1: WebFlux Foundation (ADITIVA)

### Dependencias

**Agregar:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```
**Reemplazar:**
```xml
<!-- QUITAR -->
<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
<!-- AGREGAR -->
<artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
```

**NO se quita `spring-boot-starter-web` en esta fase.**

### Tasks

1. Agregar dependencia WebFlux
2. Reemplazar springdoc starter
3. Migrar `CalculationController` → `Mono<ResponseEntity<...>>`
4. Migrar `CallHistoryController` → `Mono<ResponseEntity<...>>`
5. Reemplazar `MockMvc` por `WebTestClient` en controller tests

### Lo que NO se toca

- `RateLimitFilter` (sigue `OncePerRequestFilter`, convive con WebFlux)
- `CallHistoryFilter` (sigue `OncePerRequestFilter`, convive)
- `RestClient` (se migra en Phase 2)
- JPA (se migra en Phase 4)
- `@Async` (se migra en Phase 4)

### Exit criteria

- [ ] App arranca con ambos starters (MVC + WebFlux)
- [ ] Controllers devuelven tipos reactivos
- [ ] `WebTestClient` reemplaza `MockMvc` en controller tests
- [ ] Swagger UI funciona
- [ ] Tests verdes, coverage sin baja

### Abstractions check

Validar secciones 7 y 8 del Preservation Contract.

---

## Phase 2: Calculation Use Case

### Implementation order

1. input port
2. output port
3. use case/service
4. external adapter
5. controller
6. error handling
7. tests

### Tasks

1. Reemplazar `RestClient` por `WebClient` en `PercentageRestClientConfig`
2. `HttpClientFactory` → produce `WebClient`
3. `RetryConfig` → produce `RetryStrategy` (ya refactorizado en 0.5.4)
4. Remover dependencia `resilience4j-spring-boot3`
5. `RetryingPercentageFetcher`: `Retry.decorateSupplier` → `retryStrategy.apply(webClientMono)`
6. `PercentageProviderPort` → `Mono<BigDecimal> getPercentage()`
7. `CalculateWithPercentageUseCase` → `Mono<CalculationResult> calculate(Input)`
8. `CalculateWithPercentageService` → composición reactiva con `.flatMap().map()`
9. `MockPercentageHttpTransport` → `MockWebServer` (OkHttp)
10. Migrar tests: `MockMvc` → `WebTestClient` para cálculo

### Exit criteria

- [ ] `POST /api/v1/calculations` funciona con `WebClient` reactivo
- [ ] Retry es composición reactiva (`RetryStrategy.apply()`)
- [ ] `Resilience4j` removido del classpath
- [ ] Domain sin tipos reactivos
- [ ] Tests verdes, coverage preservado

### Abstractions check

Validar secciones 1 (filas `CalculateWithPercentageUseCase`, `PercentageProviderPort`), 3, 4 (`PercentageFetcher`), 5 (filas `RetryingPercentageFetcher`, `ExternalPercentageProvider`, `CalculateWithPercentageService`), y 6 del Preservation Contract.

---

## Phase 3: Rate Limit

### Tasks

1. `RateLimitFilter` → `RateLimitWebFilter` (implementa `WebFilter`, `@Order(HIGHEST_PRECEDENCE + 1)`)
2. `ClientIpResolver.resolve(ServerHttpRequest)` — adapter implementa con `ServerHttpRequest`
3. `RateLimitKeyResolver.resolve(ServerHttpRequest)` — adapter implementa con `ServerHttpRequest`
4. Escritura de rechazo: `response.getWriter()` → `response.writeWith(Mono.just(buffer))`
5. Headers: `response.setHeader(...)` → `response.getHeaders().set(...)`
6. `RateLimiterPort` y `CheckRateLimitUseCase` — sin cambios (CPU-bound, síncrono correcto)
7. `Bucket4jRateLimiterAdapter` — sin cambios (Caffeine + Bucket4j, nanosegundos)
8. Migrar tests: `MockHttpServletRequest` → `MockServerHttpRequest`

### Exit criteria

- [ ] Rate limit funciona en pipeline reactivo
- [ ] `OncePerRequestFilter` removido de ruta rate-limit
- [ ] Sin dependencias `jakarta.servlet` en ruta rate-limit
- [ ] Headers `X-RateLimit-*` y `Retry-After` preservados

### Abstractions check

Validar secciones 1 (filas `CheckRateLimitUseCase`, `RateLimiterPort`, `RateLimitPolicyResolver`), 2, 5 (filas `RateLimitFilter`, `Bucket4jRateLimiterAdapter`, `PathBasedRateLimitPolicyResolver`) del Preservation Contract.

---

## Phase 4: Call History

### Implementation order

1. reactive filter
2. ports
3. service layer
4. persistence adapter
5. repository/query implementation
6. tests

### 4.1 Filtro → WebFilter

- `CallHistoryFilter` (`OncePerRequestFilter`) → `CallHistoryWebFilter` (`WebFilter`, `@Order(HIGHEST_PRECEDENCE)`)
- `ContentCachingRequestWrapper` → `ServerHttpRequestDecorator` + `DataBufferUtils.join()`
- `ContentCachingResponseWrapper` → `ServerHttpResponseDecorator` + captura reactiva
- `EXCLUDED_PATHS` se preserva vía path matching en el WebFilter

### 4.2 Persistencia → R2DBC

**Dependencias:**
```xml
<!-- AGREGAR -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>
```

**Configuración dual (Flyway JDBC + R2DBC runtime):**
```yaml
spring:
  flyway:
    enabled: true
  datasource:               # SOLO para Flyway (JDBC)
    url: jdbc:postgresql://...
  r2dbc:                    # Runtime persistence
    url: r2dbc:postgresql://...
  jpa:                      # REMOVER al final
    open-in-view: false
    hibernate:
      ddl-auto: validate
```

**Cambios en capa de persistencia:**
- `CallHistoryEntity`: `@Entity` (jakarta) → `@Table("call_history")` (Spring Data R2DBC), `@Id` (jakarta) → `@Id` (Spring Data), `@Column` (jakarta) → `@Column` (Spring Data R2DBC)
- `CallHistoryRepository`: `JpaRepository` → `ReactiveCrudRepository<CallHistoryEntity, UUID>`
- `CallHistoryCommandAdapter`: `saveAndFlush(...)` → `save(...).then()` (devuelve `Mono<Void>`)
- `CallHistoryQueryAdapter`: `findAll(Pageable)` → `R2dbcEntityTemplate` con `count()` + `select().from("call_history").page(pageable)` ordenado por `occurred_at DESC, id DESC`

### 4.3 `@Async` → Reactive Scheduler

- `AsyncCallHistoryRecorder.record()` → `Mono<Void>` con `Mono.fromRunnable(...).subscribeOn(Schedulers.boundedElastic())`
- Remover `@EnableAsync`, `SimpleAsyncTaskExecutor`, `CallHistoryAsyncProperties`, `CallHistoryAsyncExecutorConfigTest`

### 4.4 Puertos reactivos

```java
public interface CallHistoryPersistencePort {
    Mono<Void> save(RecordCallHistoryCommand command);
}

public interface CallHistoryQueryPort {
    Mono<CallHistoryPage> findPage(PaginationRequest request);
}
```

### Exit criteria

- [ ] Historia se graba con R2DBC
- [ ] Paginación funciona con `count()` + `select().page()` reactivos
- [ ] `@Async` y virtual threads removidos
- [ ] `OncePerRequestFilter` removido de ruta call-history
- [ ] Flyway sigue funcionando con `DataSource` JDBC separado
- [ ] Tests migrados, coverage preservado

### Abstractions check

Validar secciones 1 (filas `RecordCallHistoryUseCase`, `GetCallHistoryUseCase`, `CallHistoryPersistencePort`, `CallHistoryQueryPort`), 4 (`CallHistoryRecorder`), 5 (filas `AsyncCallHistoryRecorder`, `CallHistoryFilter`, `CallHistoryCommandAdapter`, `CallHistoryQueryAdapter`), y 6 del Preservation Contract.

---

## Phase 5: Cleanup and Hardening

### Tasks

1. Remover `spring-boot-starter-web`
2. Remover `spring-boot-starter-data-jpa`
3. Simplificar HikariCP (solo queda un `DataSource` mínimo para Flyway)
4. Remover propiedades `spring.jpa.*` y `spring.datasource.hikari.*`
5. **`docker-compose.yml`:**
   - Agregar `SPRING_R2DBC_URL: r2dbc:postgresql://postgres:5432/tenpo_challenge`
   - Remover `SPRING_DATASOURCE_HIKARI_*`
   - Conservar `SPRING_DATASOURCE_URL` (Flyway)
6. **`docker-compose.hub.yml`:**
   - Mismos cambios de variables de entorno
   - Cambiar `image: marcev/tenpo-challenge:latest` → `image: marcev/tenpo-challenge-webflux:latest`
7. Revisar imports y límites de paquetes
8. Ejecutar suite completa de verificación (`mvn verify`)

### Docker build & push

```bash
docker compose build api
docker tag tenpo-challenge-api marcev/tenpo-challenge-webflux:latest
docker push marcev/tenpo-challenge-webflux:latest
```

### Exit criteria

- [ ] Código 100% WebFlux, sin MVC/JPA/Servlet
- [ ] ArchUnit verde (sin dependencias a servlet, JPA, Hibernate, `jakarta.persistence`)
- [ ] Coverage ≥ baseline Phase 0
- [ ] Build verde (`mvn verify` completo)

### Pendientes post-Phase 5

1. **GlobalExceptionHandler**: adaptar manejo de errores para WebFlux. MVC usa `MethodArgumentNotValidException`, WebFlux usa `WebExchangeBindException`. Ya se agregó handler para `WebExchangeBindException` pero quedan 5 tests fallando:
   - `rejectsEmptyBody`: body vacío → en WebFlux lanza excepción distinta
   - `rejectsNonNumericNum1`: tipo inválido → `HttpMessageNotReadableException` no se atrapa igual
   - `rejectsMalformedJson`: JSON malformado → misma causa
   - `rejectsNonNumericSizeBeforeUseCaseIsReached`: parámetro no numérico → `MethodArgumentTypeMismatchException` tiene comportamiento distinto
   - `rejectsNonNumericPageBeforeUseCaseIsReached`: ídem
2. **Tests de persistencia R2DBC**: migrar de `@DataJpaTest` a `@DataR2dbcTest` y reescribir los 4 tests eliminados (`CallHistoryCommandAdapterTest`, `CallHistoryQueryAdapterTest`, `CallHistoryQueryAdapterPaginationTest`, `CallHistoryPersistenceMapperTest`)
3. **CallHistoryWebFilter test**: crear tests unitarios para el nuevo `CallHistoryWebFilter`
4. **Tests de integración deshabilitados**: reactivar y migrar `CalculationE2ETest`, `CallHistoryRecordingIntegrationTest` con el stack 100% WebFlux
5. **docker-compose**: actualizar `docker-compose.yml` y `docker-compose.hub.yml` con `SPRING_R2DBC_URL` y remover `SPRING_DATASOURCE_HIKARI_*`
6. **Imagen Docker**: cambiar tag a `marcev/tenpo-challenge-webflux:latest`

### Abstractions check

Validar **todas** las secciones del Preservation Contract. Todos los ☐ deben estar en ✓.

---

## Migration Sequence (resumen)

| Fase | Nombre | Tipo |
|---|---|---|
| 0 | Safety Net | Análisis |
| 0.5 | Pre-Migration Structural Cleanup | Refactor (sin WebFlux) |
| 1 | WebFlux Foundation | Aditiva |
| 2 | Calculation Use Case | Migración |
| 3 | Rate Limit | Migración |
| 4 | Call History | Migración |
| 5 | Cleanup and Hardening | Sustractiva (remueve MVC) |

---

## Dependencias removidas por fase

| Dependencia | Fase |
|---|---|
| `resilience4j-spring-boot3` | Phase 2 |
| `springdoc-openapi-starter-webmvc-ui` | Phase 1 |
| `spring-boot-starter-data-jpa` | Phase 5 |
| `spring-boot-starter-web` | Phase 5 |

---

## Docker & Deployment

| Archivo | Cambio |
|---|---|
| `Dockerfile` | Sin cambios |
| `docker-compose.yml` | Phase 5: agregar `SPRING_R2DBC_URL`, remover `HIKARI_*` |
| `docker-compose.hub.yml` | Phase 5: mismos cambios + `image: marcev/tenpo-challenge-webflux:latest` |

---

## Validation Per Slice

- Build and test después de cada fase
- No avanzar si la fase actual no compila o tiene tests rojos
- Commits pequeños y revisables
- Validar Preservation Contract al final de cada fase
- Coverage drops → tratarlos como regresiones
- Detenerse al final de cada fase para validación manual antes de continuar
