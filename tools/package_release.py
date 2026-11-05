"""Audit and package built runtime/source JARs. Standard library only; no downloads."""
from pathlib import Path
import hashlib
import json
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line)
version = properties['mod_version']
stem = f"turkey-time-{version}"
jar = ROOT/'build/libs'/f'{stem}.jar'
sources = ROOT/'build/libs'/f'{stem}-sources.jar'
expected_titles = ['Turkey Hunter', 'Smells Good', 'Happy Thanksgiving!', 'Turkey Hatchery', 'Turkey Breeder']

def sha(data):
    return hashlib.sha256(data).hexdigest()

with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    meta = json.loads(archive.read('fabric.mod.json'))
    assert meta['version'] == version and meta['authors'] == ['MadReaper16']
    assert meta['depends']['minecraft'] == '26.3'
    assert meta['depends']['fabric-api'] == '>=0.165.0+26.3'
    assert meta['license'] == 'MIT'
    assert b'MIT License' in archive.read('LICENSE')
    assert '${' not in archive.read('fabric.mod.json').decode()
    assert not any('GameTests' in n or 'turkeytime_tests' in n or n.startswith('net/minecraft/') for n in names)
    assert not any(n.startswith(('run/', 'art/', 'docs/', '.gradle/')) for n in names)
    assert 'COPYRIGHT.txt' in names
    assert meta['icon'] in names
    assert meta['icon'] == 'assets/turkeytime/icon.png'
    icon = archive.read(meta['icon'])
    assert icon.startswith(b'\x89PNG\r\n\x1a\n') and struct.unpack('>II', icon[16:24]) == (512, 512)
    assert 'com/madreaper16/turkeytime/client/TurkeyTimeClient.class' in names
    assert 'com/madreaper16/turkeytime/mixin/CampfireCookingMixin.class' in names
    mixin = json.loads(archive.read('turkeytime.mixins.json'))
    assert mixin['required'] and mixin['mixins'] == ['CampfireCookingMixin']
    lang = json.loads(archive.read('assets/turkeytime/lang/en_us.json'))
    titles = [lang[f'advancements.turkeytime.{key}.title'] for key in
              ['turkey_hunter', 'smells_good', 'happy_thanksgiving', 'turkey_hatchery', 'turkey_breeder']]
    assert titles == expected_titles
    json_count = 0
    for name in names:
        if name.endswith('.json'):
            json.loads(archive.read(name)); json_count += 1
    for block in ['cooked_turkey', 'thanksgiving_feast']:
        variants = json.loads(archive.read(f'assets/turkeytime/blockstates/{block}.json'))['variants']
        assert len(variants) == 32
        for bite in range(8):
            model = json.loads(archive.read(f'assets/turkeytime/models/block/{block}_{bite}.json'))
            assert model['elements']
    for bird in ['tom', 'hen', 'poult']:
        assert f'assets/turkeytime/textures/entity/turkey/{bird}_autumn.png' in names
    recipe = json.loads(archive.read('data/turkeytime/recipe/thanksgiving_feast.json'))
    assert recipe['pattern'] == ['BTC', 'SSS'] and recipe['key']['S'] == '#minecraft:wooden_slabs'
    sounds = json.loads(archive.read('assets/turkeytime/sounds.json'))
    sound_files = set()
    for event in sounds.values():
        assert event['subtitle'] in lang
        for sound in event['sounds']:
            namespace, path = sound.split(':')
            path = f'assets/{namespace}/sounds/{path}.ogg'
            assert path in names and archive.read(path).startswith(b'OggS')
            sound_files.add(path)
    assert len(sound_files) == 7
with zipfile.ZipFile(sources) as archive:
    assert 'com/madreaper16/turkeytime/entity/Turkey.java' in archive.namelist()
    assert b'MIT License' in archive.read('LICENSE')

out = ROOT/'build/release'/version
out.mkdir(parents=True, exist_ok=True)
files = {
    jar.name: jar.read_bytes(), sources.name: sources.read_bytes(),
    'README.md': (ROOT/'README.md').read_text(encoding='utf-8').replace(
        'src/main/resources/assets/turkeytime/icon.png', 'turkey-time-mod-icon.png').encode('utf-8'),
    'COPYRIGHT.txt': (ROOT/'COPYRIGHT.txt').read_bytes(),
    'THIRD_PARTY_NOTICES.md': (ROOT/'THIRD_PARTY_NOTICES.md').read_bytes(),
    'turkey-time-curseforge.png': (ROOT/'art/branding/turkey-time-curseforge.png').read_bytes(),
    'turkey-time-mod-icon.png': (ROOT/'art/branding/turkey-time-mod-icon.png').read_bytes(),
}
assert struct.unpack('>II', files['turkey-time-curseforge.png'][16:24]) == (400, 400)
assert files['turkey-time-mod-icon.png'] == icon
for doc in ['INSTALL', 'CONFIG', 'KNOWN_ISSUES', 'TESTING', 'CREDITS', 'ASSET_MANIFEST']:
    files[f'{doc}.md'] = (ROOT/f'docs/{doc}.md').read_bytes()
if (ROOT/'LICENSE').exists():
    files['LICENSE'] = (ROOT/'LICENSE').read_bytes()
manifest = {'version': version, 'license': meta['license'], 'files': {
    name: {'sha256': sha(data), 'bytes': len(data)} for name, data in sorted(files.items())}}
files['MANIFEST.json'] = (json.dumps(manifest, indent=2) + '\n').encode()
checksums = ''.join(f'{sha(data)}  {name}\n' for name, data in sorted(files.items()))
files['SHA256SUMS.txt'] = checksums.encode()
bundle = out/f'{stem}-package.zip'
with zipfile.ZipFile(bundle, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for name, data in sorted(files.items()):
        info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        archive.writestr(info, data, compresslevel=9)
with zipfile.ZipFile(bundle) as archive:
    assert archive.testzip() is None
    assert set(archive.namelist()) == set(files)
    for name, data in files.items():
        assert sha(archive.read(name)) == sha(data)
for name, data in files.items():
    (out/name).write_bytes(data)
report = {'version': version, 'runtime_sha256': sha(jar.read_bytes()), 'sources_sha256': sha(sources.read_bytes()),
          'package_sha256': sha(bundle.read_bytes()), 'packaged_json_resources': json_count,
          'sound_files': len(sound_files), 'advancement_titles': titles,
          'runtime_excludes_tests_and_game_code': True, 'package_files': sorted(files),
          'private_worlds_accounts_logs_and_build_caches_excluded': True}
(out/'AUDIT.json').write_text(json.dumps(report, indent=2) + '\n')
(out/'PACKAGE-SHA256.txt').write_text(f'{sha(bundle.read_bytes())}  {bundle.name}\n')
print(json.dumps(report, indent=2))
print(bundle)
