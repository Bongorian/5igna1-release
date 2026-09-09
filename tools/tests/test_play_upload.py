import copy
import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('play_upload', Path(__file__).parents[1] / 'play-upload.py')
uploader = importlib.util.module_from_spec(spec)
spec.loader.exec_module(uploader)


class FakePlay:
    def __init__(self, track=None, bundles=None, commit_error=False, wrong_upload=False):
        self.track = track or {'track': 'alpha', 'releases': [{'versionCodes': ['11'], 'status': 'completed'}]}
        self.bundles = bundles or []
        self.commit_error = commit_error
        self.wrong_upload = wrong_upload
        self.calls = []

    def request(self, method, path, body=None, media=None):
        self.calls.append((method, path, copy.deepcopy(body)))
        if method == 'POST' and path == '/edits':
            return {'id': '42'}
        if method == 'GET' and path.endswith('/tracks/alpha'):
            return copy.deepcopy(self.track)
        if method == 'GET' and path.endswith('/bundles'):
            return {'bundles': copy.deepcopy(self.bundles)}
        if media is not None:
            return {'versionCode': 12, 'sha256': 'wrong' if self.wrong_upload else hashlib.sha256(media).hexdigest()}
        if ':commit?' in path and self.commit_error:
            raise RuntimeError('CHANGES_ALREADY_IN_REVIEW')
        return {}


class PlayUploadTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.aab = Path(self.folder.name) / 'fixture.aab'
        self.aab.write_bytes(b'fixture signed bundle')
        self.digest = hashlib.sha256(self.aab.read_bytes()).hexdigest()
        self.notes = [{'language': 'en-US', 'text': 'Example'}]

    def run_upload(self, api):
        uploader.upload(api, self.aab, '1.3.1', 12, self.notes)

    def test_preserves_existing_releases_and_rollout_settings(self):
        old = {'track': 'alpha', 'releases': [
            {'name': 'active', 'versionCodes': ['11'], 'status': 'inProgress', 'userFraction': .3, 'inAppUpdatePriority': 2}]}
        before = copy.deepcopy(old)
        result = uploader.draft_track(old, '1.3.1', 12, self.notes)
        self.assertEqual(old, before)
        self.assertEqual(result['releases'][0], before['releases'][0])
        self.assertEqual(result['releases'][1]['status'], 'draft')
        self.assertEqual(result['releases'][1]['versionCodes'], ['12'])

    def test_refuses_to_replace_an_existing_draft(self):
        with self.assertRaisesRegex(ValueError, 'another draft'):
            uploader.draft_track({'track': 'alpha', 'releases': [{'status': 'draft', 'versionCodes': ['11']}]}, '1.3.1', 12, self.notes)

    def test_refuses_newer_version_or_other_track(self):
        for track in [ {'track': 'production'}, {'track': 'alpha', 'releases': [{'versionCodes': ['13'], 'status': 'completed'}]} ]:
            with self.assertRaises(ValueError):
                uploader.draft_track(track, '1.3.1', 12, self.notes)

    def test_success_creates_draft_and_never_uses_default_review_behavior(self):
        api = FakePlay()
        self.run_upload(api)
        puts = [c for c in api.calls if c[0] == 'PUT']
        self.assertEqual(len(puts), 1)
        self.assertEqual(puts[0][2]['releases'][-1]['status'], 'draft')
        commits = [c[1] for c in api.calls if ':commit?' in c[1]]
        self.assertEqual(commits, ['/edits/42:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW'])
        self.assertFalse(any(c[0] == 'DELETE' for c in api.calls))

    def test_existing_review_error_cleans_up_without_retry_or_cancellation(self):
        api = FakePlay(commit_error=True)
        with self.assertRaisesRegex(RuntimeError, 'CHANGES_ALREADY_IN_REVIEW'):
            self.run_upload(api)
        self.assertEqual(sum(':commit?' in c[1] for c in api.calls), 1)
        self.assertEqual(api.calls[-1][:2], ('DELETE', '/edits/42'))

    def test_already_assigned_identical_bundle_is_no_op(self):
        api = FakePlay(track={'track': 'alpha', 'releases': [{'versionCodes': ['12'], 'status': 'completed'}]},
                       bundles=[{'versionCode': 12, 'sha256': self.digest}])
        self.run_upload(api)
        self.assertFalse(any(c[0] == 'PUT' or ':commit' in c[1] or 'uploadType' in c[1] for c in api.calls))

    def test_reuses_uploaded_bundle_after_interrupted_assignment(self):
        api = FakePlay(bundles=[{'versionCode': 12, 'sha256': self.digest}])
        self.run_upload(api)
        self.assertFalse(any('uploadType' in c[1] for c in api.calls))
        self.assertTrue(any(c[0] == 'PUT' for c in api.calls))

    def test_version_code_collision_is_not_overwritten(self):
        api = FakePlay(bundles=[{'versionCode': 12, 'sha256': 'another-bundle'}])
        with self.assertRaisesRegex(ValueError, 'different AAB'):
            self.run_upload(api)
        self.assertFalse(any(c[0] == 'PUT' or 'uploadType' in c[1] for c in api.calls))
        self.assertEqual(api.calls[-1][:2], ('DELETE', '/edits/42'))

    def test_upload_mismatch_never_updates_track(self):
        api = FakePlay(wrong_upload=True)
        with self.assertRaisesRegex(ValueError, 'mismatch'):
            self.run_upload(api)
        self.assertFalse(any(c[0] == 'PUT' for c in api.calls))


if __name__ == '__main__':
    unittest.main()
