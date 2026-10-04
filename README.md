# RespawnHost Integration Menu

Order and configure [RespawnHost](https://respawnhost.com) servers directly from Minecraft's multiplayer menu — with version-specific builds for Forge, NeoForge and Fabric, including Minecraft 26.3.

## Features

- **Order button in the multiplayer screen** — opens the order menu without leaving the game
- **Live plan list** — fetched from the RespawnHost API (`/api/games/short/minecraft/packages`), with an offline fallback list when the API is unreachable
- **Modpack-aware recommendations** — detects your modpack, reads its recommended RAM from the API and highlights the best-fitting plan
- **Full order configuration in-game** — billing model (fixed term / hourly), term length (30/90/180/360 days with volume discounts) and region (EU/US)
- **Deep-link checkout** — completes the order in your browser on `panel.respawnhost.com` with plan, model, term and region preselected (survives the login redirect)
- **In-game config screen** — Partner ID, Modpack ID, toggle for the order button
- **Localization** — English and German out of the box (English is the automatic fallback for all other languages)

## Supported versions

Build targets and JDK requirements are defined once in [`deploy/variants.json`](deploy/variants.json).
Both CI and releases consume this file. Choose the exact Minecraft version and loader shown in the
artifact name. Compatibility with other patch releases is not implied by a successful build.

| Minecraft build targets | Loaders |
|---|---|
| 1.12.2, 1.13.2 | Forge |
| 1.14.4, 1.15.2, 1.16.5, 1.17.1, 1.18.2, 1.19.2, 1.19.4, 1.20.1 | Forge, Fabric |
| 1.20.4 | NeoForge |
| 1.20.6, 1.21.1, 1.21.5, 1.21.10, 1.21.11 | NeoForge, Fabric |
| 26.1.2, 26.2, 26.3 | NeoForge, Fabric |

Minecraft 26.x uses Java 25. The 26.3 NeoForge target currently uses upstream
`26.3.0.48-beta`; this is a beta loader. Fabric requires Fabric API. The 1.21.1
builds additionally require Architectury API. These dependencies are included in publication metadata.

## Installation

Drop the jar matching your Minecraft version and loader into your `mods` folder. The config file is created at `config/respawnhost_integration.json` on first launch:

```json
{
  "creator_code": "",
  "pack_id": "",
  "api_base_url": "https://respawnhost.com/api",
  "panel_base_url": "https://panel.respawnhost.com",
  "game_short": "minecraft",
  "region": "eu",
  "show_order_button": true
}
```

`creator_code` is your RespawnHost affiliate/creator code. When set, every click on "Order Now" is reported to the affiliate system (`POST /api/affiliate/track/{code}`) before the checkout opens. Old configs with `partner_id` are migrated automatically.

## Repository layout

```
core/               Shared pure-Java library (API client, config, models, plan recommender)
versions/<mc>-<loader>/   One standalone Gradle build per Minecraft version + loader
versions/1.21.1/    Multi-module build (common/fabric/neoforge, Architectury)
deploy/             Build matrix, artifact validation, publication client and tests
build-all.ps1       Builds core and every variant with the correct JDK
```

## Building

Requirements: Python 3.11+ and the JDK matching each entry in `deploy/variants.json`
(JDK 8/17/21 for legacy builds, JDK 25 for Minecraft 26.x). No machine-specific JDK paths are committed.
Set `JAVA_HOME_8`, `JAVA_HOME_17`, `JAVA_HOME_21` and `JAVA_HOME_25`, or pass `-JavaHomes`.
`JAVA_HOME` can be used if it matches the selected target.

```powershell
# Build selected current variants; their shared core is built automatically.
.\build-all.ps1 -Variants 26.3-fabric,26.3-neoforge -ModVersion 1.1.0
# Build every target:
.\build-all.ps1 -ModVersion 1.1.0
# Release regression tests:
python -m unittest discover -s deploy -p 'test_*.py'
```

Production jars and checksum manifests are collected in `dist/`. Collection rejects missing,
ambiguous or incorrectly versioned jars. Sources, development jars and common-module jars
are not published.

## Releasing

Branch pushes and pull requests only build and test. They do **not** publish.
Pushing a tag such as `v1.1.0` runs the reusable build workflow for **all** matrix entries,
validates the complete artifact set, then publishes to CurseForge and Modrinth.
Tags containing `alpha`, `beta` or `rc` select the corresponding release type.

The **Release** workflow can also be started manually with a version. Its default
`publish=false` builds and validates without uploading. Set `publish=true` only for an
intended release. A manually dispatched release builds the selected workflow ref.

Required GitHub Actions repository secrets:

| Secret | Purpose |
|---|---|
| `MODRINTH_TOKEN` | Modrinth PAT with `VERSION_CREATE` scope |
| `MODRINTH_PROJECT_ID` | Modrinth project ID |
| `CURSEFORGE_API_TOKEN` | CurseForge **Upload API** token |
| `CURSEFORGE_PROJECT_ID` | Numeric CurseForge project ID |

Missing secrets now fail publication before any upload. Unknown versions, rejected uploads,
missing artifacts and missing response IDs also fail; a green validation run is not a publication.
A successful upload logs the platform's file/version ID. CurseForge moderation may still be pending.

```powershell
$env:MOD_VERSION = '1.1.0'
python deploy/upload.py dist --dry-run
# For an explicitly intended upload to one platform:
python deploy/upload.py dist --platform curseforge
```

Dry runs require a complete collected release and never contact the publishing APIs.
Uploads are intentionally not retried automatically: after a partial release, inspect the accepted
file IDs before retrying to avoid duplicates.

## Deployment diagnosis (2026-10-04)

The repository had no tags, no GitHub releases and no Release workflow runs; only Build runs.
All four secret **names** existed, but GitHub does not expose their values for validation.
Consequently there was no evidence of an attempted CurseForge upload or a token rejection.

Additional problems corrected:

- Release had fewer targets than Build and selected the wrong JDK for Forge 1.16.5.
- Upload JSON was assembled/parsed with shell text operations, dependent on whitespace.
- Missing secrets and unresolved game versions could produce a successful-looking skipped deploy.
- Minecraft 26.1.2 binaries were labeled 26.1 using directory names.
- Modrinth payloads lacked `file_parts`; required Fabric/Architectury dependencies were absent.
- Artifact collection included development/common jars; it now explicitly selects each loader output.
- Several legacy Forge descriptors stayed at version 1.0.0 even for a differently named release.

The publisher uses CurseForge's documented `gameVersionNames` field, JSON serialization,
checksum manifests and explicit failures. See the [CurseForge Upload API](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-api),
the [Modrinth version API](https://docs.modrinth.com/api/operations/createversion/)
and [Fabric 26.3 porting notes](https://www.fabricmc.net/2026/09/15/263.html).

## Verification (2026-10-04)

- Shared core build passed on JDK 21 (Java 8 bytecode retained).
- All six 26.1.2/26.2/26.3 Fabric and NeoForge builds passed on JDK 25.
- Modified Forge builds for 1.12.2, 1.13.2, 1.14.4, 1.15.2, 1.16.5 and 1.18.2 passed on their matching JDKs.
- All 12 modified/new variants were collected as version `1.1.0`, with embedded-version and SHA-256 checks.
- 14 release regression tests and actionlint passed.
- Production artifact selection was checked against the real artifacts for all 30 existing variants
  from successful GitHub Build run `30177704869`. This is not a fresh rebuild of the unchanged variants.

The local `dist/` directory contains those 12 rebuilt jars, not a complete 35-jar release.
The release workflow builds the complete set before publishing. No in-game visual/runtime test,
live publication, commit, push or release tag was performed during this verification.
GitHub secret values and CurseForge moderation status remain unverified.

## License

[MIT](LICENSE)
