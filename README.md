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
├── backend/     API Spring Boot
├── frontend/    Interfaz web del operador
├── api.http     Ejemplos de consumo de la API
└── README.md
```

## Requisitos

- JDK 17 o superior (no hace falta instalar Maven: se incluye `mvnw`)
- Node.js 20.19+ / 22.12+ y npm

## Cómo ejecutar

### Backend

```bash
cd backend
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`. Los datos se guardan en `backend/data/` (H2 en modo archivo).
Consola H2: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/support-orders`, usuario `sa`, sin contraseña).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`. Vite redirige `/api` al backend en el puerto 8080, así que no hace falta configurar CORS.

## Cómo ejecutar las pruebas

```bash
cd backend
./mvnw test
```

## Configuración de base de datos

<!-- TODO paso 6: perfil postgres + docker-compose -->

## API

<!-- TODO paso 6: tabla de endpoints, formato de errores, enlace a api.http y ejemplos curl -->

## Decisiones técnicas

### Arquitectura

<!-- TODO: capas api / application / domain / persistence -->

### Orden activa y duplicados de `externalReference`

<!-- TODO: definición de orden activa (OPEN o IN_PROGRESS) y restricción única en active_reference -->

### Patrón de diseño: reglas de transición de estado

<!-- TODO -->

### Principio SOLID aplicado

<!-- TODO -->

### Modelo de datos

<!-- TODO -->

### Paginación en producción

<!-- TODO -->

### ¿Cuándo usar NoSQL?

<!-- TODO -->

## Supuestos y limitaciones

<!-- TODO -->
