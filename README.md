# Órdenes de soporte

Solución fullstack pequeña para que una aplicación móvil registre órdenes de soporte y un operador
las consulte y actualice su estado.

| Componente | Tecnología |
|---|---|
| API REST | Java 17+, Spring Boot 3.5, Spring Data JPA, Bean Validation |
| Base de datos | H2 en modo archivo (por defecto) · PostgreSQL opcional · migraciones con Flyway |
| Frontend | React 19 + TypeScript + Vite |

```
.
├── backend/             API Spring Boot
├── frontend/            Interfaz web del operador
├── api.http             Ejemplos de consumo de la API
├── docker-compose.yml   PostgreSQL opcional
└── README.md
```

## Requisitos

- JDK 17 o superior (no hace falta instalar Maven: se incluye `mvnw`)
- Node.js 20.19+ / 22.12+ y npm
- Docker, solo si se quiere usar PostgreSQL

## Cómo ejecutar

### Backend

```bash
cd backend
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080/api/support-orders`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`. En desarrollo Vite redirige `/api` al backend (puerto 8080), así que no
hace falta configurar CORS. El cliente admite `VITE_API_URL` para apuntar a otro host, pero en ese caso
habría que habilitar CORS en el backend para el origen del frontend (no configurado en este ejercicio).

Build de producción: `npm run build` (incluye la verificación de tipos con `tsc`). Lint: `npm run lint`.

## Cómo ejecutar las pruebas

```bash
cd backend
./mvnw test                 # Windows: mvnw.cmd test
```

31 pruebas: unitarias (política de transiciones, servicio con Mockito) y de integración (repositorio
contra H2 con el esquema real de Flyway, y la API completa por HTTP con MockMvc). Usan una base H2 en
memoria (perfil `test`) y cada prueba revierte su transacción.

| Prueba exigida | Dónde |
|---|---|
| 1. Creación exitosa de una orden válida | `SupportOrderApiIntegrationTest.createsValidOrder`, `SupportOrderServiceTest.createsOpenOrderWithServerTimestampAndDefaultPriority` |
| 2. Rechazo de una solicitud inválida | `SupportOrderApiIntegrationTest.rejectsRequestWithMissingFieldsAndShortDescription`, `rejectsUnknownPriority` |
| 3. Rechazo de una transición no permitida | `SupportOrderApiIntegrationTest.rejectsTransitionNotAllowed`, `SupportOrderServiceTest.rejectsTransitionNotAllowed`, `DefaultStatusTransitionPolicyTest` |
| 4. Consulta de una orden existente o inexistente | `SupportOrderApiIntegrationTest.returnsExistingOrderDetail`, `returnsNotFoundForMissingOrder` (y sus equivalentes en `SupportOrderServiceTest`) |
| 5. Prueba de controller/integración con respuesta HTTP | Toda la clase `SupportOrderApiIntegrationTest` |

Además: duplicados (`rejectsSecondActiveOrderForSameExternalReference`), reutilización de la referencia
tras una orden final (`SupportOrderServiceIntegrationTest`) y la restricción UNIQUE en la base
(`SupportOrderRepositoryTest`).

## Configuración de base de datos

**Por defecto, H2 en modo archivo** (`backend/data/`, ignorado por Git): no requiere instalar nada y los
datos sobreviven a reinicios. Corre en modo compatibilidad PostgreSQL y el esquema lo crea Flyway
(`backend/src/main/resources/db/migration`); Hibernate solo valida (`ddl-auto: validate`).

Consola H2: `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:file:./data/support-orders`, usuario
`sa`, sin contraseña. Está habilitada solo por comodidad en desarrollo (ver limitaciones).

**PostgreSQL (opcional)**, con el perfil `postgres`:

```bash
docker compose up -d
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Credenciales por defecto `support/support`, base `support_orders` en `localhost:5432`; se pueden cambiar
con las variables `DB_URL`, `DB_USER` y `DB_PASSWORD`. La misma migración funciona en ambas bases;
se verificó contra PostgreSQL 16.4, incluida la creación concurrente de órdenes duplicadas.

## API

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `POST` | `/api/support-orders` | Crea una orden en estado `OPEN` | 201 + `Location`, 400, 409 |
| `GET` | `/api/support-orders?status=OPEN` | Lista (más recientes primero); `status` es opcional | 200, 400 |
| `GET` | `/api/support-orders/{id}` | Detalle completo, con `allowedTransitions` | 200, 404 |
| `PATCH` | `/api/support-orders/{id}/status` | Cambia el estado: `{"status": "IN_PROGRESS"}` | 200, 400, 404, 409 |

Valores: `category` ∈ PAYMENT, ACCOUNT, TECHNICAL, SERVICE, OTHER · `channel` ∈ MOBILE_APP, WEB,
CALL_CENTER, EMAIL · `priority` ∈ LOW, MEDIUM, HIGH (opcional, MEDIUM por defecto) · `status` ∈ OPEN,
IN_PROGRESS, RESOLVED, CANCELLED.

Los errores siguen RFC 7807 (`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Solicitud inválida",
  "status": 400,
  "detail": "Uno o más campos no son válidos.",
  "instance": "/api/support-orders",
  "errors": { "customerId": "es obligatorio", "description": "debe tener entre 10 y 500 caracteres" }
}
```

| Situación | HTTP |
|---|---|
| Campos faltantes o inválidos, valor de enum desconocido, JSON mal formado, `?status=` inválido | 400 (`errors` por campo cuando aplica) |
| Orden inexistente | 404 |
| Ya existe una orden activa con la misma `externalReference` | 409 |
| Transición no permitida (incluye `currentStatus` y `requestedStatus`) | 409 |
| Dos operadores modifican la misma orden a la vez (bloqueo optimista) | 409 |
| Error inesperado (el detalle solo va al log) | 500 |

### Ejemplos

[`api.http`](api.http) contiene el recorrido completo (crear, duplicado, errores de validación,
listado, detalle, transiciones válidas e inválidas) para el cliente HTTP de IntelliJ o la extensión
REST Client de VS Code. Con curl:

```bash
curl -i -X POST http://localhost:8080/api/support-orders \
  -H "Content-Type: application/json" \
  -d '{"externalReference":"MOB-1001","customerId":"CUST-001","category":"PAYMENT","description":"El pago aparece como pendiente hace varios minutos.","channel":"MOBILE_APP","priority":"MEDIUM"}'

curl -i "http://localhost:8080/api/support-orders?status=OPEN"

curl -i http://localhost:8080/api/support-orders/1

curl -i -X PATCH http://localhost:8080/api/support-orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

> En Windows (Git Bash / PowerShell), un cuerpo con tildes escrito en la línea de comandos puede no
> enviarse en UTF-8 y la API responderá 400. Usar `api.http` o `--data-binary @archivo.json` con el
> archivo guardado en UTF-8.

## Decisiones técnicas

### Arquitectura

Monolito por capas; cada paquete tiene una responsabilidad:

```
com.example.supportorders
├── api            Capa HTTP: SupportOrderController, dto/ (request/response), error/ (GlobalExceptionHandler)
├── application    Casos de uso: SupportOrderService, NewSupportOrder (comando), excepciones de negocio
├── domain         SupportOrder (entidad), enums, StatusTransitionPolicy y su implementación
├── persistence    SupportOrderRepository (Spring Data JPA)
└── config         Beans de infraestructura (Clock)
```

- El controlador solo valida, delega y convierte a DTO; no tiene reglas de negocio.
- Los DTOs de la API están separados de la entidad. El servicio recibe un comando propio
  (`NewSupportOrder`), no el DTO HTTP, así que la capa de aplicación no depende de la web.
- La entidad protege sus invariantes: solo se crea con `SupportOrder.open(...)` (siempre `OPEN` y con la
  fecha del servidor), no tiene setters y el estado solo cambia con `transitionTo(...)`.
- Las fechas vienen de un `Clock` inyectado, así que las pruebas las pueden fijar.
- No hay integraciones externas; si las hubiera (notificar al cliente, por ejemplo) irían en un paquete
  `integration` detrás de una interfaz definida en `application`.
- Por pragmatismo la entidad JPA vive en `domain`. En un dominio más grande separaría el modelo de
  dominio de la entidad de persistencia.

Frontend: `api/` (tipos del contrato y cliente `fetch` que interpreta los errores RFC 7807),
`hooks/useOrders` (carga del listado; descarta respuestas de filtros anteriores), `components/`
(listado, formulario, detalle) y `App` (layout y panel abierto). Los botones de cambio de estado salen
de `allowedTransitions` de la API, así que las reglas de transición no se duplican en el cliente.

### Orden activa y duplicados de `externalReference`

**Una orden es activa mientras está en `OPEN` o `IN_PROGRESS`**, es decir, mientras todavía requiere
trabajo. Cuando llega a `RESOLVED` o `CANCELLED`, la misma referencia externa puede generar una orden
nueva (por ejemplo, si el problema reaparece).

La regla se aplica en dos niveles:

1. **Servicio:** antes de insertar se consulta si ya hay una orden activa con esa referencia y se
   responde 409 con un mensaje claro.
2. **Base de datos:** la columna `active_reference` vale `external_reference` mientras la orden está
   activa y `NULL` en estados finales, y tiene una restricción `UNIQUE` (que admite múltiples `NULL`).
   Si dos peticiones simultáneas pasan la comprobación del servicio, la base rechaza la segunda y el
   servicio la traduce al mismo 409. Un `CHECK` garantiza que `active_reference` y `status` no se
   desincronicen. Se probó con 8 peticiones concurrentes en PostgreSQL: una 201 y siete 409, ninguna 500.

En PostgreSQL bastaría un índice único parcial (`UNIQUE (external_reference) WHERE status IN (...)`); se
eligió la columna auxiliar porque funciona igual en H2 y PostgreSQL.

### Patrón de diseño: Strategy para las transiciones de estado

`StatusTransitionPolicy` es una estrategia que responde qué estados son alcanzables desde uno dado.
`DefaultStatusTransitionPolicy` implementa las reglas del enunciado en una tabla (`EnumMap`):

```
OPEN        → IN_PROGRESS | CANCELLED
IN_PROGRESS → RESOLVED    | CANCELLED
RESOLVED, CANCELLED: finales
```

**Problema que resuelve:** sin el patrón, las reglas acabarían como `if/switch` repartidos entre el
servicio, la entidad y el frontend. Con la estrategia están en un único lugar, declarativo y probado de
forma aislada (`DefaultStatusTransitionPolicyTest`). La misma política alimenta `allowedTransitions` en
la respuesta, así que el frontend nunca ofrece una acción que el backend rechazaría. Hoy hay una sola
implementación; el valor está en poder cambiar las reglas sin tocar el caso de uso.

### Principio SOLID aplicado

- **Inversión de dependencias (D) y abierto/cerrado (O):** `SupportOrderService` y
  `SupportOrder.transitionTo` dependen de la abstracción `StatusTransitionPolicy`, no de la tabla
  concreta. Añadir reglas nuevas (reabrir órdenes, reglas distintas por rol o por categoría) es crear
  otra implementación y registrarla como bean, sin modificar el servicio ni la entidad.
- **Responsabilidad única (S):** controlador (HTTP), servicio (orquestación del caso de uso), política
  (reglas de transición), repositorio (acceso a datos) y `GlobalExceptionHandler` (traducción de
  errores) tienen cada uno un único motivo de cambio.

### Modelo de datos

Tabla `support_orders` (migración `V1__create_support_orders.sql`):

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `bigint` identity | PK |
| `external_reference` | `varchar(50)` not null | Referencia de la app móvil; indexada para consultar el historial |
| `customer_id` | `varchar(50)` not null | |
| `category`, `channel`, `priority`, `status` | `varchar` not null | Enums guardados como texto, con `CHECK` de valores |
| `description` | `varchar(500)` not null | `CHECK` de 10–500 caracteres |
| `active_reference` | `varchar(50)` null, `UNIQUE` | Ver "Orden activa y duplicados" |
| `created_at`, `updated_at` | `timestamp with time zone` | Generados por el servidor (UTC) |
| `version` | `bigint` | Bloqueo optimista (`@Version`): evita que dos operadores se pisen |

Índice `(status, created_at desc)` para el listado filtrado. El historial mínimo es la propia fila
(creación y última actualización). Para auditar cada cambio de estado añadiría una tabla
`support_order_status_history (order_id, from_status, to_status, changed_at, changed_by)`.

### Paginación en producción

No está implementada; el listado devuelve todas las órdenes que cumplen el filtro. En producción:

- Recibir `page`/`size` (Spring Data `Pageable`, con un `size` máximo, por ejemplo 100) y devolver
  `{ content, page, size, totalElements }`, o mejor **paginación por cursor (keyset)** sobre
  `(created_at, id)`: `?after=<cursor>&limit=50`. Es estable aunque entren órdenes nuevas y no degrada
  con offsets grandes; el índice `(status, created_at desc)` ya lo soporta.
- En el frontend, botón "Cargar más" o paginador con los mismos parámetros.

### ¿Cuándo usar NoSQL?

SQL encaja aquí: pocas entidades, reglas de integridad (unicidad de orden activa, valores válidos) y
cambios de estado que deben ser transaccionales.

Una base NoSQL tendría sentido si:

- **se guardan grandes volúmenes de eventos** (cada cambio de estado, telemetría de la app, mensajes del
  chat de soporte): un almacén de documentos o de columnas anchas (MongoDB, Cassandra, DynamoDB) escala
  mejor en escritura y se particiona por `orderId` o por fecha;
- **los payloads son variables** (metadatos de la incidencia distintos por categoría o versión de la
  app): un documento JSON evita migraciones frecuentes;
- **predomina la lectura del historial completo de una orden**: un documento por orden con su historial
  embebido se lee de una sola vez.

**Se ganaría** escalabilidad horizontal y flexibilidad de esquema. **Se perdería** lo que hoy protege
la base: restricciones `UNIQUE`/`CHECK`, transacciones entre varias filas y consultas ad hoc con joins
para reportes. La regla de "una orden activa por referencia" tendría que garantizarse con escrituras
condicionales o en la aplicación. Un punto intermedio razonable: mantener las órdenes en SQL (o columnas
`jsonb` en PostgreSQL para los datos variables) y mandar los eventos a un almacén NoSQL o a un log de
eventos.

### Logs

- `INFO` al crear una orden (id, referencia, categoría, prioridad, canal) y al cambiar de estado (id,
  estado anterior y nuevo).
- `WARN` para errores controlados (validación, duplicado, transición no permitida, no encontrada,
  conflicto concurrente), sin el cuerpo de la petición.
- `ERROR` con stack trace solo para errores inesperados; el cliente recibe un mensaje genérico.
- Nunca se registran `customerId` ni la descripción, que pueden contener datos personales.

## Supuestos y limitaciones

**Supuestos**

- `category` y `channel` son catálogos cerrados (enums); `OTHER` cubre lo no previsto. Si el catálogo
  cambiara a menudo, lo movería a una tabla de referencia.
- `priority` es opcional y por defecto `MEDIUM` (el enunciado no la lista entre los obligatorios).
- `externalReference` y `customerId` admiten hasta 50 caracteres; los textos se recortan (`trim`) antes
  de validarse.
- Cambiar a un estado igual al actual se trata como transición no permitida (409).

**Limitaciones**

- Sin autenticación ni autorización: cualquiera puede crear y cambiar órdenes. En producción, OAuth2/JWT
  con roles distintos para la app móvil (crear/consultar las propias) y para operadores.
- La consola H2 está habilitada en el perfil por defecto (solo para desarrollo; desactivada en
  `postgres`).
- Sin paginación (ver arriba) ni historial detallado de cambios de estado.
- Sin idempotencia por cabecera (`Idempotency-Key`): un reintento de la app con una orden activa recibe
  409, lo que ya evita duplicados pero no devuelve la orden original.
- No hay pruebas automáticas del frontend; se verificó manualmente en el navegador (escritorio y 375 px).
- Cuando la base rechaza un duplicado concurrente, Hibernate registra la violación de la restricción a
  nivel ERROR antes de que el servicio la traduzca a 409 (ruido esperado en el log).

## Trabajo colaborativo y próximos pasos

Puntos que validaría con otros equipos antes de ir a producción:

- **Producto / operaciones:** catálogo definitivo de categorías y canales, si se debe poder reabrir una
  orden resuelta y si `CANCELLED` requiere un motivo.
- **Equipo móvil:** el contrato de la API (formato de errores RFC 7807, 409 ante duplicados) y si
  necesitan idempotencia en los reintentos.
- **Seguridad / datos:** qué datos de la descripción son sensibles, retención y enmascaramiento, y el
  modelo de autenticación.
- **Plataforma:** base de datos gestionada, migraciones en el pipeline y observabilidad (métricas y
  trazas además de los logs).
