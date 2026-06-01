#!/bin/sh
set -eu

java -jar /app/platform.jar &
backend_pid="$!"

term() {
  kill "$backend_pid" 2>/dev/null || true
  nginx -s quit 2>/dev/null || true
}

trap term INT TERM

nginx -g 'daemon off;' &
nginx_pid="$!"

wait -n "$backend_pid" "$nginx_pid"
term
