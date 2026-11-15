#!/usr/bin/env sh
# 构建并启动本应用容器（前端 + 后端）。
# 后端镜像依赖宿主机预构建的 jar，因此顺序为：校验 jar -> docker build 后端镜像 -> compose 启动。
set -eu

case "$0" in
  */*) script_path=$0 ;;
  *) script_path=$(command -v "$0") || exit 1 ;;
esac
PROJECT_DIR=$(CDPATH= cd "${script_path%/*}/.." && pwd)
BACKEND_IMAGE=notebook-backend:local

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "Required command not found: docker"
docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 is required"

cd "$PROJECT_DIR"

[ -f backend/target/ai-scaffold-backend.jar ] || fail "Backend jar not found: backend/target/ai-scaffold-backend.jar. Build it first (cd backend && mvn -B clean package) or upload the jar from your build machine"

[ -f frontend/dist/index.html ] || fail "Frontend dist not found: frontend/dist. Build it first (cd frontend && npm install && npm run build) or upload the dist directory from your build machine"

docker network inspect ai-scaffold_ai-network >/dev/null 2>&1 || fail "Shared network ai-scaffold_ai-network not found. Start 619414569应用 first: cd ../619414569应用 && docker compose up -d"

docker compose config --quiet

echo "Building backend image ${BACKEND_IMAGE} from prebuilt jar"
docker build -t "$BACKEND_IMAGE" ./backend

docker compose up -d --build --remove-orphans
docker compose ps

echo "Deployment completed"
echo "Web: http://localhost:81"
echo "Backend (loopback only): http://127.0.0.1:8081/actuator/health"
