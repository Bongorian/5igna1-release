# 5igna1 website

Japanese public-release product page hosted by GitHub Pages. Static HTML/CSS, without a build dependency or JavaScript. Keep `.nojekyll`.

- Landing: https://bongorian.github.io/5igna1-release/
- Japanese privacy policy: https://bongorian.github.io/5igna1-release/privacy/
- English policy: https://bongorian.github.io/5igna1-release/privacy/en.html
- Chinese policy: https://bongorian.github.io/5igna1-release/privacy/zh.html
- Previous `/en.html` and `/zh.html` policy URLs still show the complete policy. Root now serves the landing page and prominently links to the dedicated policy.

GitHub Pages continues to deploy `codex/privacy-pages` at `/`. Publish tested website changes to that branch. Keep this separate from application main and release tags. Do not delete the Pages branch during application branch cleanup.

Serve this directory with any static HTTP server to preview. Check all relative links and images before publishing. [Asset provenance](ASSETS.md).

## Public release

Updated 2026-10-03 after verifying the unauthenticated Japanese Google Play listing, including its purchase button and 1.7.5 release notes. The primary action is https://play.google.com/store/apps/details?id=com.bongorian.signa1 . Distribution is Japan, Taiwan, United States and Canada; pricing and device eligibility are shown by Play.

The page describes released 1.7.5 features, not unreleased R8 optimization work. Historical 1.3.1 screenshots retain their version and provenance caption. The old recruitment form is no longer promoted; existing tester-form privacy disclosures remain applicable to previously collected responses.

## Play Console privacy URL update

Set the dedicated Japanese policy URL in Play Console → 5igna1 → Policy and programs → App content → Privacy policy → Manage. Save, then inspect Publishing overview and send the URL change for review if requested. Menu wording may vary; search for Privacy policy in App content. This website publication does not itself change the app's Play Console setting, submit the 1.3.1 Alpha draft for review, or publish it. Check the set of pending changes before submitting.

Recommended URL: `https://bongorian.github.io/5igna1-release/privacy/`.

The policy is public HTML without authentication. Its previous app-data content is retained; a separate website-hosting paragraph was added. The Android app displays a bundled offline policy, so moving the web URL does not require an app binary update. Other store metadata linking to the old root can use the same dedicated URL.

Official instructions: https://support.google.com/googleplay/android-developer/answer/9859455

## 1.5.0 content update

The recruitment landing page describes TAP, probability-based TIME ECHO, expanded MEDIA/DISPLAY, SEED transactions and background recording. It preserves the Google Play tester form as its primary action. The three existing developer-owned example captures remain explicitly identified as 1.3.1 imagery. Legacy root language policy URLs redirect to the current policy pages.

## Release synchronization

Every application release includes a GitHub Pages update. Compare the released source, changelog, user guide and bundled privacy policies with this site; update the displayed version, feature/operation text, versioned guide links and relevant privacy disclosures. Check screenshot captions and retain historical image labels until the images themselves are replaced. Preserve the tester form and distinguish GitHub release availability from Play review/publication status. Publish to `codex/privacy-pages`, wait for deployment, and verify the public landing and policy pages before reporting the release complete.

## 1.7.2 content update

Updated camera-side/lens selection, Settings-based PRO shooting mode, compact landscape controls and chain editor actions. Reviewed all three policies against unchanged bundled policies and permissions; this release adds no new collection or transmission. Existing screenshots retain their historical 1.3.1 labels.
