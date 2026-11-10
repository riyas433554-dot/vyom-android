import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("build_content_package", ROOT / "tools/build_content_package.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)

class ContentPackageBuilderTest(unittest.TestCase):
    def test_java_data_output_utf_encoding_is_length_prefixed(self):
        self.assertEqual(b"\x00\x05hello", MODULE.write_utf("hello"))

    def test_canonical_payload_is_stable_for_same_manifest(self):
        manifest = {
            "version": "1.0.7", "min_app_version": "0.3.1", "rollback_to": "previous",
            "changes": ["Refresh UI"],
            "files": [{"path": "ui-bundle/index.html", "sha256": "a" * 64}],
            "probes": [{"id": "presence", "type": "presence", "target": "ui-bundle/index.html",
                        "expected": "Vyom", "baseline_key": ""}]
        }
        self.assertEqual(MODULE.canonical_payload(manifest), MODULE.canonical_payload(manifest))
        self.assertTrue(MODULE.canonical_payload(manifest).startswith(MODULE.write_utf("VYOM-CONTENT-UPDATE-V1")))

    def test_prepared_package_contains_exact_ui_assets_and_manifest(self):
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / "content"
            MODULE.prepare(output, "1.0.7")
            import json
            manifest = json.loads((output / "manifest.json").read_text())
            self.assertEqual(
                {"ui-bundle/index.html", "ui-bundle/styles.css", "ui-bundle/app.js"},
                {item["path"] for item in manifest["files"]}
            )
            self.assertTrue((output / "manifest.canonical").is_file())

if __name__ == "__main__":
    unittest.main()
