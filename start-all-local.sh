#!/usr/bin/env bash
# Starts every service in the background with plain `mvn spring-boot:run`,
# no Docker involved. Logs go to ./logs/<service>.log; PIDs go to
# ./logs/<service>.pid so stop-all-local.sh can clean them up.
#
# Run this from the repo root: ./start-all-local.sh
# Requires: JDK 17+, Maven, and .env already filled in (see .env.example).
# Load .env into the environment first:
#   export $(grep -v '^#' .env | xargs)
#   ./start-all-local.sh

set -euo pipefail
cd "$(dirname "$0")"

mkdir -p logs

# Order matters: discovery-server first (everything else registers with
# it), then auth-service (everyone else depends on it for users/ELO),
# then the rest, then the gateway last since it fronts everyone else.
SERVICES=(
  discovery-server
  config-server
  auth-service
  game-engine-service
  matchmaking-service
  chat-service
  notification-service
  api-gateway
)

for service in "${SERVICES[@]}"; do
  echo "Starting $service..."
  (
    cd "$service"
    nohup mvn -q spring-boot:run > "../logs/${service}.log" 2>&1 &
    echo $! > "../logs/${service}.pid"
  )
  sleep 3  # give each service a moment before the next one starts registering
done

echo ""
echo "All services launching. Tail logs with: tail -f logs/*.log"
echo "Discovery dashboard: http://localhost:8761"
echo "Gateway:             http://localhost:8080"
echo "Stop everything with: ./stop-all-local.sh"
