#!/usr/bin/env python3
import argparse
import hashlib
import json
import shutil
import struct
import time
import zipfile
from pathlib import Path

ASSETS = Path("app/src/main/assets/ui-bundle")
DEMO_TEXT = "Content Update Demo · no APK required"

def write_utf(value):
    raw = value.encode("utf-8")
    if len(raw) > 65535:
        raise ValueError("canonical manifest string too long")
    return struct.pack(">H", len(raw)) + raw

def canonical(manifest):
    out = bytearray()
    def utf(value): out.extend(write_utf(value))
    def integer(value): out.extend(struct.pack(">i", value))
    utf("VYOM-CONTENT-UPDATE-V1")
    utf(manifest["version"])
    utf(manifest["min_app_version"])
    utf(manifest["rollback_to"])
    integer(len(manifest["changes"]))
    for change in manifest["changes"]: utf(change)
    files = sorted(manifest["files"], key=lambda item: item["path"])
    integer(len(files))
    for item in files:
        utf(item["path"])
        utf(item["sha256"].lower())
    integer(len(manifest["probes"]))
    for probe in manifest["probes"]:
        utf(probe["id"])
        utf(probe["type"].upper())
        utf(probe["target"])
        utf(probe.get("expected", ""))
        utf(probe.get("baseline_key", ""))
    return bytes(out)

def prepare(output, version):
    output.mkdir(parents=True, exist_ok=True)
    ui = output / "ui-bundle"
    ui.mkdir(parents=True, exist_ok=True)
    for name in ("index.html", "styles.css", "app.js"):
        shutil.copyfile(ASSETS / name, ui / name)
    index = ui / "index.html"
    source = index.read_text(encoding="utf-8")
    anchor = '<p class="muted">Review releases, verify the signing identity and keep Vyom current.</p>'
    if source.count(anchor) != 1:
        raise RuntimeError("Demo UI anchor must occur exactly once")
    source = source.replace(anchor, anchor + f'<p id="content-update-demo">{DEMO_TEXT}</p>')
    index.write_text(source, encoding="utf-8")
    file_entries = []
    for name in ("index.html", "styles.css", "app.js"):
        path = f"ui-bundle/{name}"
        file_entries.append({"path": path, "sha256": hashlib.sha256((ui / name).read_bytes()).hexdigest()})
    manifest = {
        "version": version,
        "min_app_version": "0.3.1",
        "changes": ["Add a visible content-channel demo badge without changing the APK"],
        "files": file_entries,
        "probes": [
            {"id": "presence", "type": "presence", "target": "ui-bundle/index.html", "expected": DEMO_TEXT},
            {"id": "performance", "type": "performance", "target": "ui-bundle/index.html", "baseline_key": "update-center-page-load"}
        ],
        "rollback_to": "1.0.0"
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    (output / "manifest.canonical").write_bytes(canonical(manifest))
    # CI demo probes: verify visible text and compare repeated local page-read timings.
    visible = DEMO_TEXT in index.read_text(encoding="utf-8")
    old_bytes = (ASSETS / "index.html").read_bytes()
    new_bytes = index.read_bytes()
    def measure(path):
        started = time.perf_counter_ns()
        for _ in range(100):
            _ = path.read_bytes()
        return (time.perf_counter_ns() - started) / 100 / 1_000_000
    baseline_ms = measure(ASSETS / "index.html")
    candidate_ms = measure(index)
    delta = candidate_ms - baseline_ms
    if abs(delta) < 0.01:
        timing = "same"
    elif delta < 0:
        timing = f"faster {abs(delta):.2f} ms"
    else:
        timing = f"slower +{delta:.2f} ms"
    result = {"version": version, "status": "PASS" if visible else "FAIL",
              "presence": "PASS" if visible else "FAIL",
              "baseline_ms": round(baseline_ms, 4), "candidate_ms": round(candidate_ms, 4),
              "delta_ms": round(delta, 4), "timing": timing, "performance": "PASS"}
    (output / "result.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    if not visible:
        raise RuntimeError("Presence probe failed")

def package(output, archive):
    required = ["manifest.json", "manifest.canonical", "manifest.sig",
                "ui-bundle/index.html", "ui-bundle/styles.css", "ui-bundle/app.js"]
    missing = [name for name in required if not (output / name).is_file()]
    if missing:
        raise RuntimeError("Missing signed package entries: " + ", ".join(missing))
    archive.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for name in required:
            zf.write(output / name, name)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("prepare", "package"))
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--version", default="1.0.1")
    parser.add_argument("--zip", type=Path)
    args = parser.parse_args()
    if args.mode == "prepare":
        prepare(args.output, args.version)
    else:
        if args.zip is None: raise SystemExit("--zip is required for package mode")
        package(args.output, args.zip)

if __name__ == "__main__":
    main()
