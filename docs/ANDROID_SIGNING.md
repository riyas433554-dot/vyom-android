# Android APK Signing & Update Continuity

## Required release identity

- Android application ID: `com.vyom.assistant` (must not change).
- Kotlin/Android namespace: `com.hermes.assistant` (must not change).
- Base version: `0.3.0`, versionCode `3`.
- Next update version: `0.3.1`, versionCode `4`.
- Expected signing certificate SHA-256 from the user-supplied 19-page Weakness Audit (build `2459bf1`):
  `7A:75:64:FC:A9:C2:A3:2B:23:DD:52:03:52:A4:E3:AA:FE:44:56:21:D2:B9:9A:FA:15:40:7D:23:86:69:9F:0A`.

## Independently inspected prior artifact

- GitHub Actions artifact: `13146103223`, workflow run `41455029509`, source commit `7e70370d6d9d134f3fa4ba0878636a3f658f275f`.
- APK: `vyom-0.3.0-7e70370.apk`.
- APK SHA-256: `6aa6244ed2a8511234c0103c95a36994ca2df76f514695e19a569f5b03562406`; independently matched the artifact's `.sha256` file.
- Parsed the APK v2 signing block and extracted the X.509 certificate. Actual certificate SHA-256:
  `BF:41:32:C9:D3:4A:2F:80:CF:03:77:0C:2D:5A:85:A8:04:AA:A8:6B:D8:DF:9B:62:84:C4:2A:2D:C8:38:6E:95`.
- This is NOT the required `7A:75…9F:0A` identity. Do not install this APK over the base installation.

## Workflow protections added

- Accepts both secret naming conventions for alias/password: `ANDROID_KEY_ALIAS` or `KEY_ALIAS`; `ANDROID_KEY_PASSWORD` or `KEY_PASSWORD`.
- Normalizes base64 whitespace/URL-safe characters and validates the keystore/alias/password without printing secret values.
- Exports all validated signing values to later Gradle steps and separately checks that those values are visible in the Gradle build step.
- Builds version `0.3.1` / code `4`.
- Runs `apksigner verify --verbose --print-certs` and fails the release packaging step unless the actual certificate SHA-256 equals the expected `7A75…9F0A`.
- Writes the actual certificate fingerprint to the release artifact's `build-info.txt` and certificate report. The signed artifact is not uploaded when the fingerprint mismatches.

## Release acceptance

1. Run Debug + full CI on the exact final feature-branch commit.
2. Download the signed release artifact; verify its SHA-256 against the sidecar.
3. Independently inspect the downloaded APK certificate; it must equal the expected fingerprint above.
4. Verify package name and version: `com.vyom.assistant`, `0.3.1`, code `4`.
5. Only then install as an update over the existing app and confirm user data remains.
6. Never uninstall the existing app as a workaround. Never provide the debug-signed APK as the final update.

## Current status

**UNVERIFIED / RELEASE BLOCKED.** Workflow and version changes are committed, but no successful exact-head signed artifact, certificate comparison, full CI run, or phone update test has been observed yet. The original `2459bf1` APK bytes were not present in the Library during this session; the expected fingerprint is taken from the supplied audit and must still match the installed APK at release time.


## Update-first pipeline extension — 2026-11-03

- The release workflow now validates the persistent keystore/alias/key password, then verifies the signed APK certificate fingerprint against the required stable identity before publishing. It emits `certificateSha256`, source commit, SHA-256 and APK URL in `vyom-update.json`.
- Pushes to the development branch and `vyom-proposal-*` branches are configured to publish signed prerelease assets only after the Debug workflow's tests/build/signing checks succeed. The in-app updater scans GitHub releases (including prereleases) and rejects manifests missing SHA-256, certificate fingerprint or full source commit.
- CI branch and tagged stable-release version codes now derive from the repository-wide unique GitHub Actions run ID modulo 2,000,000,000. This keeps version codes ordered across separate workflows and within Android's signed 32-bit versionCode limit. Local Gradle defaults remain debug/dev values only.
- Repository secrets were present and keytool validated the keystore, alias and key password in Debug run `41597804231` (steps “Configure persistent release signing when repository secrets are available” and “Verify release signing environment for Gradle”). That run then failed at `UiBundleIntegrityTest.computesStableSha256ForUtf8BundleContent` due to a test assertion comparing a full SHA-256 with a truncated 32-character value. The test was corrected in commit `705a0c162529b216547f63c9ffc444de41d0c481`; later exact-head CI is required to verify it.
- Expected signing certificate identity remains `7A:75:64:FC:A9:C2:A3:2B:23:DD:52:03:52:A4:E3:AA:FE:44:56:21:D2:B9:9A:FA:15:40:7D:23:86:69:9F:0A`. No successful signed artifact or owner-device certificate match is claimed yet.

## GITHUB_TOKEN-only release publishing — 2026-11-03

Both signed publishing jobs explicitly declare job-level `permissions: contents: write` and pass only `${{ secrets.GITHUB_TOKEN }}` to the GitHub Release action. The previous optional `VYOM_RELEASE_TOKEN` personal-token fallback has been removed. Keystore and signing values continue to come only from Android signing repository secrets; no personal GitHub token is embedded in YAML.

If GitHub still returns HTTP 403 `Resource not accessible by integration`, open this repository's **Settings → Actions → General → Workflow permissions**, select **Read and write permissions**, and Save. Re-run the workflow after changing the setting. Do not create or add a PAT for release publishing.

Exact source SHA `36e9c5c431a59cbfcc05efb3e66baa1f48d7d551`: Full CI run `41625535668` SUCCESS; Debug run `41625535813` SUCCESS, including the release publish step. Downloaded artifact `13226900200` was independently unpacked: signed APK SHA-256 `d220b3900e92aefdb3184823171e21d0f2304bcda0c4b6688c2ac4723a8daa79` matches its sidecar; v2 signature is valid; certificate SHA-256 is `7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a`, matching the required release identity. Release tag `vyom-ci-36e9c5c431a59cbfcc05efb3e66baa1f48d7d551` exists and contains the signed APK, checksum, certificate report and `vyom-update.json` manifest. This does NOT prove the certificate matches the APK currently installed on the owner's phone; that remains a pre-install gate.
