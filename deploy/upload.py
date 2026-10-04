"""Validated CurseForge/Modrinth publishing. Dry runs never contact APIs."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import urllib.error
import urllib.request
import uuid

from artifacts import variants

CF = 'https://minecraft.curseforge.com/api'
MR = 'https://api.modrinth.com/v2'
DISPLAY = {'fabric': 'Fabric', 'forge': 'Forge', 'neoforge': 'NeoForge'}


def release_type(version):
    return 'alpha' if 'alpha' in version.lower() else 'beta' if re.search(r'beta|rc', version, re.I) else 'release'


def discover(directory, version):
    expected = {(v['variant'], o['loader']): o for v in variants() for o in v['outputs']}
    found = {}
    for manifest in directory.rglob('*.json'):
        for item in json.loads(manifest.read_text()):
            key = item['variant'], item['loader']
            if key not in expected or key in found:
                raise ValueError(f'Unexpected or duplicate artifact: {key}')
            target = expected[key]
            if any(item[k] != target[k] for k in ('minecraft', 'architectury')) or item['version'] != version:
                raise ValueError(f'Incorrect artifact metadata: {key}')
            if Path(item['file']).name != item['file']:
                raise ValueError('Artifact filename must not contain a path')
            jar = manifest.parent / item['file']
            if hashlib.sha256(jar.read_bytes()).hexdigest() != item['sha256']:
                raise ValueError(f'Checksum mismatch: {jar}')
            found[key] = (item, jar)
    if found.keys() != expected.keys():
        raise ValueError(f'Missing release artifacts: {sorted(expected.keys() - found.keys())}')
    return list(found.values())


def metadata(item, version, changelog, project):
    loader, mc = item['loader'], item['minecraft']
    name = f'{version} for {mc} ({DISPLAY[loader]})'
    deps = (['fabric-api'] if loader == 'fabric' else []) + (['architectury-api'] if item['architectury'] else [])
    cf = dict(changelog=changelog, changelogType='markdown', displayName=name,
              gameVersionNames=[mc, DISPLAY[loader], 'Client'], releaseType=release_type(version))
    if deps:
        cf['relations'] = {'projects': [dict(slug=d, type='requiredDependency') for d in deps]}
    mr = dict(name=name, version_number=f'{version}-mc{mc}-{loader}', game_versions=[mc],
              loaders=[loader], version_type=release_type(version), project_id=project,
              changelog=changelog, featured=False, status='listed', environment='client_only', file_parts=['file'],
              dependencies=[dict(project_id={'fabric-api': 'P7dR8mSH', 'architectury-api': 'lhGA9TYQ'}[d],
                                 dependency_type='required') for d in deps])
    return cf, mr


def upload(url, header, token, field, data, jar):
    boundary = 'respawnhost-' + uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"\r\n'
            'Content-Type: application/json\r\n\r\n').encode() + json.dumps(data).encode() + b'\r\n'
    body += (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{jar.name}"\r\n'
             'Content-Type: application/java-archive\r\n\r\n').encode() + jar.read_bytes()
    body += f'\r\n--{boundary}--\r\n'.encode()
    request = urllib.request.Request(url, body, headers={header: token,
        'Content-Type': f'multipart/form-data; boundary={boundary}',
        'User-Agent': 'RespawnHost-com/minecraft-menu-mod (release pipeline)'})
    # Never retry POST blindly: a timeout may follow an accepted upload.
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            result = json.load(response)
    except urllib.error.HTTPError as error:
        raise RuntimeError(f'Upload failed: HTTP {error.code}') from None
    if not result.get('id'):
        raise ValueError('Upload response did not contain a file/version ID')
    return result['id']


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('directory', type=Path, nargs='?', default=Path('artifacts'))
    parser.add_argument('--dry-run', action='store_true')
    parser.add_argument('--platform', choices=['all', 'curseforge', 'modrinth'], default='all')
    args = parser.parse_args()
    version = os.environ.get('MOD_VERSION', '')
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?', version):
        raise ValueError('MOD_VERSION must be a release version such as 1.1.0 or 1.1.0-beta.1')
    records = discover(args.directory, version)
    changelog_file = os.environ.get('CHANGELOG_FILE')
    changelog = Path(changelog_file).read_text(encoding='utf-8') if changelog_file else os.environ.get('CHANGELOG', f'Release {version}')
    platforms = ['curseforge', 'modrinth'] if args.platform == 'all' else [args.platform]
    secrets = {}
    for platform in platforms:
        prefix = platform.upper()
        token = os.environ.get(prefix + ('_API_TOKEN' if platform == 'curseforge' else '_TOKEN'))
        project = os.environ.get(prefix + '_PROJECT_ID')
        if not args.dry_run and (not token or not project):
            raise ValueError(f'Missing {platform} token or project ID; publication aborted')
        secrets[platform] = token, project
    for item, jar in records:
        cf, mr = metadata(item, version, changelog, secrets.get('modrinth', (None, None))[1])
        for platform in platforms:
            if args.dry_run:
                print(f'[dry-run] {platform}: {jar.name} -> Minecraft {item["minecraft"]}, {item["loader"]}')
                continue
            token, project = secrets[platform]
            if platform == 'curseforge':
                result = upload(f'{CF}/projects/{project}/upload-file', 'X-Api-Token', token, 'metadata', cf, jar)
            else:
                result = upload(f'{MR}/version', 'Authorization', token, 'data', mr, jar)
            print(f'{platform} accepted {jar.name}: ID {result}', flush=True)


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, RuntimeError, KeyError) as error:
        print(f'ERROR: {error}', file=sys.stderr)
        sys.exit(1)
