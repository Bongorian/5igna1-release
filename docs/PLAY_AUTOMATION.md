# Automatic Play uploads

[日本語](PLAY_AUTOMATION.ja.md) · [Releasing](RELEASING.md)

Publishing a stable `vX.Y.Z` GitHub release triggers **Upload Play AAB**. It resolves the published tag to its commit, checks metadata/tests/dependencies, builds a signed `playRelease` AAB, verifies its manifest and existing upload certificate, and saves it as an **Alpha draft** with English/Japanese release notes. Review submission and publication remain in Play Console.

## Cost and credentials

The repository is public and the job uses a standard Ubuntu runner, which is [free for public repositories](https://docs.github.com/en/billing/concepts/product-billing/github-actions). This workflow does not store Actions artifacts or caches, run a server, or enable paid Cloud resources. Existing Play developer registration is separate. Reassess billing before changing repository visibility or runner type.

Google authentication uses [Workload Identity Federation](https://github.com/google-github-actions/auth), with a short-lived Android Publisher access token. No service-account private-key JSON is created. The existing Play upload keystore and passwords are encrypted GitHub environment secrets and restored only during the build. This does not rotate any signing key.

## Configuration

Repository: `Bongorian/5igna1-release`. Environment: `google-play`, allowing `main` and `v*` tags. Repository variable `PLAY_UPLOAD_ENABLED=true` enables the job; set it to `false` to stop future uploads.

| Environment setting | Type |
|---|---|
| PLAY_UPLOAD_KEYSTORE_BASE64 | Secret: existing Play upload keystore encoded in Base64 |
| PLAY_UPLOAD_STORE_PASSWORD | Secret |
| PLAY_UPLOAD_KEY_ALIAS | Secret |
| PLAY_UPLOAD_KEY_PASSWORD | Secret |
| PLAY_WORKLOAD_IDENTITY_PROVIDER | Variable: configured Google identity provider resource |
| PLAY_SERVICE_ACCOUNT | Variable: dedicated Play uploader account |

The identity provider limits access to this repository and owner by numeric IDs, the `play-upload.yml` workflow, and `main` or version tags. The service account receives only 5igna1 app-view, quality-view and testing-track release permissions in Play Console. It has no production release or account administration permission. Android Publisher, IAM, IAM Credentials and Security Token Service APIs are enabled. See [Google API setup](https://developers.google.com/android-publisher/getting_started).

Actions are pinned to reviewed commits. The automation-only bundle validator is Google bundletool 1.18.3 (Apache-2.0), fetched from its official release and checked against SHA-256 `a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29`. This adds no application dependency.

## Running and retrying

1. Release a new, checked version with an increased versionCode. Publish its GitHub release. Drafts and prereleases do not upload.
2. Open GitHub → Actions → **Upload Play AAB** and inspect the run. A successful run reports the Alpha draft and AAB checksum.
3. Open Play Console → Closed testing → Alpha to review the draft, then submit and publish when appropriate.

For an existing stable release, use **Run workflow**, select `main`, and enter its tag. Select `verify_only` to test the signed build without contacting Play. This also supports tags published before the upload workflow existed. The normal release-event path requires the workflow in the released source; use main as the basis of future releases.

Uploads run serially. Existing track releases and their settings are retained. Another Alpha draft, a newer version, a conflicting versionCode/hash, or API failure stops the job. An identical AAB already assigned to Alpha is a no-op. A rebuild is not guaranteed byte-identical: if a versionCode belongs to a different hash, do not replace it; inspect the existing bundle and use a new release version for changed builds.

The new release is explicitly `draft`; existing releases are unchanged. The commit uses `changesInReviewBehavior=ERROR_IF_IN_REVIEW`. The rejection-specific `changesNotSentForReview` parameter is omitted because this app rejects it; draft status controls the new release. [Google's commit API](https://developers.google.com/android-publisher/api-ref/rest/v3/edits/commit) can otherwise cancel a pending review by default. A review conflict therefore fails safely; finish the existing review/publication before rerunning. Never remove this guard to force a retry. Failed, uncommitted edits are deleted when possible and otherwise expire automatically.

A Play draft is [not served to users](https://developers.google.com/android-publisher/api-ref/rest/v3/edits.tracks). This automation does not modify tester lists, countries, store listings, the F-Droid submission, GitHub APKs, or existing tags.

## Verification

Upload logic tests cover preserving rollout settings, draft/version collisions, duplicate uploads, upload hash mismatches and review conflicts. They run in regular Ubuntu/macOS CI and the upload job. Real AAB validation checks the package, version, non-debuggable manifest and existing Play certificate before Google authentication.

## First successful run — 2026-09-09

[GitHub run 34316132266](https://github.com/Bongorian/5igna1-release/actions/runs/34316132266) built the immutable `v1.3.1` source, authenticated through federation, and saved **12 (1.3.1)** as an unpublished Alpha draft. Play Console separately confirmed **11 (1.3.0)** remained published to testers. AAB SHA-256: `26e91fb345cb49d6b08ba06893d68a80fae92ec305accb81fb187fd5919637d3`. Ubuntu and macOS CI also passed for the final upload logic.

## 1.4.0 upload — 2026-09-09

[Run 34327243407](https://github.com/Bongorian/5igna1-release/actions/runs/34327243407) successfully uploaded 1.4.0 / code 13 as an Alpha draft after the GitHub release was published. AAB SHA-256: `e30842447f1d95f375a61ad5a088dec35bdd69381b426701a4c5241b11d1f64e`. Existing-draft and review guards remained enabled; no conflicting release or review was removed. This confirms draft upload, not review submission or tester delivery.

## 1.5.0

1.5.0 / code 14 uploaded successfully as an Alpha draft on 2026-09-09. This does not submit it for review or publish it to testers. [Run](https://github.com/Bongorian/5igna1-release/actions/runs/34352867100).
