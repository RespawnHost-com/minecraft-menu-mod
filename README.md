# RespawnHost Integration Menu

Order and configure [RespawnHost](https://respawnhost.com) servers directly from Minecraft's multiplayer menu — available for every Minecraft version from 1.12.2 to 26.1, across Forge, NeoForge and Fabric.

## Features

- **Order button in the multiplayer screen** — opens the order menu without leaving the game
- **Live plan list** — fetched from the RespawnHost API (`/api/games/short/minecraft/packages`), with an offline fallback list when the API is unreachable
- **Modpack-aware recommendations** — detects your modpack, reads its recommended RAM from the API and highlights the best-fitting plan
- **Full order configuration in-game** — billing model (fixed term / hourly), term length (30/90/180/360 days with volume discounts) and region (EU/US)
- **Deep-link checkout** — completes the order in your browser on `panel.respawnhost.com` with plan, model, term and region preselected (survives the login redirect)
- **In-game config screen** — Partner ID, Modpack ID, toggle for the order button
- **Localization** — English and German out of the box (English is the automatic fallback for all other languages)

## Supported versions

Every Minecraft release from 1.12 up to 26.1 is supported. One jar covers its whole segment — mappings are version-stable within a segment, so e.g. the 1.16.5 jar runs on every 1.16.x.

| Minecraft | Forge | NeoForge | Fabric |
|---|---|---|---|
| 1.12 – 1.12.2 | ✔ | — | — |
| 1.13 – 1.13.2 | ✔ | — | — |
| 1.14 – 1.14.4 | ✔ | — | ✔ |
| 1.15 – 1.15.2 | ✔ | — | ✔ |
| 1.16 – 1.16.5 | ✔ | — | ✔ |
| 1.17 – 1.17.1 | ✔ | — | ✔ |
| 1.18 – 1.18.2 | ✔ | — | ✔ |
| 1.19 – 1.19.2 | ✔ | — | ✔ |
| 1.19.3 – 1.19.4 | ✔ | — | ✔ |
| 1.20 – 1.20.1 | ✔ | — | ✔ |
| 1.20.2 – 1.20.4 | — | ✔ | ✔ |
| 1.20.5 – 1.20.6 | — | ✔ | ✔ |
| 1.21 – 1.21.1 | — | ✔ | ✔ |
| 1.21.2 – 1.21.5 | — | ✔ | ✔ |
| 1.21.6 – 1.21.10 | — | ✔ | ✔ |
| 1.21.11 | — | ✔ | ✔ |
| 26.1 | — | ✔ | ✔ |

## Installation

Drop the jar matching your Minecraft version and loader into your `mods` folder. The config file is created at `config/respawnhost_integration.json` on first launch:

```json
{
  "partner_id": "",
  "pack_id": "",
  "api_base_url": "https://respawnhost.com/api",
  "panel_base_url": "https://panel.respawnhost.com",
  "game_short": "minecraft",
  "region": "eu",
  "show_order_button": true
}
```

## Repository layout

```
core/               Shared pure-Java library (API client, config, models, plan recommender)
versions/<mc>-<loader>/   One standalone Gradle build per Minecraft version + loader
versions/1.21.1/    Multi-module build (common/fabric/neoforge, Architectury)
deploy/upload.sh    CurseForge + Modrinth upload script used by the release pipeline
build-all.ps1       Builds core and every variant with the correct JDK
```

## Building

Requirements: JDK 8, 17, 21 and 26 (paths are configured in `build-all.ps1`).

Build everything:

```powershell
.\build-all.ps1
```

Build a single variant:

```powershell
cd versions\1.20.1-fabric
.\gradlew.bat build
```

Jars land in each variant's `build/libs/`.

## Releasing

Releases are driven by git tags. Pushing `v1.2.3` builds all variants as version `1.2.3` and uploads every jar to CurseForge and Modrinth (tags containing `beta`/`rc` are published as beta). Every variant build honors `-Pmod_version=<x>` or the `MOD_VERSION` environment variable.

Required repository secrets (Settings → Secrets and variables → Actions):

| Secret | Purpose |
|---|---|
| `MODRINTH_TOKEN` | Modrinth PAT with `CREATE_VERSION` scope |
| `MODRINTH_PROJECT_ID` | Modrinth project ID |
| `CURSEFORGE_API_TOKEN` | CurseForge API token |
| `CURSEFORGE_PROJECT_ID` | CurseForge project ID |

Without secrets the deploy job skips gracefully, so tags are safe to push before they are configured.

## License

[MIT](LICENSE)
