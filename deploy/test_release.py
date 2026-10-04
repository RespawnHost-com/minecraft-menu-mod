import hashlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import zipfile

import artifacts
import upload


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def fixture(self):
        for variant in artifacts.variants():
            folder = self.root / variant['variant']
            folder.mkdir()
            records = []
            for output in variant['outputs']:
                name = output['loader'] + '.jar'
                (folder / name).write_bytes(b'fixture jar')
                records.append(dict(output, variant=variant['variant'], file=name,
                                    version='1.1.0', sha256=hashlib.sha256(b'fixture jar').hexdigest()))
            (folder / 'artifacts.json').write_text(json.dumps(records))

    def test_matrix_covers_every_build(self):
        self.assertEqual({v['variant'] for v in artifacts.variants()},
                         {p.name for p in (artifacts.ROOT / 'versions').iterdir() if p.is_dir()})

    def test_flattened_artifact_directories(self):
        self.fixture()
        records = upload.discover(self.root, '1.1.0')
        self.assertEqual(len(records), sum(len(v['outputs']) for v in artifacts.variants()))

    def test_missing_artifacts_fail(self):
        with self.assertRaisesRegex(ValueError, 'Missing release artifacts'):
            upload.discover(self.root, '1.1.0')

    def test_corrupt_artifact_fails(self):
        self.fixture()
        next(self.root.rglob('*.jar')).write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError, 'Checksum mismatch'):
            upload.discover(self.root, '1.1.0')

    def test_wrong_release_fails(self):
        self.fixture()
        with self.assertRaisesRegex(ValueError, 'Incorrect artifact metadata'):
            upload.discover(self.root, '1.2.0')

    def test_duplicate_artifact_fails(self):
        self.fixture()
        manifest = next(self.root.rglob('*.json'))
        manifest.with_name('duplicate.json').write_bytes(manifest.read_bytes())
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            upload.discover(self.root, '1.1.0')

    def test_metadata_exact_versions_and_dependencies(self):
        item = dict(loader='fabric', minecraft='26.1.2', architectury=False)
        changelog = 'Quotes " and tabs\tand newlines\nGrüße'
        cf, mr = upload.metadata(item, '1.1.0-rc.1', changelog, 'project')
        self.assertEqual(cf['gameVersionNames'], ['26.1.2', 'Fabric', 'Client'])
        self.assertEqual(cf['releaseType'], 'beta')
        self.assertEqual(mr['file_parts'], ['file'])
        self.assertEqual(mr['environment'], 'client_only')
        self.assertEqual(mr['dependencies'][0]['project_id'], 'P7dR8mSH')
        self.assertEqual(json.loads(json.dumps(cf))['changelog'], changelog)
        item.update(minecraft='1.21.1', architectury=True)
        cf, mr = upload.metadata(item, '1.1.0', '', 'project')
        self.assertEqual(len(cf['relations']['projects']), 2)
        self.assertEqual(len(mr['dependencies']), 2)

    def test_excludes_development_jars(self):
        for name in ['mod-sources.jar', 'mod-dev.jar', 'mod-dev-shadow.jar', 'mod-common-1.0.0.jar']:
            self.assertFalse(artifacts.production(Path(name)))
        self.assertTrue(artifacts.production(Path('mod-1.0.0.jar')))

    def test_packaged_versions_across_loaders(self):
        cases = [
            ('fabric.mod.json', '{"version":"1.1.0"}'),
            ('mcmod.info', '[{"modid":"respawnhost_integration","version":"1.1.0"}]'),
            ('META-INF/mods.toml', '[[mods]]\nmodId="respawnhost_integration"\nversion="1.1.0"'),
            ('META-INF/neoforge.mods.toml', '[[mods]]\nmodId="respawnhost_integration"\nversion="1.1.0"'),
            ('META-INF/mods.toml', '[[mods]]\nmodId="respawnhost_integration"\nversion="${file.jarVersion}"'),
        ]
        for name, data in cases:
            with self.subTest(name=name, data=data):
                jar = self.root / 'mod.jar'
                with zipfile.ZipFile(jar, 'w') as archive:
                    archive.writestr(name, data)
                    archive.writestr('META-INF/MANIFEST.MF', 'Implementation-Version: 1.1.0\r\n')
                self.assertEqual(artifacts.packaged_version(jar), '1.1.0')

    def test_collection_rejects_wrong_embedded_version(self):
        folder = self.root / 'versions/example/build/libs'
        folder.mkdir(parents=True)
        with zipfile.ZipFile(folder / 'mod.jar', 'w') as archive:
            archive.writestr('fabric.mod.json', '{"version":"1.0.0"}')
        matrix = [dict(variant='example', outputs=[dict(loader='fabric', minecraft='26.3',
                   directory='build/libs', architectury=False)])]
        with patch.object(artifacts, 'ROOT', self.root), patch.object(artifacts, 'variants', return_value=matrix):
            with self.assertRaisesRegex(ValueError, 'Embedded mod version'):
                artifacts.collect('example', self.root / 'dist', '1.1.0')

    def test_upload_multipart_and_id(self):
        jar = self.root / 'mod.jar'
        jar.write_bytes(b'jar payload')
        with patch('urllib.request.urlopen', return_value=io.BytesIO(b'{"id":123}')) as request:
            self.assertEqual(upload.upload('https://example.test/upload', 'X-Api-Token', 'secret',
                                           'metadata', {'displayName': 'Grüße'}, jar), 123)
        sent = request.call_args.args[0]
        self.assertIn(b'name="metadata"', sent.data)
        self.assertIn(b'name="file"; filename="mod.jar"', sent.data)
        self.assertIn(b'jar payload', sent.data)

    def test_http_errors_and_missing_ids_fail(self):
        jar = self.root / 'mod.jar'
        jar.write_bytes(b'jar')
        with patch('urllib.request.urlopen', side_effect=urllib.error.HTTPError('https://example.test', 401, '', {}, None)):
            with self.assertRaisesRegex(RuntimeError, 'HTTP 401'):
                upload.upload('https://example.test', 'Authorization', 'secret', 'data', {}, jar)
        with patch('urllib.request.urlopen', return_value=io.BytesIO(b'{}')):
            with self.assertRaisesRegex(ValueError, 'ID'):
                upload.upload('https://example.test', 'Authorization', 'secret', 'data', {}, jar)

    def test_dry_run_is_offline(self):
        self.fixture()
        with patch.dict('os.environ', {'MOD_VERSION': '1.1.0'}, clear=True), \
             patch('sys.argv', ['upload.py', str(self.root), '--dry-run']), \
             patch('urllib.request.urlopen') as network, patch('sys.stdout', new_callable=io.StringIO):
            upload.main()
        network.assert_not_called()

    def test_missing_secrets_prevent_upload(self):
        self.fixture()
        with patch.dict('os.environ', {'MOD_VERSION': '1.1.0'}, clear=True), \
             patch('sys.argv', ['upload.py', str(self.root)]), patch('urllib.request.urlopen') as network:
            with self.assertRaisesRegex(ValueError, 'Missing curseforge'):
                upload.main()
        network.assert_not_called()


if __name__ == '__main__':
    unittest.main()
