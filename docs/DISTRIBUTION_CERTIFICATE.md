# Distribution signing certificate

The GitHub APK uses a dedicated long-term distribution key, separate from the Play upload key. Moving to **Bongorian/5igna1-release** does not change the app ID or signing identity.

- Algorithm: RSA 4096 / SHA256withRSA
- Created: 2026-09-08
- Certificate SHA-256: `2aa9671259a1d0642e237ed69a0f711ffdf6769be7b5350598bc8428eaf18461`

The key and recovery settings are not tracked. A local backup under Documents was verified against the originals by SHA-256. Only this certificate fingerprint is public; it is different from a release APK's file hash.

To allow updates between GitHub and Play, the same app signing key must be selected during Play App Signing enrollment. The upload key has a different role. Ordinary F-Droid signing does not share this identity automatically.

[Installing and updating](INSTALLATION.md) · [Release procedure](RELEASING.md)
