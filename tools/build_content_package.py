#!/usr/bin/env python3
"""Prepare and archive the signed UI content package; signing is a separate CI-only Java step."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/ui-bundle"
ASSET_NAMES = ("index.html", "styles.css", "app.js")

def write_utf(value: str) -> bytes:
    encoded = value.encode("utf-8")
    if len(encoded) > 65535:
        raise ValueError("canonical field exceeds modified-UTF length limit")
    return struct.pack(">H", len(encoded)) + encoded

def canonical_payload(manifest: dict) -> bytes:
    out = bytearray()
    def utf(value: str) -> None:
        out.extend(write_utf(value))
    def integer(value: int) -> None:
        out.extend(struct.pack(">i", value))
    utf("VYOM-CONTENT-UPDATE-V1")
    utf(manifest["version"])
    utf(manifest["min_app_version"])
    utf(manifest["rollback_to"])
    integer(len(manifest["changes"]))
    for change in manifest["changes"]:
        utf(change)
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

def prepare(output: Path, version: str) -> None:
    if not version or not all(part.isdigit() for part in version.split(".")) or len(version.split(".")) != 3:
        raise ValueError("content version must be numeric major.minor.patch")
    files = []
    files_dir = output / "files" / "ui-bundle"
    files_dir.mkdir(parents=True, exist_ok=True)
    for name in ASSET_NAMES:
        source = ASSETS / name
        if not source.is_file():
            raise FileNotFoundError(source)
        data = source.read_bytes()
        files.append({"path": f"ui-bundle/{name}", "sha256": hashlib.sha256(data).hexdigest()})
        (files_dir / name).write_bytes(data)
    manifest = {
        "version": version, "min_app_version": "0.3.1",
        "changes": ["Refresh the signed Vyom UI bundle"], "files": files,
        "probes": [
            {"id": "presence", "type": "presence", "target": "ui-bundle/index.html",
             "expected": "Vyom Update Center"},
            {"id": "performance", "type": "performance", "target": "ui-bundle/index.html",
             "baseline_key": "ui-bundle-index-read-v1"}
        ],
        "rollback_to": "previous"
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    (output / "manifest.canonical").write_bytes(canonical_payload(manifest))

def archive(output: Path) -> Path:
    signature = output / "manifest.sig"
    if not signature.is_file() or not signature.stat().st_size:
        raise FileNotFoundError("CI signer did not produce manifest.sig")
    target = output.parent / "vyom-content-update-demo.zip"
    with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED) as package:
        for name in ("manifest.json", "manifest.canonical", "manifest.sig"):
            package.write(output / name, name)
        for name in ASSET_NAMES:
            relative = f"ui-bundle/{name}"
            package.write(output / "files" / relative, relative)
    return target

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version")
    parser.add_argument("--prepare", action="store_true")
    parser.add_argument("--archive", action="store_true")
    args = parser.parse_args()
    if args.prepare == args.archive:
        parser.error("choose exactly one of --prepare or --archive")
    if args.prepare:
        prepare(args.output, args.version or "")
    else:
        print(f"CONTENT_PACKAGE={archive(args.output).name}")

if __name__ == "__main__":
    main()
