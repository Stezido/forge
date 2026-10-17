#!/usr/bin/env python3
"""Package a reviewed Forge build for portable Windows playtests; no network or user data."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import zipfile
from card_images import card_image_filenames

ROOT = Path(__file__).resolve().parents[1]

def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build-root', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, required=True)
    parser.add_argument('--output', type=Path, default=ROOT/'.chaosbrawl-local/windows-release')
    parser.add_argument('--version', default='0.1.0-beta.1')
    args = parser.parse_args()
    if not re.fullmatch(r'\d+\.\d+\.\d+-[a-z0-9.]+', args.version):
        raise ValueError('Invalid version')
    build = args.build_root.resolve()
    subprocess.run(['git', '-C', str(build), 'diff', '--exit-code', 'HEAD', '--'], check=True, stdout=subprocess.DEVNULL)
    commit = subprocess.check_output(['git', '-C', str(build), 'rev-parse', 'HEAD'], text=True).strip()
    batch = build/'chaosbrawl/batches/owner-decisions-001'
    source = json.loads((batch/'source.json').read_text())
    scripts = json.loads((batch/'scripts.json').read_text())
    for item in scripts['files']:
        if digest(build/item['path']) != item['sha256']:
            raise ValueError('Reviewed executable mismatch: '+item['path'])
    runtime = json.loads((ROOT/'chaosbrawl/releases/windows-runtime.json').read_text())
    if digest(args.runtime) != runtime['sha256'] or args.runtime.stat().st_size != runtime['bytes']:
        raise ValueError('Windows runtime checksum/size mismatch')
    jars = list((build/'forge-gui-desktop/target').glob('*-jar-with-dependencies.jar'))
    if len(jars) != 1:
        raise ValueError('Build the reviewed desktop jar first')
    cards = {}
    for path in sorted((build/'chaosbrawl/batches').glob('*/source.json')):
        if path.parent.name <= batch.name:
            cards.update({c['id']: c for c in json.loads(path.read_text())['cards']})
    if len(cards) != 314:
        raise ValueError('Expected the reviewed 314 source records')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    name = 'ChaosBrawl-Forge-'+args.version+'-Windows-x64'
    target = output/name
    if target.exists():
        raise ValueError('Refusing to overwrite an existing package directory: '+str(target))
    target.mkdir()
    shutil.copyfile(jars[0], target/'forge.jar')
    shutil.copytree(build/'forge-gui/res', target/'res')
    with zipfile.ZipFile(args.runtime) as z:
        for info in z.infolist():
            member = PurePosixPath(info.filename)
            if member.is_absolute() or '..' in member.parts or len(member.parts) < 1:
                raise ValueError('Unsafe runtime archive path')
            relative = Path(*member.parts[1:])
            if str(relative) == '.':
                continue
            dest = target/'runtime'/relative
            if info.is_dir():
                dest.mkdir(parents=True, exist_ok=True)
            else:
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(z.read(info))
    if not (target/'runtime/bin/javaw.exe').is_file():
        raise ValueError('Windows runtime is incomplete')
    pictures = target/'Cache/pics/cards/CBT'
    pictures.mkdir(parents=True)
    native = build/'.chaosbrawl-local/rendered'/source['sourceSha256']
    ordered = [card for cid, card in sorted(cards.items())]
    filenames = card_image_filenames(ordered, build/'forge-gui/res/editions/Chaos Brawl Local Test.txt')
    for card in ordered:
        shutil.copyfile(native/(card['nativeUid']+'.png'), pictures/filenames[card['id']])
    token_source = build/'.chaosbrawl-local/cache/pics/tokens'
    token_target = target/'Cache/pics/tokens'
    token_target.mkdir(parents=True)
    for image in sorted(token_source.glob('*.png')):
        shutil.copyfile(image, token_target/image.name)
    decks = target/'Decks/constructed'
    decks.mkdir(parents=True)
    for deck in sorted((build/'.chaosbrawl-local/decks/constructed').glob('Chaos Brawl Beta *.dck')):
        shutil.copyfile(deck, decks/deck.name)
    if len(list(decks.glob('*.dck'))) != 41:
        raise ValueError('Expected 41 reviewed test decks')
    (target/'forge.profile.properties').write_text('userDir=UserData\ncacheDir=Cache\ndecksDir=Decks\ndecksConstructedDir=Decks/constructed\n')
    prefs = target/'UserData/preferences'
    prefs.mkdir(parents=True)
    (prefs/'forge.preferences').write_text('AUTO_UPDATE=none\nCHECK_SNAPSHOT_AT_STARTUP=false\n')
    launcher = '''@echo off
setlocal
cd /d "%~dp0"
set "SENTRY_ENABLED=false"
if not exist "runtime\\bin\\javaw.exe" (
  echo Please extract the entire ZIP before starting Chaos Brawl.
  pause
  exit /b 1
)
start "" "runtime\\bin\\javaw.exe" -Xmx4g -Dforge.chaosbrawl=true -Dsentry.enabled=false -Dfile.encoding=UTF-8 -Dio.netty.tryReflectionSetAccessible=true -jar forge.jar
'''
    (target/'Play Chaos Brawl.cmd').write_bytes(launcher.replace('\n','\r\n').encode('ascii'))
    for notice in ['LICENSE', 'LICENSE.txt', 'LICENSE.md', 'COPYING', 'NOTICE', 'NOTICE.txt']:
        if (build/notice).is_file():
            shutil.copyfile(build/notice, target/notice)
    metadata = {'version':args.version, 'platform':'windows-x64', 'engineCommit':commit,
                'mseSha256':source['sourceSha256'], 'jarSha256':digest(jars[0]),
                'runtime':runtime, 'sourceRecords':len(cards), 'nonTokenRecords':sum(not c['token'] for c in cards.values()),
                'tokenRecords':sum(c['token'] for c in cards.values()), 'testDecks':41}
    (target/'release.json').write_text(json.dumps(metadata, indent=2)+'\n')
    (target/'README.txt').write_text(f'''Chaos Brawl for Forge {args.version} - Windows x64

1. Extract the entire ZIP into a writable folder.
2. Double-click "Play Chaos Brawl.cmd". Java is included.
3. Choose Constructed, then a Chaos Brawl Beta Batch 01-08 deck.
   Batch 08 includes the clarified cards and the three new instants.
   For smaller groups, choose a Chaos Brawl Beta NN - Group N deck.
4. Filter the card pool to Chaos Brawl Beta Local Test (CBT).

Your settings and decks stay in this portable folder, separate from ordinary Forge.
Keep UserData and Decks when moving/backing up your installation.
Do not install upstream updates over this custom build.

All 314 Beta records: 292 non-token entries and 22 authored token mappings.
These are exercise decks, not balanced competitive decks.
Online games use Forge's Host/Join lobby. Every player needs the same custom build.
A remote Chaos Brawl playtest is not yet verified.

Matching source, version information and feedback: the website Playtest area.
Engine commit: {commit}
Beta MSE SHA-256: {source['sourceSha256']}
Forge retains its upstream GPL notices. Bundled Temurin notices are in runtime/legal.
Bundled runtime and source: https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1
''')
    source_zip = output/('ChaosBrawl-Forge-'+args.version+'-Source.zip')
    subprocess.run(['git','-C',str(build),'archive','--format=zip','--prefix=ChaosBrawl-Forge-Source/','--output='+str(source_zip),commit], check=True)
    archive = output/(target.name+'.zip')
    seen = set()
    with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for file in sorted(target.rglob('*')):
            if not file.is_file():
                continue
            relative = str(file.relative_to(target)).replace('\\','/')
            if relative.casefold() in seen:
                raise ValueError('Windows path collision: '+relative)
            seen.add(relative.casefold())
            z.write(file, target.name+'/'+relative)
    artifacts = {'windows':{'filename':archive.name,'bytes':archive.stat().st_size,'sha256':digest(archive)},
                 'source':{'filename':source_zip.name,'bytes':source_zip.stat().st_size,'sha256':digest(source_zip)}}
    result = {**metadata, 'artifacts':artifacts}
    (output/'release-manifest.json').write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()
