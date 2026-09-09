#!/usr/bin/env python3
"""Check local Markdown destinations and docs file-map coverage (offline)."""
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
errors = []
checked = 0
mapped = set()
for source in [*DOCS.rglob('*.md'), ROOT / 'README.md', ROOT / 'README.ja.md']:
    # Repository links use inline Markdown; code fences may contain command examples.
    text = re.sub(r'```.*?```', '', source.read_text(), flags=re.S)
    for match in re.finditer(r'\]\(([^\n]+?)\)', text):
        url = match[1].strip().strip('<>')
        if urlsplit(url).scheme or url.startswith('#'):
            continue
        path = unquote(url.split('#', 1)[0])
        if not path:
            continue
        target = (source.parent / path).resolve()
        checked += 1
        if not target.exists():
            errors.append(f'{source.relative_to(ROOT)}: missing {url}')
        if source == DOCS / 'DOCUMENTATION.md':
            mapped.add(target)
for target in DOCS.rglob('*'):
    if target.is_file() and target.name not in {'DOCUMENTATION.md', 'DOCUMENTATION.ja.md'}:
        if target.resolve() not in mapped:
            errors.append(f'Unmapped document: {target.relative_to(ROOT)}')
if errors:
    raise SystemExit('\n'.join(errors))
print(f'PASS {checked} local links; every docs file has a documented role')
