#!/usr/bin/env sh
set -eu
umask 077

case "$0" in
  */*) script_path=$0 ;;
  *) script_path=$(command -v "$0") || exit 1 ;;
esac
SCRIPT_DIR=$(CDPATH= cd "${script_path%/*}" && pwd)
PROJECT_DIR=$(CDPATH= cd "$SCRIPT_DIR/.." && pwd)
ENV_FILE="$PROJECT_DIR/.env"
ENV_EXAMPLE="$PROJECT_DIR/.env.example"
web_port_override=${WEB_PORT:-}
temp_file=

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

cleanup() {
  if [ -n "$temp_file" ] && [ -f "$temp_file" ]; then
    rm -f "$temp_file"
  fi
}

trap cleanup EXIT
trap 'exit 1' HUP INT TERM

random_hex() {
  od -An -N"${1:-32}" -tx1 /dev/urandom | tr -d ' \n'
}

set_env_value() {
  key=$1
  value=$2
  temp_file="${ENV_FILE}.tmp.$$"
  awk -v key="$key" -v value="$value" '
    BEGIN { found = 0 }
    index($0, key "=") == 1 { print key "=" value; found = 1; next }
    { print }
    END { if (!found) print key "=" value }
  ' "$ENV_FILE" > "$temp_file"
  mv "$temp_file" "$ENV_FILE"
  temp_file=
}

get_env_value() {
  key=$1
  awk -F= -v key="$key" '
    $1 == key {
      sub(/^[^=]*=/, "")
      sub(/\r$/, "")
      print
      exit
    }
  ' "$ENV_FILE"
}

require_env_value() {
  key=$1
  value=$(get_env_value "$key")
  [ -n "$value" ] || fail "Required value is missing from .env: $key"
}

wait_for_health() {
  service=$1
  attempts=90

  while [ "$attempts" -gt 0 ]; do
    container_id=$(docker compose ps -q "$service")
    if [ -n "$container_id" ]; then
      status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id" 2>/dev/null || true)
      case "$status" in
        healthy|running)
          echo "Service $service is $status"
          return 0
          ;;
        exited|dead)
          docker compose logs --tail=100 "$service" || true
          fail "Service $service stopped before becoming healthy"
          ;;
      esac
    fi
    attempts=$((attempts - 1))
    sleep 5
  done

  docker compose logs --tail=100 "$service" || true
  fail "Timed out waiting for service $service"
}

if [ -n "$web_port_override" ]; then
  case "$web_port_override" in
    *[!0-9]*) fail "WEB_PORT must be an integer between 1 and 65535" ;;
  esac
  if [ "$web_port_override" -lt 1 ] || [ "$web_port_override" -gt 65535 ]; then
    fail "WEB_PORT must be an integer between 1 and 65535"
  fi
fi

require_command docker
require_command awk
require_command chmod
require_command cp
require_command grep
require_command mv
require_command od
require_command rm
require_command tr

[ -f "$ENV_EXAMPLE" ] || fail "Environment template not found: $ENV_EXAMPLE"

docker_os=$(docker info --format '{{.OSType}}' 2>/dev/null) || fail "Docker daemon is not available"
[ "$docker_os" = "linux" ] || fail "Docker must be configured to run Linux containers"
docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 is required"

cd "$PROJECT_DIR"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  database_password=$(random_hex 24)
  set_env_value POSTGRES_PASSWORD "$database_password"
  set_env_value DB_PASSWORD "$database_password"
  set_env_value REDIS_PASSWORD "$(random_hex 24)"
  set_env_value JWT_SECRET "$(random_hex 48)"
  echo "Created $ENV_FILE with random infrastructure secrets"
fi

if [ -n "$web_port_override" ]; then
  set_env_value WEB_PORT "$web_port_override"
fi

chmod 600 "$ENV_FILE"

for key in POSTGRES_PASSWORD DB_PASSWORD REDIS_PASSWORD JWT_SECRET DEMO_ADMIN_PASSWORD; do
  require_env_value "$key"
done

jwt_secret=$(get_env_value JWT_SECRET)
[ "${#jwt_secret}" -ge 32 ] || fail "JWT_SECRET must contain at least 32 characters"

if grep -Eq '=(change-me-|replace-this-)' "$ENV_FILE"; then
  fail "Unsafe default infrastructure credentials remain in .env"
fi

docker compose config --quiet
docker compose up -d --build --remove-orphans

wait_for_health postgres
wait_for_health redis
wait_for_health rocketmq-namesrv
wait_for_health rocketmq-broker
wait_for_health backend
wait_for_health frontend

web_port=$(awk -F= '$1 == "WEB_PORT" { print $2 }' "$ENV_FILE")
admin_user=$(awk -F= '$1 == "DEMO_ADMIN_USERNAME" { print $2 }' "$ENV_FILE")
web_port=${web_port:-80}
admin_user=${admin_user:-admin}

echo "Deployment completed"
echo "Web: http://localhost:${web_port}"
echo "Admin user: ${admin_user}"
echo "Admin password is stored in .env"
