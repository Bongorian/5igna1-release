"""Check native translations without requiring Android or third-party packages."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

res = Path(__file__).resolve().parents[1] / "app/src/main/res"
translations = {}
for directory in ("values", "values-ja", "values-zh"):
    entries = ET.parse(res / directory / "strings.xml").getroot()
    strings = {entry.attrib["name"]: entry.text or "" for entry in entries}
    assert len(entries) == len(strings), f"Duplicate keys in {directory}"
    assert all(value.strip('"').strip() for value in strings.values()), directory
    translations[directory] = strings

base = translations["values"]
# These resources use simple Java Formatter conversions, with no space flag.
placeholder = re.compile(r"%(?:\d+\$)?[-#+0,(]*\d*(?:\.\d+)?[a-zA-Z%]")
for directory, strings in translations.items():
    assert strings.keys() == base.keys(), f"Missing or extra keys in {directory}"
    for key, value in strings.items():
        assert placeholder.findall(value) == placeholder.findall(base[key]), (directory, key)

arrays = ET.parse(res / "values/effect_strings.xml").getroot()
expected = {"effect_descriptions": 14}
for array in arrays:
    assert len(array) == expected[array.attrib["name"]]
    for item in array:
        assert item.text == '""' or (item.text.startswith("@string/") and item.text[8:] in base), item.text
print(f"PASS: {len(base)} translated strings, matching format placeholders and effect arrays")

# Dynamic fault-control labels must exist in every language.
controls = res.parent / "java/com/bongorian/signa1/Effects.java"
for key in re.findall(r'c\("([^"\n]+)"', controls.read_text()):
    assert "fault_control_" + key in base, key
