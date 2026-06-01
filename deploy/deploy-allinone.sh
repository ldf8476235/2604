#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

REGISTRY_HOST="${REGISTRY_HOST:-crpi-e6nng8qj0ou5u67r.cn-hangzhou.personal.cr.aliyuncs.com}"
IMAGE_REPO="${IMAGE_REPO:-$REGISTRY_HOST/webfeng/webfeng}"
IMAGE_TAG="${IMAGE_TAG:-allinone-$(date +%Y%m%d%H%M%S)}"
REMOTE_HOST="${REMOTE_HOST:-menghu-prod}"
REMOTE_DIR="${REMOTE_DIR:-/opt/delta-trade}"
PLATFORM="${PLATFORM:-linux/amd64}"

IMAGE="$IMAGE_REPO:$IMAGE_TAG"
LATEST_IMAGE="$IMAGE_REPO:allinone-latest"
BUILD_DIR="$ROOT_DIR/.codex-temp/registry-build-$IMAGE_TAG"

log() {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_cmd npm
require_cmd mvn
require_cmd docker
require_cmd ssh
require_cmd curl

log "Building frontends"
npm run build:frontends

log "Building backend jar"
mvn -f backend/pom.xml -DskipTests package

log "Preparing Docker build context: $BUILD_DIR"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/backend" "$BUILD_DIR/nginx" "$BUILD_DIR/allinone"
cp deploy/docker/allinone.Dockerfile "$BUILD_DIR/Dockerfile"
cp deploy/docker/allinone/start.sh "$BUILD_DIR/allinone/start.sh"
cp deploy/nginx/default.conf "$BUILD_DIR/nginx/default.conf"
cp backend/target/platform-0.1.0.jar "$BUILD_DIR/backend/platform-0.1.0.jar"
cp -R apps/web/dist "$BUILD_DIR/web"
cp -R apps/admin/dist "$BUILD_DIR/admin"
cp -R apps/studio/dist "$BUILD_DIR/studio"

log "Building and pushing Docker image: $IMAGE ($PLATFORM)"
docker buildx build \
  --platform "$PLATFORM" \
  -t "$IMAGE" \
  -t "$LATEST_IMAGE" \
  --push \
  "$BUILD_DIR"

log "Deploying on $REMOTE_HOST"
scp deploy/nginx/default.conf "$REMOTE_HOST:$REMOTE_DIR/nginx/default.conf"
ssh "$REMOTE_HOST" "set -euo pipefail
cd '$REMOTE_DIR'
docker pull '$IMAGE'
docker tag '$IMAGE' '$LATEST_IMAGE'
docker compose -f docker-compose.yml --env-file .env.prod stop app
docker compose -f docker-compose.yml --env-file .env.prod rm -f app
docker compose -f docker-compose.yml --env-file .env.prod up -d app caddy
sleep 20
docker compose -f docker-compose.yml --env-file .env.prod ps
docker image inspect '$IMAGE' --format 'deployed {{.Id}}'
"

log "Verifying public endpoints"
curl -fsSL --max-time 15 http://49.232.154.165:8901/ | grep -q '<title>萌虎</title>'
curl -fsSL --max-time 15 http://49.232.154.165:8901/beifanghanzi/ | grep -q '<title>萌虎管理后台</title>'
curl -fsSI --max-time 15 http://49.232.154.165:8901/brand/menghu-icon.jpg >/dev/null
curl -fsS --max-time 15 http://49.232.154.165:8902/actuator/health | grep -q '"status":"UP"'

log "Deployment completed"
echo "Image: $IMAGE"
echo "User:  http://49.232.154.165:8901/"
echo "Admin: http://49.232.154.165:8901/beifanghanzi/"
