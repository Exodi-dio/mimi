# Signing keys

`mimi-beta.keystore` is MIMI's beta signing key.

**It is committed to this repository on purpose, and it is not a secret.** Its
credentials are intentionally public:

| Field | Value |
| --- | --- |
| alias | `mimi-beta` |
| store password | `mimibeta` |
| key password | `mimibeta` |

## Why a public key instead of a hidden one

Android refuses to install an APK over an existing one unless both are signed by
the **same certificate**. If each cloud build minted its own throwaway key, every
release would look like a different app and she would have to uninstall (losing
her library cache and settings) before installing the next one.

Because this is a personal beta for a single user, the trade-off is worth it: one
stable, shared key means a new APK installs straight over the old one. The key
was generated on a GitHub Actions runner by `gen-beta-keystore.yml`, and the
private half has never existed anywhere but in this repository.

The release workflow verifies that the built APK's signing certificate actually
matches this keystore, and fails the release if it does not.

## If MIMI ever ships publicly

Generate a properly secured release key, keep it out of the repository, and pass
it to Gradle through environment variables. The build already prefers nothing —
it just uses this file when it is present — so a real key can be wired in later
without disturbing the beta flow.
