#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/vyom-complain"

description="$(build_description "control" "high" "P0" "log: fake failure" "Open Settings then tap Control" "The target screen opens" "read_screen returns the Settings screen" "native change needing an APK" "Control action fails")"
for field in "**Source:** termux" "**Area:** control" "**Severity:** high" "**Priority:** P0" "**Evidence:** log: fake failure" "**Repro steps:** Open Settings then tap Control" "**Expected result:** The target screen opens" "**Verification probe:** read_screen returns the Settings screen" "**Fix type:** native change needing an APK" "Control action fails"; do
  [[ "$description" == *"$field"* ]] || { echo "Missing field: $field" >&2; exit 1; }
done

for area in control android_control memory design personality repo skill skills schedule voice update whatsapp play_store floating_orb other; do
  valid_area "$area" || { echo "Supported area rejected: $area" >&2; exit 1; }
done
[[ "$(default_priority control)" == "P0" ]] || { echo "Control must default to P0" >&2; exit 1; }
[[ "$(default_priority android_control)" == "P0" ]] || { echo "Android control must default to P0" >&2; exit 1; }
[[ "$(default_priority update)" == "P0" ]] || { echo "Update must default to P0" >&2; exit 1; }
[[ "$(default_priority memory)" == "P1" ]] || { echo "Memory must default to P1" >&2; exit 1; }
[[ "$(default_priority design)" == "P1" ]] || { echo "Design must default to P1" >&2; exit 1; }
[[ "$(default_priority voice)" == "P2" ]] || { echo "Voice must default to P2" >&2; exit 1; }
[[ "$(default_priority other)" == "P3" ]] || { echo "Unknown area must default to P3" >&2; exit 1; }
if valid_area "not-an-area"; then echo "Invalid area accepted" >&2; exit 1; fi
if valid_priority "P9"; then echo "Invalid priority accepted" >&2; exit 1; fi
if valid_severity "urgent"; then echo "Invalid severity accepted" >&2; exit 1; fi
echo "Factory complaint schema test PASS"
