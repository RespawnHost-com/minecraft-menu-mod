"""Collect production jars and their exact Minecraft targets."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def variants():
    return json.loads((ROOT / 'deploy/variants.json').read_text())


def production(path):
    return not any(part in path.stem for part in ('-sources', '-dev', '-javadoc', '-common'))


def packaged_version(jar):
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        if 'fabric.mod.json' in names:
            return json.loads(archive.read('fabric.mod.json'))['version']
        for name in ('META-INF/neoforge.mods.toml', 'META-INF/mods.toml'):
            if name in names:
                data = tomllib.loads(archive.read(name).decode('utf-8'))
                version = next(m['version'] for m in data['mods'] if m['modId'] == 'respawnhost_integration')
                if version == '${file.jarVersion}':
                    manifest = archive.read('META-INF/MANIFEST.MF').decode().replace('\r\n ', '')
                    return next(line.split(': ', 1)[1] for line in manifest.splitlines() if line.startswith('Implementation-Version: '))
                return version
        if 'mcmod.info' in names:
            return next(m['version'] for m in json.loads(archive.read('mcmod.info')) if m['modid'] == 'respawnhost_integration')
    raise ValueError(f'Missing mod metadata: {jar}')


def collect(variant, destination, version):
    entry = next(v for v in variants() if v['variant'] == variant)
    destination.mkdir(parents=True, exist_ok=True)
    records = []
    for output in entry['outputs']:
        jars = [p for p in (ROOT / 'versions' / variant / output['directory']).glob('*.jar') if production(p)]
        if len(jars) != 1:
            raise ValueError(f'{variant}/{output["loader"]}: expected one production jar, found {jars}')
        jar = jars[0]
        if packaged_version(jar) != version:
            raise ValueError(f'Embedded mod version does not match {version}: {jar}')
        name = f'respawnhost-integration-{version}-mc{output["minecraft"]}-{output["loader"]}.jar'
        shutil.copy2(jar, destination / name)
        records.append(dict(output, variant=variant, file=name, version=version,
                            sha256=hashlib.sha256(jar.read_bytes()).hexdigest()))
    (destination / f'{variant}.json').write_text(json.dumps(records, indent=2) + '\n')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--matrix', action='store_true')
    parser.add_argument('--variant')
    parser.add_argument('--version')
    parser.add_argument('--destination', type=Path, default=ROOT / 'dist')
    args = parser.parse_args()
    if args.matrix:
        print(json.dumps({'include': [{k: v[k] for k in ('variant', 'java')} for v in variants()]}))
    else:
        if not args.variant or not args.version:
            parser.error('--variant and --version are required')
        collect(args.variant, args.destination, args.version)
