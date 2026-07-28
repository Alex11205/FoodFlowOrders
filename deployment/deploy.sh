#!/usr/bin/env bash

set -Eeuo pipefail

IMAGE_TAG="${1:-}"
DEPLOY_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$DEPLOY_ROOT/compose.prod.yaml"
ENV_FILE="$DEPLOY_ROOT/.env"
AWS_REGION="${AWS_REGION:-ca-central-1}"

if [[ ! "$IMAGE_TAG" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Usage: deploy.sh <full-git-commit-sha>" >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Missing $COMPOSE_FILE" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE. Keep the production secrets and ECR_REGISTRY there." >&2
  exit 1
fi

DEPLOYED_COMMIT="$(git -C "$DEPLOY_ROOT" rev-parse HEAD)"
if [[ "$DEPLOYED_COMMIT" != "$IMAGE_TAG" ]]; then
  echo "Checkout $DEPLOYED_COMMIT does not match image tag $IMAGE_TAG" >&2
  exit 1
fi

if grep -q '^IMAGE_TAG=' "$ENV_FILE"; then
  sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$IMAGE_TAG/" "$ENV_FILE"
else
  printf '\nIMAGE_TAG=%s\n' "$IMAGE_TAG" >> "$ENV_FILE"
fi

ECR_REGISTRY="$(sed -n 's/^ECR_REGISTRY=//p' "$ENV_FILE" | tail -n 1)"
if [[ -z "$ECR_REGISTRY" ]]; then
  echo "ECR_REGISTRY is missing from $ENV_FILE" >&2
  exit 1
fi

aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$ECR_REGISTRY"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --quiet
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up \
  -d \
  --remove-orphans \
  --wait \
  --wait-timeout 300
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps
