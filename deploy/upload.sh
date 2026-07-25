#!/usr/bin/env bash
# Upload built jars to Modrinth and CurseForge. Usage: upload.sh [artifacts-dir]
set -u

ARTIFACTS_DIR="${1:-artifacts}"
DRY_RUN="${DRY_RUN:-0}"
MOD_VERSION="${MOD_VERSION:-}"
CHANGELOG="${CHANGELOG:-Release $MOD_VERSION}"

if [ -z "$MOD_VERSION" ]; then
  echo "ERROR: MOD_VERSION is not set"
  exit 1
fi

case "$MOD_VERSION" in
  *beta*|*Beta*|*BETA*|*rc*|*Rc*|*RC*) VERSION_TYPE="beta" ;;
  *) VERSION_TYPE="release" ;;
esac

failures=0

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g' | sed ':a;N;$!ba;s/\n/\\n/g'
}

loader_display() {
  case "$1" in
    forge) printf 'Forge' ;;
    fabric) printf 'Fabric' ;;
    neoforge) printf 'NeoForge' ;;
  esac
}

# --- jar discovery ---
mapfile -t jars < <(find "$ARTIFACTS_DIR" -path '*/build/libs/*.jar' \
  ! -name '*-sources.jar' ! -name '*-dev-shadow.jar' ! -name 'respawnhost-core-*.jar' | sort)

if [ "${#jars[@]}" -eq 0 ]; then
  echo "ERROR: no jars found under $ARTIFACTS_DIR"
  exit 1
fi

echo "Found ${#jars[@]} jar(s) to deploy (MOD_VERSION=$MOD_VERSION, type=$VERSION_TYPE)"

# --- platform availability ---
HAVE_MODRINTH=1
if [ -z "${MODRINTH_TOKEN:-}" ] || [ -z "${MODRINTH_PROJECT_ID:-}" ]; then
  echo "NOTICE: MODRINTH_TOKEN or MODRINTH_PROJECT_ID not set - skipping Modrinth"
  HAVE_MODRINTH=0
fi

HAVE_CURSEFORGE=1
if [ -z "${CURSEFORGE_API_TOKEN:-}" ] || [ -z "${CURSEFORGE_PROJECT_ID:-}" ]; then
  echo "NOTICE: CURSEFORGE_API_TOKEN or CURSEFORGE_PROJECT_ID not set - skipping CurseForge"
  HAVE_CURSEFORGE=0
fi

# --- CurseForge game version resolution ---
CF_VERSIONS_JSON=""
if [ "$HAVE_CURSEFORGE" = 1 ]; then
  if [ "$DRY_RUN" = 1 ]; then
    echo "NOTICE: dry run - skipping CurseForge game version resolution"
    HAVE_CURSEFORGE=2
  elif ! CF_VERSIONS_JSON=$(curl -sfS -H "X-Api-Token: $CURSEFORGE_API_TOKEN" -H "Accept: application/json" \
      https://minecraft.curseforge.com/api/game/versions); then
    echo "ERROR: failed to resolve CurseForge game versions - skipping CurseForge"
    failures=$((failures + 1))
    HAVE_CURSEFORGE=0
  fi
fi

cf_version_id() {
  printf '%s' "$CF_VERSIONS_JSON" | sed 's/},{/}\n{/g' | grep -F "\"name\": \"$1\"" \
    | sed -n 's/.*"id": *\([0-9][0-9]*\).*/\1/p' | head -1
}

# --- Modrinth upload ---
upload_modrinth() {
  local jar="$1" mc="$2" loader="$3"
  local name="$MOD_VERSION for $mc ($(loader_display "$loader"))"
  local vnum="$MOD_VERSION-mc$mc-$loader"
  local data
  data=$(printf '{"name":"%s","version_number":"%s","game_versions":["%s"],"loaders":["%s"],"version_type":"%s","project_id":"%s","changelog":"%s","featured":false,"status":"listed"}' \
    "$(json_escape "$name")" "$(json_escape "$vnum")" "$mc" "$loader" "$VERSION_TYPE" \
    "$(json_escape "$MODRINTH_PROJECT_ID")" "$(json_escape "$CHANGELOG")")
  if [ "$DRY_RUN" = 1 ]; then
    echo "[dry-run][modrinth] curl -sS -X POST -H \"Authorization: \$MODRINTH_TOKEN\" \\"
    echo "  -F 'data=$data;type=application/json' \\"
    echo "  -F \"file=@$jar\" https://api.modrinth.com/v2/version"
    return 0
  fi
  local resp code
  resp=$(mktemp)
  code=$(curl -sS -o "$resp" -w '%{http_code}' -X POST \
    -H "Authorization: $MODRINTH_TOKEN" \
    -F "data=$data;type=application/json" \
    -F "file=@$jar" \
    https://api.modrinth.com/v2/version)
  if [ "$code" -ge 200 ] && [ "$code" -lt 300 ]; then
    echo "Modrinth OK: $(basename "$jar")"
  elif grep -qiE 'unknown[^"]*game.?version|game.?version[^"]*(not found|not supported|invalid)' "$resp"; then
    echo "WARN: Modrinth skipped $(basename "$jar"): unknown game version $mc"
  else
    echo "ERROR: Modrinth upload failed for $(basename "$jar") (HTTP $code): $(cat "$resp")"
    failures=$((failures + 1))
  fi
  rm -f "$resp"
}

# --- CurseForge upload ---
upload_curseforge() {
  local jar="$1" mc="$2" loader="$3"
  local name="$MOD_VERSION for $mc ($(loader_display "$loader"))"
  local wanted=("$mc" "$(loader_display "$loader")" "Client")
  local ids=() missing=() n id
  for n in "${wanted[@]}"; do
    id=$(cf_version_id "$n")
    if [ -n "$id" ]; then ids+=("$id"); else missing+=("$n"); fi
  done
  local id_list
  id_list=$(IFS=,; echo "${ids[*]}")
  local metadata
  metadata=$(printf '{"changelog":"%s","changelogType":"markdown","displayName":"%s","gameVersions":[%s],"releaseType":"%s"}' \
    "$(json_escape "$CHANGELOG")" "$(json_escape "$name")" "$id_list" "$VERSION_TYPE")
  if [ "$DRY_RUN" = 1 ]; then
    if [ "${#missing[@]}" -gt 0 ]; then
      echo "[dry-run][curseforge] NOTE: unresolved version names: ${missing[*]}"
    fi
    echo "[dry-run][curseforge] curl -sS -X POST -H \"X-Api-Token: \$CURSEFORGE_API_TOKEN\" \\"
    echo "  -F 'metadata=$metadata;type=application/json' \\"
    echo "  -F \"file=@$jar\" https://minecraft.curseforge.com/api/projects/\$CURSEFORGE_PROJECT_ID/upload-file"
    return 0
  fi
  if [ "${#missing[@]}" -gt 0 ]; then
    echo "WARN: CurseForge skipped $(basename "$jar"): unresolvable game version(s): ${missing[*]}"
    return 0
  fi
  local resp code
  resp=$(mktemp)
  code=$(curl -sS -o "$resp" -w '%{http_code}' -X POST \
    -H "X-Api-Token: $CURSEFORGE_API_TOKEN" \
    -F "metadata=$metadata;type=application/json" \
    -F "file=@$jar" \
    "https://minecraft.curseforge.com/api/projects/$CURSEFORGE_PROJECT_ID/upload-file")
  if [ "$code" -ge 200 ] && [ "$code" -lt 300 ]; then
    echo "CurseForge OK: $(basename "$jar")"
  elif grep -qiE 'unknown[^"]*game.?version|game.?version[^"]*(not found|not supported|invalid)' "$resp"; then
    echo "WARN: CurseForge skipped $(basename "$jar"): unknown game version $mc"
  else
    echo "ERROR: CurseForge upload failed for $(basename "$jar") (HTTP $code): $(cat "$resp")"
    failures=$((failures + 1))
  fi
  rm -f "$resp"
}

# --- main loop ---
for jar in "${jars[@]}"; do
  rel="${jar#"$ARTIFACTS_DIR"/}"
  IFS='/' read -ra parts <<< "$rel"
  variant="${parts[0]#respawnhost-integration-}"
  variant="${variant%-jars}"
  mc="" loader=""
  case "$variant" in
    *-forge) mc="${variant%-forge}"; loader="forge" ;;
    *-fabric) mc="${variant%-fabric}"; loader="fabric" ;;
    *-neoforge) mc="${variant%-neoforge}"; loader="neoforge" ;;
    *)
      mc="$variant"
      for p in "${parts[@]}"; do
        case "$p" in fabric|neoforge|forge) loader="$p"; break ;;
        esac
      done
      ;;
  esac
  if [ -z "$loader" ]; then
    echo "WARN: cannot determine loader for $jar - skipping"
    continue
  fi
  echo "== $(basename "$jar") -> mc=$mc loader=$loader"
  [ "$HAVE_MODRINTH" = 1 ] && upload_modrinth "$jar" "$mc" "$loader"
  [ "$HAVE_CURSEFORGE" -ge 1 ] && upload_curseforge "$jar" "$mc" "$loader"
done

if [ "$failures" -gt 0 ]; then
  echo "ERROR: $failures upload(s) failed"
  exit 1
fi
echo "Deploy finished successfully"
