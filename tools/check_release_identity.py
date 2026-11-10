#!/usr/bin/env python3
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
identity = (root / "config/release-certificate.sha256").read_text().strip().lower()
if not re.fullmatch(r"[0-9a-f]{64}", identity):
    raise SystemExit("Invalid canonical release certificate fingerprint")
gradle = (root / "app/build.gradle.kts").read_text()
activity = (root / "app/src/main/java/com/hermes/assistant/MainActivity.kt").read_text()
release = (root / ".github/workflows/vyom-android-release.yml").read_text()
if 'config/release-certificate.sha256' not in gradle or 'REQUIRED_RELEASE_CERT_SHA256' not in gradle:
    raise SystemExit("Gradle must generate the app trust identity from the canonical file")
if 'BuildConfig.REQUIRED_RELEASE_CERT_SHA256' not in activity:
    raise SystemExit("MainActivity must use the generated canonical release identity")
if 'config/release-certificate.sha256' not in release or 'EXPECTED_CERT_SHA256' not in release:
    raise SystemExit("Release workflow must enforce the canonical release identity")
old = "7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a"
if old in gradle + activity + release:
    raise SystemExit("Obsolete release certificate identity remains in active source")
print("RELEASE_IDENTITY_SOURCE_CHECK=PASS")
