#!/usr/bin/env python3
"""Package a reviewed Forge build for portable Windows playtests; no network or user data."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import struct
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

def write_shell_link(path, relative_target, display_name, icon_name):
    """Write a portable Unicode Shell Link with a relative target and icon."""
    clsid = bytes.fromhex('0114020000000000c000000000000046')
    flags = 0x0004 | 0x0008 | 0x0040 | 0x0080
    header = struct.pack('<I16sIIQQQIiIHHII', 0x4C, clsid, flags, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0)

    def string_data(value):
        encoded = value.encode('utf-16le')
        return struct.pack('<H', len(encoded) // 2) + encoded

    path.write_bytes(header + b''.join((
        string_data(display_name),
        string_data(relative_target.replace('/', '\\')),
        string_data(icon_name.replace('/', '\\')),
    )) + b'\0\0\0\0')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build-root', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, required=True)
    parser.add_argument('--output', type=Path, default=ROOT/'.chaosbrawl-local/windows-release')
    parser.add_argument('--version', default='0.1.0-beta.6')
    parser.add_argument('--java', default='java', help='JDK 21+ executable for the runtime image-path audit')
    args = parser.parse_args()
    if not re.fullmatch(r'\d+\.\d+\.\d+-[a-z0-9.]+', args.version):
        raise ValueError('Invalid version')
    build = args.build_root.resolve()
    subprocess.run(['git', '-C', str(build), 'diff', '--exit-code', 'HEAD', '--'], check=True, stdout=subprocess.DEVNULL)
    commit = subprocess.check_output(['git', '-C', str(build), 'rev-parse', 'HEAD'], text=True).strip()
    batch = sorted((build/'chaosbrawl/batches').glob('*/source.json'))[-1].parent
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
    adventure_jars = list((build/'forge-gui-mobile-dev/target').glob('*-jar-with-dependencies.jar'))
    if len(adventure_jars) != 1:
        raise ValueError('Build the reviewed Adventure desktop jar first')
    code = 'CBFRbeta'
    cards = {}
    for path in sorted((build/'chaosbrawl/batches').glob('*/source.json')):
        if path.parent.name <= batch.name:
            cards.update({c['id']: c for c in json.loads(path.read_text())['cards']})
    expected_records = source.get('sourceRecordCount')
    if not isinstance(expected_records, int) or len(cards) != expected_records:
        raise ValueError(f'Expected {expected_records!r} reviewed source records, found {len(cards)}')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    name = 'ChaosBrawl-Forge-'+args.version+'-Windows-x64'
    target = output/name
    if target.exists():
        raise ValueError('Refusing to overwrite an existing package directory: '+str(target))
    target.mkdir()
    support = target/'support'
    support.mkdir()
    shutil.copyfile(jars[0], support/'forge.jar')
    shutil.copyfile(adventure_jars[0], support/'forge-adventure.jar')
    launchers = [build/'forge-gui-desktop/target/forge.exe', build/'forge-gui-mobile-dev/target/forge-adventure.exe']
    if not all(path.is_file() for path in launchers):
        raise ValueError('Build the reviewed desktop and Adventure launchers first')
    shutil.copyfile(launchers[0], target/'Chaos Brawl Forge.exe')
    shutil.copyfile(launchers[1], target/'Chaos Brawl Adventure.exe')
    icon_assets = ROOT/'chaosbrawl/assets'
    if not all((icon_assets/icon).is_file() for icon in ('forge-chaos-brawl.ico','forge-adventure-chaos-brawl.ico')):
        raise ValueError('Missing reviewed launcher icon assets')
    shutil.copytree(build/'forge-gui/res', target/'res')
    plane = target/'res/adventure/ChaosBrawl'
    if plane.exists():
        raise ValueError('Unexpected pre-existing ChaosBrawl plane')
    shutil.copytree(target/'res/adventure/Shandalar',plane)
    config=json.loads((target/'res/adventure/common/config.json').read_text())
    config.update(allowedEvents=[code],eventFormats=['Draft','Sealed'])
    (plane/'config.json').write_text(json.dumps(config,ensure_ascii=False,indent=2)+'\n')
    with zipfile.ZipFile(args.runtime) as z:
        for info in z.infolist():
            member = PurePosixPath(info.filename)
            if member.is_absolute() or '..' in member.parts or len(member.parts) < 1:
                raise ValueError('Unsafe runtime archive path')
            relative = Path(*member.parts[1:])
            if str(relative) == '.':
                continue
            dest = support/'runtime'/relative
            if info.is_dir():
                dest.mkdir(parents=True, exist_ok=True)
            else:
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(z.read(info))
    if not (support/'runtime/bin/javaw.exe').is_file():
        raise ValueError('Windows runtime is incomplete')
    pictures = target/'Cache/pics/cards'/code
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
    seed_decks = sorted((build/'.chaosbrawl-local/decks/constructed').glob('Chaos Brawl Beta *.dck'))
    if not seed_decks:
        raise ValueError('No reviewed test decks prepared')
    for deck in seed_decks:
        shutil.copyfile(deck, decks/deck.name)
    if len(list(decks.glob('*.dck'))) != len(seed_decks):
        raise ValueError('Prepared test deck copy incomplete')
    (target/'forge.profile.properties').write_text('userDir=UserData\ncacheDir=Cache\ndecksDir=Decks\ndecksConstructedDir=Decks/constructed\n')
    prefs = target/'UserData/preferences'
    prefs.mkdir(parents=True)
    (prefs/'forge.preferences').write_text('AUTO_UPDATE=none\nCHECK_SNAPSHOT_AT_STARTUP=false\nALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE=true\nDECKGEN_CARDBASED=false\n')
    adventure_settings=target/'UserData/adventure'
    adventure_settings.mkdir(parents=True)
    (adventure_settings/'settings.json').write_text(json.dumps({'plane':'ChaosBrawl','width':1280,'height':720,'videomode':'720p'})+'\n')
    # Restore the original portable CMD entry points removed by layout polishing.
    launcher = '''@echo off
setlocal
cd /d "%~dp0.."
set "SENTRY_ENABLED=false"
if not exist "support\\runtime\\bin\\javaw.exe" (
  echo Please extract the entire ZIP before starting Chaos Brawl.
  pause
  exit /b 1
)
start "" "support\\runtime\\bin\\javaw.exe" -Xmx4g -Dforge.chaosbrawl=true -Dsentry.enabled=false -Dfile.encoding=UTF-8 -Dio.netty.tryReflectionSetAccessible=true -jar support/forge.jar
'''
    (support/'launch-chaos-brawl.cmd').write_bytes(launcher.replace('\n','\r\n').encode('ascii'))
    (support/'launch-adventure.cmd').write_bytes(launcher.replace('-jar support/forge.jar','-jar support/forge-adventure.jar').replace('\n','\r\n').encode('ascii'))
    for icon in ('forge-chaos-brawl.ico', 'forge-adventure-chaos-brawl.ico'):
        shutil.copyfile(icon_assets/icon, support/icon)
    write_shell_link(target/'Play Chaos Brawl.lnk', 'support/launch-chaos-brawl.cmd', 'Play Chaos Brawl', 'support/forge-chaos-brawl.ico')
    write_shell_link(target/'Play Adventure.lnk', 'support/launch-adventure.cmd', 'Play Adventure', 'support/forge-adventure-chaos-brawl.ico')
    subprocess.run([args.java, '-Xmx4g', '-Djava.awt.headless=true', '-cp', str(support/'forge.jar'),
                    str(ROOT/'chaosbrawl/VerifyWindowsLaunchers.java'), str(target)], check=True)
    for notice in ['LICENSE', 'LICENSE.txt', 'LICENSE.md', 'COPYING', 'NOTICE', 'NOTICE.txt']:
        if (build/notice).is_file():
            shutil.copyfile(build/notice, target/notice)
    metadata = {'version':args.version, 'platform':'windows-x64', 'engineCommit':commit,
                'mseSha256':source['sourceSha256'], 'jarSha256':digest(jars[0]),
                'adventureJarSha256':digest(adventure_jars[0]), 'setCode':code, 'reservedReleaseSetCode':'CBFR',
                'runtime':runtime, 'sourceRecords':len(cards), 'nonTokenRecords':sum(not c['token'] for c in cards.values()),
                'tokenRecords':sum(c['token'] for c in cards.values()), 'testDecks':len(seed_decks)}
    (target/'release.json').write_text(json.dumps(metadata, indent=2)+'\n')
    # A render count alone misses functional-variant/art-index naming errors.
    # Exercise the actual packaged engine's lookup before producing a ZIP.
    with (output/(name+'-image-audit.log')).open('w') as log:
        subprocess.run([args.java, '-Xmx4g', '-Djava.awt.headless=true', '-cp',
                        str(support/'forge.jar'), str(ROOT/'chaosbrawl/VerifyPackageImages.java'),
                        str(target), str(sum(not c['token'] for c in cards.values()))],
                       stdout=log, stderr=subprocess.STDOUT, check=True)
    with (output/(name+'-adventure-audit.log')).open('w') as log:
        subprocess.run([args.java,'-Xmx4g','-Dforge.chaosbrawl=true','-Dsentry.enabled=false','-Djava.awt.headless=true',
                        '-cp',str(support/'forge-adventure.jar'),str(ROOT/'chaosbrawl/VerifyAdventureEvents.java'),
                        '--games','--images',str(sum(not c['token'] for c in cards.values()))],
                       cwd=target,stdout=log,stderr=subprocess.STDOUT,check=True)
    for jar in ('forge.jar', 'forge-adventure.jar'):
        with (output/(name+'-'+jar+'-mechanics-audit.log')).open('w') as log:
            subprocess.run([args.java, '-Xmx4g', '-Dforge.chaosbrawl=true', '-Dsentry.enabled=false',
                            '-Djava.awt.headless=true', '-cp', str(support/jar),
                            str(ROOT/'chaosbrawl/VerifyCustomMechanics.java')], cwd=target,
                           stdout=log, stderr=subprocess.STDOUT, check=True)
    batch_decks = [deck.name for deck in seed_decks if 'Batch' in deck.name]
    batch_label = batch_decks[-1].removesuffix('.dck') if batch_decks else seed_decks[-1].stem
    (target/'README.txt').write_text(f'''Chaos Brawl for Forge {args.version} - Windows x64

1. Extract the entire ZIP into a writable folder.
2. Double-click "Play Chaos Brawl.lnk" (or support/launch-chaos-brawl.cmd).
   The branded "Chaos Brawl Forge.exe" is also available.
3. Choose Constructed, then a Chaos Brawl Beta deck such as {batch_label}.
   For smaller groups, choose a Chaos Brawl Beta NN - Group N deck.
4. Filter the card pool to Chaos Brawl - The Foul Realms Beta (CBFRbeta).

For Adventure: close the classic client, then double-click "Play Adventure.lnk"
(or support/launch-adventure.cmd). "Chaos Brawl Adventure.exe" is also available.
Choose Adventure Mode. The separate ChaosBrawl plane offers this set at town inns.
Draft uses 3 packs (36 picks); Sealed uses 8 packs (96 cards).
Normal virtual entry fees: Draft 3000 gold / 50 shards; Sealed 6000 / 100.
M21 supplies normal basic lands. Draft rankings use owner-confirmed Limited ratings.
Finish the character tutorial, then visit a town inn and choose Info.

The support folder contains the two application JARs and bundled Java runtime.
This release keeps the Beta set code CBFRbeta; CBFR is reserved for the full set.
CBT remains an edition alias for old decks. Your old installation is preserved;
back up UserData and Decks before transferring them into this version.
Keep Adventure saves under their existing plane name. A new profile defaults to
ChaosBrawl; an existing profile can select it in Adventure Settings.
Do not copy old card-image cache files over the included CBFRbeta cache.

Your settings and decks stay in this portable folder, separate from ordinary Forge.
Keep UserData and Decks when moving/backing up your installation.
Do not install upstream updates over this custom build.

All {len(cards)} Beta records: {sum(not c['token'] for c in cards.values())} non-token entries and {sum(c['token'] for c in cards.values())} authored token mappings.
These are exercise decks, not balanced competitive decks.
Online games use Forge's Host/Join lobby. Every player needs the same custom build.
A remote Chaos Brawl playtest is not yet verified.

Matching source, version information and feedback: the website Playtest area.
Engine commit: {commit}
Beta MSE SHA-256: {source['sourceSha256']}
Forge retains its upstream GPL notices. Bundled Temurin notices are in support/runtime/legal.
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
