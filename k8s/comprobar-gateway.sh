#!/usr/bin/env bash
# Comprueba lo que no se puede dar por bueno leyendo los manifiestos: que el gateway
# reparte bien las rutas y que corta de verdad cuando se pasa del límite.
#
# Uso:  ./comprobar-gateway.sh
set -uo pipefail

GW="${GW:-http://localhost}"
fallos=0

titulo() { printf '\n\033[1m%s\033[0m\n' "$1"; }
bien()   { printf '  \033[32mOK\033[0m   %s\n' "$1"; }
mal()    { printf '  \033[31mFALLA\033[0m %s\n' "$1"; fallos=$((fallos + 1)); }

codigo() { curl -s -o /dev/null -m 15 -w '%{http_code}' "$@"; }

# ── 1. Reparto de rutas ─────────────────────────────────────────────────────
# /api/shipments lo comparten dos APIs: el historial y el itinerario son de logística,
# mientras que crear y listar envíos son de la API de envíos. Con prefijos puros no se
# distinguen, así que este es el reparto que de verdad hay que verificar.
titulo "1. Reparto de rutas"

c=$(codigo "$GW/api/shipments/TF0000000001/events")
case "$c" in
  200|404) bien "historial -> logística (HTTP $c)" ;;
  401|403) mal  "historial -> llegó a la API de envíos, que pide token (HTTP $c).
         El Ingress general no está aplicando la regla con expresión regular." ;;
  *)       mal  "historial -> respuesta inesperada (HTTP $c)" ;;
esac

c=$(codigo "$GW/api/centers")
case "$c" in
  401|403) bien "centros -> logística, y exige token (HTTP $c)" ;;
  200)     bien "centros -> logística (HTTP $c)" ;;
  *)       mal  "centros -> respuesta inesperada (HTTP $c)" ;;
esac

# En el clúster mínimo la API de reportes no está desplegada: un 503 aquí es correcto.
c=$(codigo "$GW/api/tracking/TF0000000001")
case "$c" in
  200|404|429) bien "rastreo -> reportes (HTTP $c)" ;;
  503)         bien "rastreo -> reportes, que no está desplegado en el modo mínimo (HTTP 503)" ;;
  *)           mal  "rastreo -> respuesta inesperada (HTTP $c)" ;;
esac

# ── 2. El límite corta de verdad ────────────────────────────────────────────
titulo "2. Límite del gateway en el inicio de sesión"
echo "  enviando 15 intentos seguidos..."

respuestas=""
for _ in $(seq 1 15); do
  respuestas="$respuestas $(codigo -X POST "$GW/api/auth/login" \
    -H 'Content-Type: application/json' \
    --data-binary '{"username":"no-existe","password":"no-importa"}')"
done
echo "  respuestas:$respuestas"

if echo "$respuestas" | grep -q 429; then
  bien "el gateway devolvió 429 al pasarse del límite"
else
  mal "ninguna respuesta fue 429: el límite no se está aplicando.
         Revisa que el Ingress trackflow-gateway-auth exista y que el controlador
         tenga use-forwarded-headers activado."
fi

# ── 3. La sesión no comparte el límite estricto ─────────────────────────────
# /api/auth/sesion se consulta en cada carga de pantalla: si cayera bajo el techo de 10
# por minuto, el uso normal de una oficina dejaría a todos fuera.
titulo "3. La consulta de sesión no cae en el límite estricto"
c=$(codigo "$GW/api/auth/sesion")
if [ "$c" = "429" ]; then
  mal "sesión devolvió 429: está bajo el límite de autenticación y no debería"
else
  bien "sesión responde $c, fuera del límite estricto"
fi

# ── Resultado ───────────────────────────────────────────────────────────────
echo
if [ "$fallos" -eq 0 ]; then
  printf '\033[32mTodo correcto.\033[0m El gateway reparte y limita como se esperaba.\n'
else
  printf '\033[31m%s comprobación(es) fallaron.\033[0m\n' "$fallos"
  echo "Para ver qué está recibiendo el gateway:"
  echo "  kubectl -n ingress-nginx logs deploy/ingress-nginx-controller --tail=40"
fi
exit "$fallos"
