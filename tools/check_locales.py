#!/usr/bin/env python3
"""Check Vietnamese resource coverage and formatting without Android tooling."""
from collections import Counter
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"


def read_strings(directory):
    strings = {}
    for filename in ("strings.xml", "feature_strings.xml"):
        for item in ET.parse(directory / filename).getroot().findall("string"):
            name = item.attrib["name"]
            assert name not in strings, f"{directory}: duplicate string {name}"
            value = "".join(item.itertext())
            assert value.strip(), f"{directory}: empty string {name}"
            strings[name] = value
    return strings


base = read_strings(RES / "values")
vietnamese = read_strings(RES / "values-vi")
assert base.keys() == vietnamese.keys(), (
    f"Vietnamese keys differ: missing {base.keys() - vietnamese.keys()}, "
    f"extra {vietnamese.keys() - base.keys()}"
)
for name, value in base.items():
    placeholders = lambda text: Counter(re.findall(r"%(?:\d+\$)?[sd]", text))
    assert placeholders(value) == placeholders(vietnamese[name]), f"{name}: placeholder mismatch"
for name in ("app_name", "english", "simplified_chinese", "default_key", "default_required_item"):
    assert vietnamese[name] == base[name], f"{name}: technical value or native name changed"

locales = ET.parse(RES / "xml/locales_config.xml").getroot()
tags = [item.attrib["{http://schemas.android.com/apk/res/android}name"] for item in locales]
assert "vi" in tags, "Vietnamese is missing from Android's locale list"
assert len(tags) == len(set(tags)), "Duplicate Android locales"
activity = (ROOT / "app/src/main/java/com/reed/fcmguard/MainActivity.java").read_text(encoding="utf-8")
codes_block = re.search(r"LANGUAGE_CODES\s*=\s*\{(.*?)\};", activity, re.S)
codes = re.findall(r'"([^"]+)"', codes_block.group(1))
assert codes == ["system", *tags], "Picker codes differ from registered locales"
native_names = {
    "system": "@string/follow_system", "vi": "Tiếng Việt", "en": "English",
    "zh-CN": "简体中文", "zh-TW": "繁體中文", "fr": "Français", "ja": "日本語",
    "ko": "한국어", "es": "Español", "pt": "Português", "de": "Deutsch", "ru": "Русский",
}
entries = ET.parse(RES / "values/arrays.xml").findall("string-array[@name='language_entries']/item")
assert [item.text for item in entries] == [native_names[code] for code in codes], (
    "Picker labels do not match language codes"
)
print(f"Vietnamese locale checks passed: {len(vietnamese)} strings")
