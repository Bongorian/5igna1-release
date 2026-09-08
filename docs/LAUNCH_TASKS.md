# Launch checklist

The public project starts here: **Bongorian/5igna1-release**. As of 2026-09-08, the owner reports F-Droid submission awaiting merge and Google Play closed testing awaiting review. Both submitted candidates remain 1.0.0 / code 8; current source prepares 1.1.0 / code 9.

## GitHub / Obtainium

- [x] Create a clean source snapshot without the old development Git history or Toren1BD screenshots.
- [x] Keep the previous repository private after the owner changed the publication plan.
- [x] Back up the Play upload key, certificate, and recovery settings under Documents; verify file hashes.
- [x] Create a separate long-term distribution key and back it up with its certificate and recovery settings.
- [x] Sign and verify the 1.0.0 FOSS APK and SHA-256.
- [x] Rewrite the public presentation in English, including the one-time encounter, fault models, and distinction from a preset-filter workflow.
- [x] Add first-use, installation, recipe, format, controls, and troubleshooting guides, with a Japanese edition.
- [x] Publish this clean repository and its initial v1.0.0 release.
- [x] Verify anonymous APK/checksum/source downloads from the new URL.
- [ ] Confirm Obtainium installation on a device using the new source URL.

## Ongoing releases and keys

- [ ] Copy the verified local backup to a separate offline medium. The Documents copy is on the same computer.
- [ ] If using GitHub for signing, configure the release environment's four Secrets and certificate fingerprint variable.
- [ ] Test an in-place update with a higher versionCode and the same certificate for the next release.

The first release can be signed locally; GitHub Secrets are not needed for that path. Never attach key or recovery files to a release or issue. [Public certificate fingerprint](DISTRIBUTION_CERTIFICATE.md)

## F-Droid

- [x] Prepare FOSS flavors, reviewed dependencies, metadata, and Japanese/English store text.
- [x] Set the source URL to this clean repository and the candidate tag to v1.0.0.
- [ ] Run fdroid build --test in the F-Droid Linux environment.
- [x] Submit to fdroiddata (owner report).
- [ ] Complete review and merge.
- [ ] Add the actual listing URL after acceptance.

## Google Play — closed-test review pending

- [ ] Confirm any remaining account verification requirements in Console.
- [ ] Decide Play App Signing at initial enrollment. To allow updates between GitHub and Play, select the distribution key as the app signing key; the upload key remains separate.
- [ ] Confirm a public privacy-policy URL, declarations, screenshots, and required testing.
- [x] Submit the closed-test candidate (owner report).
- [ ] Complete closed-test review and required testing.
- [ ] Add the actual Play listing URL.

Billing products and Sponsors setup are optional future work, not prerequisites for this release.

## Canonical public URLs

- Repository / Obtainium source: `https://github.com/Bongorian/5igna1-release`
- Latest release: `https://github.com/Bongorian/5igna1-release/releases/latest`
- Initial release: `https://github.com/Bongorian/5igna1-release/releases/tag/v1.0.0`

## Publication verification — 2026-09-08

The initial public history has one root commit and no imported parent commits. APK and checksum downloads were fetched without authentication and matched the local files. The tagged app source was readable publicly; the previous private repository returned 404 without authentication. [Initial Ubuntu/macOS CI](https://github.com/Bongorian/5igna1-release/actions/runs/34147880216) passed both jobs. Obtainium’s on-device import/install remains a follow-up check.
