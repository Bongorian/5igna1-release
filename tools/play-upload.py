#!/usr/bin/env python3
"""Upload a verified Play AAB as an Alpha draft; never promote or cancel review."""
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request

PACKAGE = 'com.bongorian.signa1'
API = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/' + PACKAGE
UPLOAD = 'https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/' + PACKAGE


def metadata(source):
    text = (source / 'app/build.gradle').read_text()
    version = re.search(r"^\s*versionName\s+'([^']+)'", text, re.M).group(1)
    code = int(re.search(r'^\s*versionCode\s+(\d+)', text, re.M).group(1))
    if not re.fullmatch(r'\d+\.\d+\.\d+', version) or code <= 0:
        raise ValueError('Invalid release version')
    notes = [{'language': locale, 'text': (source / 'fastlane/metadata/android' / locale /
              'changelogs' / f'{code}.txt').read_text().strip()} for locale in ('en-US', 'ja-JP')]
    if any(not n['text'] or len(n['text']) > 500 for n in notes):
        raise ValueError('Missing or oversized release notes')
    return version, code, notes


def validate_bundle(aab, bundletool, certificate, version, code):
    subprocess.run(['jarsigner', '-verify', str(aab)], check=True, capture_output=True)
    cert = subprocess.check_output(['keytool', '-J-Duser.language=en', '-printcert', '-jarfile', str(aab)], text=True)
    fingerprints = re.findall(r'SHA256:\s*([0-9A-Fa-f:]+)', cert)
    expected = certificate.replace(':', '').lower()
    if not re.fullmatch(r'[0-9a-f]{64}', expected) or not fingerprints or any(
            f.replace(':', '').lower() != expected for f in fingerprints):
        raise ValueError('AAB upload certificate does not match the existing Play upload key')
    subprocess.run(['java', '-jar', str(bundletool), 'validate', '--bundle=' + str(aab)], check=True, capture_output=True)
    import xml.etree.ElementTree as ET
    manifest = ET.fromstring(subprocess.check_output([
        'java', '-jar', str(bundletool), 'dump', 'manifest', '--bundle=' + str(aab), '--module=base']))
    ns = '{http://schemas.android.com/apk/res/android}'
    if manifest.get('package') != PACKAGE or manifest.get(ns + 'versionName') != version or manifest.get(ns + 'versionCode') != str(code):
        raise ValueError('AAB package or version does not match the release tag')
    app = manifest.find('application')
    if app is None or app.get(ns + 'debuggable', 'false') not in ('false', '0'):
        raise ValueError('Refusing a debuggable bundle')


def draft_track(track, version, code, notes):
    if track.get('track') != 'alpha':
        raise ValueError('Only the existing Alpha track is supported')
    releases = track.get('releases', [])
    if any(str(code) in r.get('versionCodes', []) for r in releases):
        return None  # Already assigned: preserve its current review/rollout state.
    if any(int(c) > code for r in releases for c in r.get('versionCodes', [])):
        raise ValueError('Alpha already has a newer version; refusing an older draft')
    if any(r.get('status') == 'draft' for r in releases):
        raise ValueError('Alpha already has another draft; finish it in Play Console before retrying')
    result = copy.deepcopy(track)
    result.setdefault('releases', []).append({
        'name': f'{code} ({version})', 'versionCodes': [str(code)],
        'status': 'draft', 'releaseNotes': notes,
    })
    return result


class Play:
    def __init__(self, token):
        self.token = token

    def request(self, method, path, body=None, media=None):
        url = (UPLOAD if media is not None else API) + path
        data = media if media is not None else (json.dumps(body).encode() if body is not None else None)
        headers = {'Authorization': 'Bearer ' + self.token}
        if data is not None:
            headers['Content-Type'] = 'application/octet-stream' if media is not None else 'application/json'
        try:
            with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=headers, method=method), timeout=180) as response:
                content = response.read()
                return json.loads(content) if content else {}
        except urllib.error.HTTPError as error:
            try:
                message = json.loads(error.read()).get('error', {}).get('message', 'Play API request rejected')
            except (ValueError, AttributeError):
                message = 'Play API request rejected'
            raise RuntimeError(f'Play API HTTP {error.code}: {message}') from None


def upload(play, aab, version, code, notes):
    payload = aab.read_bytes()
    digest = hashlib.sha256(payload).hexdigest()
    edit = play.request('POST', '/edits', {})['id']
    if not re.fullmatch(r'[A-Za-z0-9_-]+', edit):
        raise ValueError('Invalid edit ID')
    base = '/edits/' + edit
    committed = False
    try:
        track = play.request('GET', base + '/tracks/alpha')
        updated = draft_track(track, version, code, notes)
        bundles = play.request('GET', base + '/bundles').get('bundles', [])
        existing = next((b for b in bundles if int(b['versionCode']) == code), None)
        if existing and existing.get('sha256', '').lower() != digest:
            raise ValueError('This versionCode already belongs to a different AAB; do not overwrite it')
        if updated is None:
            if not existing:
                raise ValueError('Version is assigned but its AAB cannot be verified')
            print(f'Already present: {version} / {code}; no changes made')
            return
        if not existing:
            bundle = play.request('POST', base + '/bundles?uploadType=media', media=payload)
            if int(bundle['versionCode']) != code or bundle.get('sha256', '').lower() != digest:
                raise ValueError('Uploaded bundle identity/hash mismatch')
        play.request('PUT', base + '/tracks/alpha', updated)
        play.request('POST', base + ':validate')
        # Only the new draft differs from the fetched track. Draft status prevents rollout.
        # changesNotSentForReview is rejection-specific and is rejected by this app.
        # The default review behavior cancels an existing review. Never use that default.
        play.request('POST', base + ':commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW')
        committed = True
        print(f'Uploaded Alpha draft: {version} / {code}; SHA-256 {digest}')
    finally:
        if not committed:
            try:
                play.request('DELETE', base)
            except Exception:
                print('Temporary Play edit cleanup failed; it expires automatically', file=__import__('sys').stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--tag', required=True)
    parser.add_argument('--aab', type=Path, required=True)
    parser.add_argument('--bundletool', type=Path, required=True)
    parser.add_argument('--certificate-sha256', required=True)
    parser.add_argument('--verify-only', action='store_true')
    args = parser.parse_args()
    version, code, notes = metadata(args.source)
    if args.tag != 'v' + version:
        raise ValueError('Tag differs from source version')
    validate_bundle(args.aab, args.bundletool, args.certificate_sha256, version, code)
    if args.verify_only:
        print(f'PASS signed Play AAB: {version} / {code}; package, manifest and certificate verified')
        return
    token = os.environ.get('PLAY_ACCESS_TOKEN')
    if not token:
        raise ValueError('Missing short-lived Play access token')
    upload(Play(token), args.aab, version, code, notes)


if __name__ == '__main__':
    main()
