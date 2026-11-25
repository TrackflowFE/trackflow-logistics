# Guía de observabilidad y despliegue local: API 3, Logística

Repositorio: **trackflow-logistics** (el nombre de la imagen Docker sí es
`trackflow-logistics-api:local`, porque es el que usan los manifiestos compartidos del equipo).

Esta guía cubre los puntos 2 y 3 del entregable:

- Punto 2: contenedores Docker desplegados en Kubernetes de forma local.
- Punto 3: integración con Prometheus y Grafana.

El punto 1 (refinar la lógica de las APIs) se resuelve con las historias de usuario que cada quien ya tiene; al final hay una lista corta para revisar.

Estado: **el paso 3 (los cambios de código) ya está aplicado en la rama
`feat/observabilidad-k8s`**, y la carpeta `k8s/` ya está en este repositorio con el
manifiesto de logística completado. Lo que queda por hacer en tu equipo es del paso 4 en
adelante: construir la imagen, levantar el clúster y tomar las capturas.

Cada paso termina con una comprobación. Si una comprobación falla, copia el mensaje exacto
del error y no sigas hasta resolverlo.

## 1. Cómo encaja todo

Se levanta un solo clúster local (con la herramienta kind) en el computador de quien haga la demostración. Dentro corren, cada una en su propio contenedor:

- Postgres y RabbitMQ, compartidos por las tres APIs (en producción son Supabase y CloudAMQP).
- Las tres APIs.
- Prometheus, que cada 15 segundos le pregunta a cada API cómo va.
- Grafana, que dibuja esos números en un dashboard.

Las métricas de cada API salen por un segundo puerto (8081) que solo existe dentro del clúster. El puerto público (8080) no las publica. Así no quedan abiertas a internet, y en Render todo sigue como hoy.

La carpeta `k8s/` ya trae hechos los archivos del clúster:

| Archivo | Qué es |
|---|---|
| `kind-config.yaml` | Crea el clúster y enlaza puertos a tu equipo |
| `00-namespace.yaml` | Espacio de nombres `trackflow` |
| `crear-secretos.sh` | Crea las claves del clúster al azar |
| `10-postgres.yaml`, `11-rabbitmq.yaml` | Base de datos y broker compartidos |
| `20-prometheus.yaml`, `21-grafana.yaml` | Observabilidad (el dashboard ya viene cargado) |
| `30-api-envios.yaml`, `31-api-reportes.yaml`, `32-api-logistica.yaml` | Las tres APIs |

A ti te toca: métricas en tu API (paso 3), imagen Docker (paso 4) y desplegarla (paso 5). La parte compartida (Postgres, RabbitMQ, Prometheus y Grafana) la levanta quien integra la demostración; para probar tu API sola puedes levantarla tú en tu equipo con los mismos archivos.

## 2. Instalar las herramientas (una sola vez por equipo)

2.1. Docker Desktop debe estar abierto, con la integración de WSL activada para Ubuntu (Settings, Resources, WSL integration). Comprueba en la terminal de Ubuntu:

```bash
cd ~
docker --version
docker ps
```

Los dos comandos deben responder sin error.

2.2. Instala kubectl y kind:

```bash
cd ~
curl -Lo kubectl https://dl.k8s.io/release/v1.31.0/bin/linux/amd64/kubectl
curl -Lo kind https://kind.sigs.k8s.io/dl/v0.24.0/kind-linux-amd64
sudo install -m 0755 kubectl /usr/local/bin/kubectl
sudo install -m 0755 kind /usr/local/bin/kind
rm kubectl kind
kubectl version --client
kind --version
```

Comprobación: los dos últimos comandos muestran una versión.

2.3. Memoria. Un clúster con tres APIs, Postgres, RabbitMQ, Prometheus y Grafana necesita unos 6 GB. Si tu WSL tiene menos, crea en Windows el archivo `C:\Users\TU_USUARIO\.wslconfig` con este contenido y reinicia WSL con `wsl --shutdown`:

```
[wsl2]
memory=6GB
```

## 3. Métricas en tu API (ya aplicado)

Estos cambios ya están en la rama `feat/observabilidad-k8s`; se listan para que sepas qué
se tocó y por qué.

| Archivo | Cambio |
|---|---|
| `pom.xml` | Dependencia `io.micrometer:micrometer-registry-prometheus` |
| `application.properties` | `management.metrics.tags.application=trackflow-logistics-api` y `management.endpoints.web.exposure.include` leída de la variable de entorno |
| `SecurityConfig.java` | Regla que abre `/actuator/**` solo para las peticiones que llegan por el puerto de gestión |
| `RegistrarEventoLogistico.java` | Contador `trackflow_eventos_logisticos_registrados_total` con etiqueta `tipo` |

Sobre la regla de seguridad: cuando `MANAGEMENT_SERVER_PORT` no está definida —que es el
caso de Render— la condición `puertoDeGestion > 0` es falsa, no se añade ninguna regla y
`/actuator/prometheus` sigue cayendo en el `denyAll()` del final. En producción nada cambia.

El contador se incrementa en `publicar(...)`, que es por donde pasan tanto los eventos de
la cola como las recogidas, y solo después de que el movimiento quedó guardado: una
reentrega descartada por idempotencia no lo mueve. En Prometheus aparece como
`trackflow_eventos_logisticos_registrados_total{tipo="DISPATCHED",...}`.

Para seguir trabajando sobre la rama:

```bash
cd ~/trackflow-logistics
git fetch origin
git checkout feat/observabilidad-k8s
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw test 2>&1 | grep -E "Tests run:|BUILD" | tail -3
```

Comprobación: `BUILD SUCCESS`.

## 4. Imagen Docker

Cada API ya tiene un `Dockerfile` que compila y empaqueta. Construye la imagen:

```bash
cd ~/trackflow-logistics
docker build -t trackflow-logistics-api:local .
docker images | grep trackflow-logistics-api
```

Comprobación: la segunda línea muestra la imagen con la etiqueta `local`. La primera vez tarda varios minutos porque descarga Maven y las dependencias.

## 5. Clúster local

Si el clúster ya existe en el equipo de quien integra, usa ese. Para probar tu API sola, levántalo tú:

### 5.1. Levantar la parte compartida (si no la tienes)

La carpeta `k8s/` ya está en este repositorio, así que no hay que descomprimir nada:

```bash
cd ~/trackflow-logistics/k8s
kind create cluster --config kind-config.yaml
kubectl apply -f 00-namespace.yaml
./crear-secretos.sh
kubectl apply -f 10-postgres.yaml -f 11-rabbitmq.yaml
kubectl -n trackflow get pods
```

El script `crear-secretos.sh` muestra las claves UNA sola vez: anótalas. No las subas al repositorio. Comprobación: `postgres` y `rabbitmq` en `1/1 Running`.

La base de datos es una sola para las tres APIs. Si tu API necesita tablas que crea otra API (por ejemplo, las migraciones de la API de envíos), despliega esa primero.

### 5.2. Desplegar tu API

El archivo `32-api-logistica.yaml` ya está cotejado contra el `render.yaml` de este
repositorio: se le agregaron `TRACKFLOW_SECURITY_ENABLED`, `TRACKFLOW_INACTIVIDAD_MINUTOS`,
`TRACKFLOW_EVENTS_EXCHANGE`, `TRACKFLOW_SHIPMENT_CREATED_KEY` y
`TRACKFLOW_LOGISTICS_REGISTERED_KEY`. Los nombres de colas y la DLQ no hacen falta porque
`application.properties` ya trae esos mismos valores por defecto.

```bash
cd ~/trackflow-logistics/k8s
kind load docker-image trackflow-logistics-api:local --name trackflow
kubectl apply -f 32-api-logistica.yaml
kubectl -n trackflow rollout status deployment/api-logistica --timeout=300s
kubectl -n trackflow get pods
```

Comprobación: el pod `api-logistica` llega a `1/1 Running` (1 a 3 minutos). Si no, mira los registros:

```bash
cd ~/trackflow-logistics/k8s
kubectl -n trackflow logs deploy/api-logistica --tail=60
```

Si dice `ImagePullBackOff`, faltó el `kind load` (se repite cada vez que reconstruyas la imagen, y luego `kubectl -n trackflow rollout restart deployment/api-logistica`).

### 5.3. Comprobar la API y que las métricas NO están abiertas afuera

El puerto público (8080 del contenedor) se enlaza a `localhost:8083` de tu equipo:

```bash
cd ~/trackflow-logistics/k8s
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8083/actuator/prometheus
```

Comprobación: NO debe responder 200 (lo esperado es 401, 403 o 404).

Las métricas sí deben salir por el puerto de gestión. En una terminal:

```bash
cd ~/trackflow-logistics/k8s
kubectl -n trackflow port-forward deploy/api-logistica 8081:8081
```

En otra terminal:

```bash
cd ~/trackflow-logistics/k8s
curl -s http://localhost:8081/actuator/prometheus | head -15
```

Comprobación: aparecen líneas como `jvm_memory_used_bytes{application="trackflow-logistics-api",...}`. Cierra el `port-forward` con `Ctrl + C`.

## 6. Prometheus y Grafana

Esta parte la levanta quien integra (Oliver). Si pruebas tu API sola en tu propio clúster, aplica los dos archivos igual:

```bash
cd ~/trackflow-logistics/k8s
kubectl apply -f 20-prometheus.yaml -f 21-grafana.yaml
kubectl -n trackflow get pods
```

Comprobación en el navegador:

1. `http://localhost:9090/targets`: tu API debe aparecer como `UP`. Las otras APIs aparecen en rojo hasta que se desplieguen, y eso es normal.
2. `http://localhost:3000`: usuario `admin` y la clave de Grafana que mostró `crear-secretos.sh`. Ve a Dashboards, carpeta TrackFlow, dashboard "TrackFlow - APIs".
3. Genera tráfico hacia tu API (cambia la ruta por una pública de tu API) y comprueba que "Peticiones por segundo" se mueve para `trackflow-logistics-api`.

Si el dashboard no aparece, el plan B es importarlo a mano: Dashboards, New, Import, y escribe el número 4701 (JVM Micrometer).

## 6 bis. API Gateway

El límite de peticiones estaba escrito tres veces —una por servicio, con copias de las
mismas clases Java— y eso es exactamente lo que un API Gateway evita. En el clúster el
papel lo hace el controlador de Ingress: ve todo el tráfico, así que el límite se declara
una vez y fuera del código.

Instálalo una sola vez por clúster:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/kind/deploy.yaml
```

Espera a que esté listo y aplica las reglas:

```bash
kubectl -n ingress-nginx wait --for=condition=ready pod -l app.kubernetes.io/component=controller --timeout=180s
```

```bash
kubectl apply -f ~/trackflow-logistics/k8s/40-gateway.yaml
```

A partir de ahí las tres APIs se alcanzan por **http://localhost**, sin puerto:

| Ruta | A dónde va | Límite |
|---|---|---|
| `/api/auth/login`, `/api/auth/clave/**` | Envíos | 10 por minuto y por IP |
| `/api/reports/**`, `/api/tracking/**` | Reportes | 100 por minuto y por IP |
| `/api/centers/**` | Logística | 100 por minuto y por IP |
| todo lo demás | Envíos | 100 por minuto y por IP |

El límite estricto cubre el inicio de sesión y las rutas de contraseña, no todo
`/api/auth`: la pantalla consulta `/api/auth/sesion` en cada carga, y varias personas
de una oficina salen por una sola IP. Con el prefijo completo, el uso normal agotaría
la cuota y nadie podría entrar.

Comprobación: 15 peticiones seguidas al login deben terminar en 429.

```bash
for i in $(seq 1 15); do curl -s -o /dev/null -w "%{http_code} " -X POST http://localhost/api/auth/login -H 'Content-Type: application/json' -d '{"username":"x","password":"y"}'; done; echo
```

Los filtros que quedan dentro de cada API no sobran. Si alguien alcanza un servicio sin
pasar por el gateway —otro pod del clúster, o una prueba contra el puerto 8083— el
límite propio del servicio sigue aplicándose. El gateway es la primera barrera, no la
única, que es lo que pide Zero Trust.

**Esto vale para el clúster, no para Render.** Allí los tres servicios son despliegues
independientes con su propio dominio y no hay un punto por donde pase todo; por eso el
límite por IP dentro de cada API sigue siendo necesario en producción.

## 7. Qué hay que entregar (evidencia)

1. El pull request de la rama `feat/observabilidad-k8s` en **trackflow-logistics**, con la dependencia, la regla de seguridad, el contador de negocio y los archivos de `k8s/`.
2. Captura de `kubectl -n trackflow get pods,svc` con todos los pods en `Running`.
3. Captura de `http://localhost:9090/targets` con las APIs en `UP`.
4. Captura del dashboard de Grafana con tráfico en las gráficas.
5. Captura de `docker images` donde se vea la imagen `trackflow-logistics-api:local`.
6. Captura del `for` de arriba, donde se vean los 200 pasando a 429 al cruzar el límite
   del gateway.

Todas las capturas deben mostrar la fecha y hora del equipo o un fondo reconocible, para que se vea que son propias.

## 8. Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `ImagePullBackOff` | Falta cargar la imagen en kind | `kind load docker-image trackflow-logistics-api:local --name trackflow` y reiniciar el despliegue |
| `CrashLoopBackOff` | La API no arranca | `kubectl -n trackflow logs deploy/api-logistica --tail=80`: busca el primer `ERROR` o `Caused by` |
| Pod en `0/1` mucho tiempo | La JVM tarda en arrancar | Espera hasta 3 minutos; si pasa de eso, mira los registros |
| `Address already in use` o la API local choca | Otro programa usa el puerto 8080, 8082 u 8083 | Cierra la API que corras a mano antes de usar el clúster |
| En Prometheus el objetivo sale `DOWN` | Falta `MANAGEMENT_SERVER_PORT` o el nombre del Service no coincide | Revisa el manifiesto y que el Service se llame `api-logistica` |
| `docker: command not found` en Ubuntu | Docker Desktop cerrado o sin integración WSL | Abre Docker Desktop y activa la integración con Ubuntu |
| El clúster se queda sin memoria | Poca RAM para WSL | Configura `.wslconfig` (paso 2.3) y cierra otras aplicaciones |

Para borrar el clúster y empezar de cero: `kind delete cluster --name trackflow`.

## 9. Punto 1: refinar la lógica de la API

Revisa estos puntos en tus historias de usuario antes de entregar:

- Cada historia cumple sus criterios de aceptación de principio a fin (no solo el caso feliz).
- Los errores se responden con un código HTTP y un mensaje claros, sin mostrar detalles internos.
- Los datos que llegan del cliente se validan en el servidor.
- Las rutas que modifican datos están protegidas por rol; solo es público lo que debe serlo.
- Las pruebas automáticas de tus historias pasan en limpio.
- Lo que cambia entre API (contratos de eventos, nombres de campos) está acordado con quien consume tu API.
