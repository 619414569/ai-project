#!/usr/bin/env sh
# 启动本应用容器（前端 + 后端）。
# 前置条件：同机 619414569应用 的 PostgreSQL / Redis / RocketMQ 容器已运行，
#          共享网络 ai-scaffold_ai-network 已存在，且独立库已按
#          数据/初始化/01_create_database_shared_instance.sql 创建并获得确认。
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
  set_env_value JWT_SECRET "$(random_hex 48)"
  echo "Created $ENV_FILE with a random JWT_SECRET"
  echo "Fill DB_PASSWORD and REDIS_PASSWORD (must match 619414569应用) before re-running"
fi

chmod 600 "$ENV_FILE"

for key in DB_PASSWORD REDIS_PASSWORD JWT_SECRET; do
  require_env_value "$key"
done

jwt_secret=$(get_env_value JWT_SECRET)
[ "${#jwt_secret}" -ge 32 ] || fail "JWT_SECRET must contain at least 32 characters"

if grep -Eq '=(change-me-|replace-this-)' "$ENV_FILE"; then
  fail "Unsafe default infrastructure credentials remain in .env"
fi

shared_network=$(get_env_value SHARED_NETWORK_NAME)
shared_network=${shared_network:-ai-scaffold_ai-network}
docker network inspect "$shared_network" >/dev/null 2>&1 || fail "Shared network '$shared_network' not found. Start the 619414569应用 middleware first: cd ../619414569应用 && docker compose up -d postgres redis rocketmq-namesrv rocketmq-broker"

docker compose config --quiet
docker compose up -d --build --remove-orphans

wait_for_health backend
wait_for_health frontend

web_port=$(get_env_value WEB_PORT)
web_port=${web_port:-81}
backend_port=$(get_env_value BACKEND_PORT)
backend_port=${backend_port:-8081}

echo "Deployment completed"
echo "Web: http://localhost:${web_port}"
echo "Backend (loopback only): http://127.0.0.1:${backend_port}/actuator/health"
