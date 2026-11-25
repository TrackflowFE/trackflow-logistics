#!/usr/bin/env bash
# Levanta el conjunto mínimo para demostrar la API de logística y el API Gateway en un
# equipo con poca memoria: Postgres, RabbitMQ, la API de envíos (que es quien ejecuta las
# migraciones) y la de logística, más el controlador de Ingress.
#
# Queda fuera lo que no hace falta para esa demostración: Prometheus, Grafana y la API de
# reportes. Para el clúster completo se aplican los manifiestos uno a uno, como describe
# docs/observabilidad-y-kubernetes.md.
#
# Presupuesto de memoria, con los topes recortados más abajo:
#   nodo de kind ~500 MB · Postgres 256 · RabbitMQ 320 · envíos 512 · logística 512 ·
#   Ingress 150  ≈ 2,3 GB
#
# Uso:  ./cluster-minimo.sh
set -euo pipefail

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$AQUI"

paso() { printf '\n\033[1m== %s\033[0m\n' "$1"; }

# ── 0. Comprobaciones previas ───────────────────────────────────────────────
paso "Comprobando herramientas"
for cmd in docker kubectl kind; do
  command -v "$cmd" >/dev/null || { echo "Falta $cmd en el PATH"; exit 1; }
done
docker info >/dev/null 2>&1 || { echo "El motor de Docker no responde: abre Docker Desktop"; exit 1; }
echo "docker, kubectl y kind listos"

# En un equipo justo de memoria, saberlo antes evita un diagnóstico confuso después:
# un pod que no arranca por falta de RAM no dice "falta RAM", dice OOMKilled o Pending.
libre_kb=$(awk '/MemAvailable/ {print $2}' /proc/meminfo 2>/dev/null || echo 0)
if [ "$libre_kb" -gt 0 ] && [ "$libre_kb" -lt 2500000 ]; then
  echo "AVISO: quedan $((libre_kb / 1024)) MB libres y hacen falta unos 2500."
  echo "Cierra el navegador y lo que no estés usando antes de seguir."
  read -r -p "¿Continuar de todos modos? [s/N] " r
  [ "$r" = "s" ] || exit 1
fi

# ── 1. Clúster ──────────────────────────────────────────────────────────────
if kind get clusters 2>/dev/null | grep -qx trackflow; then
  paso "El clúster trackflow ya existe, se reutiliza"
else
  paso "Creando el clúster"
  kind create cluster --config kind-config.yaml
fi

kubectl apply -f 00-namespace.yaml
if ! kubectl -n trackflow get secret trackflow-secretos >/dev/null 2>&1; then
  paso "Creando los secretos (se muestran UNA sola vez: anótalos)"
  ./crear-secretos.sh
fi

# ── 2. Base de datos y broker ───────────────────────────────────────────────
paso "Postgres y RabbitMQ"
kubectl apply -f 10-postgres.yaml -f 11-rabbitmq.yaml
kubectl -n trackflow rollout status deploy/postgres --timeout=300s
kubectl -n trackflow rollout status deploy/rabbitmq --timeout=300s

# ── 3. Las dos APIs, con el tope de memoria recortado ───────────────────────
# La API de envíos va primero porque es la única que ejecuta las migraciones de Flyway.
# Logística valida contra ese esquema al arrancar, así que sin él no levanta.
paso "API de envíos (crea el esquema)"
kubectl apply -f 30-api-envios.yaml
kubectl -n trackflow set resources deploy/api-envios --limits=memory=512Mi --requests=memory=320Mi
kubectl -n trackflow rollout status deploy/api-envios --timeout=420s

paso "API de logística"
kubectl apply -f 32-api-logistica.yaml
kubectl -n trackflow set resources deploy/api-logistica --limits=memory=512Mi --requests=memory=320Mi
kubectl -n trackflow rollout status deploy/api-logistica --timeout=420s

# ── 4. API Gateway ──────────────────────────────────────────────────────────
paso "Controlador de Ingress (el gateway)"
if ! kubectl get ns ingress-nginx >/dev/null 2>&1; then
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/kind/deploy.yaml
fi
kubectl -n ingress-nginx wait --for=condition=ready pod \
  -l app.kubernetes.io/component=controller --timeout=300s

# Las reglas nombran api-reportes, que en el modo mínimo no existe. El Ingress se aplica
# igual: nginx deja esas rutas en 503 y las demás funcionan.
kubectl apply -f 40-gateway.yaml || echo "AVISO: alguna regla del gateway no se aplicó; revisa la salida"

paso "Estado final"
kubectl -n trackflow get pods -o wide
echo
echo "Gateway:   http://localhost"
echo "Envíos:    http://localhost:8080   Logística: http://localhost:8083"
echo
echo "Comprobaciones:"
echo "  ./comprobar-gateway.sh"
