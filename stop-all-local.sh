#!/usr/bin/env bash
# Kills every service started by start-all-local.sh, using the PID files
# it wrote to ./logs/*.pid. Run from the repo root: ./stop-all-local.sh

set -euo pipefail
cd "$(dirname "$0")"

if [ ! -d logs ] || [ -z "$(ls -A logs/*.pid 2>/dev/null)" ]; then
  echo "No running services found (no logs/*.pid files)."
  exit 0
fi

for pidfile in logs/*.pid; do
  service=$(basename "$pidfile" .pid)
  pid=$(cat "$pidfile")
  if kill -0 "$pid" 2>/dev/null; then
    echo "Stopping $service (pid $pid)..."
    kill "$pid"
  else
    echo "$service (pid $pid) already stopped."
  fi
  rm -f "$pidfile"
done

echo "Done."
