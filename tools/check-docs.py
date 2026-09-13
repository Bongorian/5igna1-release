#!/usr/bin/env python3
"""Check local Markdown links and reachability from the guide indexes (offline)."""
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
errors = []
checked = 0
graph = {}
for source in [*DOCS.rglob('*.md'), ROOT / 'README.md', ROOT / 'README.ja.md']:
    text = re.sub(r'```.*?```', '', source.read_text(), flags=re.S)
    targets = set()
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
        targets.add(target)
    graph[source.resolve()] = targets

# Require discoverability, without a second hand-maintained file-by-file index.
pending = [DOCS / 'README.md', DOCS / 'README.ja.md']
reachable = set()
while pending:
    source = pending.pop().resolve()
    if source in reachable:
        continue
    reachable.add(source)
    pending.extend(graph.get(source, ()))
for target in DOCS.rglob('*'):
    if target.is_file() and target.resolve() not in reachable:
        errors.append(f'Unreachable document: {target.relative_to(ROOT)}')
if errors:
    raise SystemExit('\n'.join(errors))
print(f'PASS {checked} local links; every docs file is reachable from the guide indexes')
