#!/usr/bin/env bash
set -euo pipefail

REPO="${VYOM_FACTORY_REPO:-riyas433554-dot/vyom-android}"
MARKER="<!-- VYOM_FACTORY_META:"
PRIORITIES="P0 P1 P2 P3"

usage() {
  cat <<'USAGE'
Vyom Factory shared task CLI (requires GitHub CLI + jq)
  vyom-factory.sh create "title" "description" [P0|P1|P2|P3] [source-id]
  vyom-factory.sh list
  vyom-factory.sh edit ISSUE_NUMBER "title" "description"
  vyom-factory.sh priority ISSUE_NUMBER P0|P1|P2|P3
  vyom-factory.sh status ISSUE_NUMBER open|in_progress|blocked|paused|completed|cancelled
  vyom-factory.sh comment ISSUE_NUMBER "event text"

All operations use the same GitHub Issues metadata schema as the Vyom Android app.
USAGE
}

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
need_tools() {
  command -v gh >/dev/null 2>&1 || fail "GitHub CLI is missing. Install it with: pkg install gh"
  command -v jq >/dev/null 2>&1 || fail "jq is missing. Install it with: pkg install jq"
  gh auth status -h github.com >/dev/null 2>&1 || fail "Authenticate once with: gh auth login"
}
valid_priority() { [[ " $PRIORITIES " == *" $1 "* ]]; }
valid_status() { [[ " open in_progress blocked paused completed cancelled " == *" $1 "* ]]; }

issue_json() {
  local number="$1"
  [[ "$number" =~ ^[0-9]+$ ]] || fail "Issue number must be numeric"
  gh api "repos/$REPO/issues/$number"
}
metadata_from_body() {
  jq -er '.body // "" | capture("<!-- VYOM_FACTORY_META:(?<meta>.*) -->"; "s").meta' <<< "$1"
}
description_from_body() {
  jq -r '.body // "" | sub("\\n?\\n?<!-- VYOM_FACTORY_META:.*? -->"; "") | rtrimstr("\\n")' <<< "$1"
}
save_issue() {
  local number="$1" title="$2" body="$3" state="$4"
  gh api -X PATCH "repos/$REPO/issues/$number" -f "title=$title" -f "body=$body" -f "state=$state" >/dev/null
}

create_task() {
  local title="${1:-}" description="${2:-}" priority="${3:-P2}" source_id="${4:-termux-$(date +%s)-$RANDOM}"
  [[ ${#title} -ge 4 && ${#title} -le 120 ]] || fail "Title must be 4–120 characters"
  [[ -n "$description" && ${#description} -le 12000 ]] || fail "Description must be 1–12,000 characters"
  valid_priority "$priority" || fail "Priority must be P0, P1, P2 or P3"
  [[ -n "$source_id" && ${#source_id} -le 120 ]] || fail "Invalid source ID"
  local key="termux:$source_id"
  local meta body created number
  meta="$(jq -cn --arg p "$priority" --arg sid "$source_id" --arg key "$key" \
    '{schema:1,priority:$p,status:"open",source:"termux",source_id:$sid,idempotency_key:$key,events:["created"]}')"
  body="$description

<!-- VYOM_FACTORY_META:$meta -->"
  created="$(gh api "repos/$REPO/issues" -f "title=[VYOM FACTORY][$priority] $title" -f "body=$body")"
  number="$(jq -er '.number' <<< "$created")"
  gh api "repos/$REPO/issues/$number/comments" -f "body=[Vyom Factory] Created from Termux" >/dev/null
  jq -r '"Registered #" + (.number|tostring) + " · " + .html_url' <<< "$created"
}

list_tasks() {
  gh api --paginate --slurp "repos/$REPO/issues?state=all&per_page=100" |
    jq -r 'add[] |
      select((.pull_request|not) and ((.body // "") | contains("<!-- VYOM_FACTORY_META:"))) |
      . as $issue |
      (try (($issue.body | capture("<!-- VYOM_FACTORY_META:(?<meta>.*) -->"; "s").meta) | fromjson) catch null) as $meta |
      select($meta.schema == 1 and
        ($meta.priority | IN("P0","P1","P2","P3")) and
        ($meta.status | IN("open","in_progress","blocked","paused","completed","cancelled"))) |
      [$issue.number, $meta.status, $meta.priority, $issue.title, $issue.html_url] | @tsv'
}

edit_task() {
  local number="$1" title="$2" description="$3" issue meta body next_meta
  issue="$(issue_json "$number")"
  meta="$(metadata_from_body "$issue")"
  [[ -n "$meta" ]] || fail "Issue is not a Vyom Factory task"
  [[ ${#title} -ge 4 && ${#title} -le 120 ]] || fail "Title must be 4–120 characters"
  [[ -n "$description" && ${#description} -le 12000 ]] || fail "Description must be 1–12,000 characters"
  next_meta="$(jq -c --arg e "edited:$(date -u +%FT%TZ)" '.events = ((.events // []) + [$e])[-500:]' <<< "$meta")"
  body="$description

<!-- VYOM_FACTORY_META:$next_meta -->"
  local priority state
  priority="$(jq -r '.priority' <<< "$next_meta")"
  state="$(jq -r '.status' <<< "$next_meta")"
  save_issue "$number" "[VYOM FACTORY][$priority] $title" "$body" "$( [[ "$state" == completed || "$state" == cancelled ]] && echo closed || echo open )"
  gh api "repos/$REPO/issues/$number/comments" -f "body=[Vyom Factory] Edited from Termux" >/dev/null
  printf 'Updated Factory task #%s\n' "$number"
}

update_metadata() {
  local number="$1" field="$2" value="$3" issue meta description next_meta title state priority github_state
  issue="$(issue_json "$number")"
  meta="$(metadata_from_body "$issue")"
  [[ -n "$meta" ]] || fail "Issue is not a Vyom Factory task"
  description="$(description_from_body "$issue")"
  title="$(jq -r '.title' <<< "$issue" | sed -E 's/^\\[VYOM FACTORY\\]\\[[P0-3]\\] //')"
  state="$(jq -r '.status' <<< "$meta")"
  priority="$(jq -r '.priority' <<< "$meta")"
  if [[ "$field" == priority ]]; then
    valid_priority "$value" || fail "Priority must be P0, P1, P2 or P3"
    priority="$value"
  else
    valid_status "$value" || fail "Unknown status"
    case "$state:$value" in
      open:blocked|open:paused|open:cancelled|in_progress:open|in_progress:blocked|in_progress:paused|in_progress:completed|in_progress:cancelled|blocked:open|blocked:cancelled|paused:open|paused:cancelled) state="$value" ;;
      *) [[ "$state" == "$value" ]] || fail "Transition $state -> $value is not allowed" ;;
    esac
  fi
  next_meta="$(jq -c --arg p "$priority" --arg s "$state" --arg e "$field:$value:$(date -u +%FT%TZ)" '.priority=$p | .status=$s | .events=((.events // []) + [$e])[-500:]' <<< "$meta")"
  body="$description

<!-- VYOM_FACTORY_META:$next_meta -->"
  github_state=open
  [[ "$state" == completed || "$state" == cancelled ]] && github_state=closed
  save_issue "$number" "[VYOM FACTORY][$priority] $title" "$body" "$github_state"
  gh api "repos/$REPO/issues/$number/comments" -f "body=[Vyom Factory] $field changed to $value from Termux" >/dev/null
  printf 'Updated Factory task #%s · %s=%s\n' "$number" "$field" "$value"
}

main() {
  [[ $# -gt 0 ]] || { usage; exit 2; }
  need_tools
  case "$1" in
    create) [[ $# -ge 3 ]] || fail "create requires title and description"; create_task "$2" "$3" "${4:-P2}" "${5:-}" ;;
    list) list_tasks ;;
    edit) [[ $# -eq 4 ]] || fail "edit requires issue number, title and description"; edit_task "$2" "$3" "$4" ;;
    priority) [[ $# -eq 3 ]] || fail "priority requires issue number and P0..P3"; update_metadata "$2" priority "$3" ;;
    status) [[ $# -eq 3 ]] || fail "status requires issue number and status"; update_metadata "$2" status "$3" ;;
    comment) [[ $# -eq 3 ]] || fail "comment requires issue number and text"; gh api "repos/$REPO/issues/$2/comments" -f "body=[Vyom Factory] $3" >/dev/null; printf 'Comment added to #%s\n' "$2" ;;
    help|-h|--help) usage ;;
    *) usage; fail "Unknown command: $1" ;;
  esac
}
main "$@"
