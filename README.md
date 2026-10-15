# TrackFlow Logistics API

API 3 de TrackFlow (Fábrica Escuela UdeA 2026-2). Separada del monolito modular
[FabricaEscuela-Proyecto-TrackFlow](https://github.com/TrackflowFE/FabricaEscuela-Proyecto-TrackFlow),
que se conserva como respaldo. Responsables: Eyner Gómez y Julián Monsalve.

| Historia | Qué cubre |
|---|---|
| HU-02 | Registrar un evento logístico (`POST /api/shipments/{tn}/events`) |
| HU-04 | Consultar el historial de movimientos (`GET /api/shipments/{tn}/events`) |

Además lleva la red logística: centros, rutas entre ciudades, validación del recorrido
(cada movimiento tiene que encajar en la ruta planeada) e itinerario del envío.

## Estado del despliegue

URL pública: pendiente de crear el servicio en Render. `render.yaml` define el servicio
`trackflow-logistics`. La API de Envíos y acceso debe desplegarse primero, porque es la
que crea el esquema de la base (Flyway V1 a V22).

## Endpoints

| Método y ruta | Acceso |
|---|---|
| `POST /api/shipments/{tn}/events` | Rol operador. Responde 202: el movimiento se guarda al consumir la cola. Admite `Idempotency-Key` |
| `GET /api/shipments/{tn}/events` | Público (HU-04) |
| `GET /api/shipments/{tn}/route` | Público: ciudades recorridas, escalas pendientes y destino |
| `GET /api/shipments/{tn}/actions` | Rol operador: qué movimientos admite el envío ahora y en qué centros |
| `GET /api/centers`, `GET /api/centers/{id}` | Rol operador |
| `POST /api/admin/rebuild-projections` | Rol administrador: republica los eventos logísticos |

Swagger: `/swagger-ui.html`. Health: `/actuator/health` (incluye base de datos y RabbitMQ).

El token se obtiene en `POST /api/auth/login` de la **API de Envíos y acceso**; esta API
no emite tokens, solo los verifica con el mismo secreto y comprueba que la sesión siga
abierta (y renueva su actividad, para que la inactividad de 30 minutos cuente también
el trabajo hecho aquí).

## Comunicación con las otras APIs

Ninguna API llama a otra por HTTP: los eventos viajan por RabbitMQ, en el exchange
compartido `trackflow.events` (topic), en JSON.

| Evento | Clave | Esta API |
|---|---|---|
| Envío creado (`EnvioCreadoEvent`) | `shipment.created` | Lo **consume** en `trackflow.logistics.shipment-created` para conocer origen, destino y fecha de registro del envío |
| Evento logístico registrado (`EventoLogisticoRegistradoEvent`) | `logistics.event.registered` | Lo **publica** después de guardar el movimiento; lo consumen Envíos y Reportes |

Los dos contratos (`shared/events`) son idénticos en las tres APIs y no cambian sin avisar
a los otros responsables. Lo que falla tras 4 intentos va a `trackflow.logistics.events.dlq`.

La cola interna de solicitudes de registro (`logistics.api.requests.queue`, exchange
`logistics.api.topic`) es solo de esta API y tiene un nombre distinto al del monolito.

## Base de datos

Base única de Supabase. Esta API no ejecuta migraciones (sin Flyway, Hibernate en
`validate`) y usa un pool de 3 conexiones.

- Escribe: `logistics_events`, `logistics_tracked_shipments`, y en `auth_sessions` solo la última actividad.
- Lee: `logistics_centers`, `logistics_route_legs`, `cities`, `auth_users`.

## Variables de entorno

| Variable | Valor |
|---|---|
| `TRACKFLOW_DB_URL`, `TRACKFLOW_DB_USER`, `TRACKFLOW_DB_PASSWORD` | La base de Supabase del equipo |
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` | El broker compartido por las tres APIs |
| `RABBITMQ_PORT`, `RABBITMQ_SSL` | `5671` y `true` en CloudAMQP |
| `TRACKFLOW_JWT_SECRET` | El mismo de las otras dos APIs (mínimo 32 caracteres) |
| `TRACKFLOW_CORS_ORIGINS` | Dominio del frontend; por defecto `http://localhost:5173` |
| `TRACKFLOW_SECURITY_ENABLED` | `true` (solo `false` en local, sin token) |

Las credenciales van en las variables del servicio de Render, nunca en el repositorio.

## Ejecución local

Requisitos: JDK 21, PostgreSQL con el esquema creado por la API de Envíos y RabbitMQ.
No hace falta Maven: el wrapper está incluido.

```bash
./mvnw spring-boot:run
```

En Windows: `.\mvnw.cmd spring-boot:run`. Por defecto escucha en el puerto 8083.

```bash
docker build -t trackflow-logistics .
```
