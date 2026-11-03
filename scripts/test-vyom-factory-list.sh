#!/usr/bin/env bash
set -euo pipefail

command -v jq >/dev/null 2>&1 || { echo "jq is required" >&2; exit 1; }
fixture="$(mktemp)"
trap 'rm -f "$fixture"' EXIT
cat > "$fixture" <<'JSON'
[
  [
    {
      "number": 41,
      "state": "open",
      "title": "[VYOM FACTORY][P2] Running task",
      "html_url": "https://github.com/example/repo/issues/41",
      "body": "Details\n\n<!-- VYOM_FACTORY_META:{\"schema\":1,\"priority\":\"P2\",\"status\":\"in_progress\",\"source\":\"termux\",\"source_id\":\"test-41\",\"idempotency_key\":\"termux:test-41\",\"events\":[\"created\"]} -->"
    },
    {
      "number": 42,
      "state": "closed",
      "title": "[VYOM FACTORY][P1] Finished task",
      "html_url": "https://github.com/example/repo/issues/42",
      "body": "Details\n\n<!-- VYOM_FACTORY_META:{\"schema\":1,\"priority\":\"P1\",\"status\":\"completed\",\"source\":\"factory\",\"source_id\":\"test-42\",\"idempotency_key\":\"factory:test-42\",\"events\":[\"created\"]} -->"
    },
    {
      "number": 43,
      "state": "open",
      "title": "Ordinary issue",
      "html_url": "https://github.com/example/repo/issues/43",
      "body": "Not a Factory task"
    },
    {
      "number": 44,
      "state": "open",
      "title": "Malformed Factory marker",
      "html_url": "https://github.com/example/repo/issues/44",
      "body": "<!-- VYOM_FACTORY_META:{bad} -->"
    }
  ]
]
JSON

actual="$(jq -r 'add[] |
  select((.pull_request|not) and ((.body // "") | contains("<!-- VYOM_FACTORY_META:"))) |
  . as $issue |
  (try (($issue.body | capture("<!-- VYOM_FACTORY_META:(?<meta>.*) -->"; "s").meta) | fromjson) catch null) as $meta |
  select($meta.schema == 1 and
    ($meta.priority | IN("P0","P1","P2","P3")) and
    ($meta.status | IN("open","in_progress","blocked","paused","completed","cancelled"))) |
  [$issue.number, $meta.status, $meta.priority, $issue.title, $issue.html_url] | @tsv' "$fixture")"
expected=$'41\tin_progress\tP2\t[VYOM FACTORY][P2] Running task\thttps://github.com/example/repo/issues/41\n42\tcompleted\tP1\t[VYOM FACTORY][P1] Finished task\thttps://github.com/example/repo/issues/42'
[[ "$actual" == "$expected" ]] || { printf 'Expected:\n%s\nActual:\n%s\n' "$expected" "$actual" >&2; exit 1; }
echo "Factory list metadata test PASS"
