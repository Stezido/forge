#!/usr/bin/env python3
"""Bundle reviewed Windows Beta resources for the shared Android engine.

Only shipped resources, custom images and seed decks/settings are used. Never copy
a live player's profile. Rebuild the desktop staging package with package_windows.py
when the authoritative MSE source changes; the input must be a clean release ZIP.
"""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile
import io
import re


def bundle(release_zip: Path, manifest: Path, assets: Path):
    release = json.loads(manifest.read_text())
    expected = release['artifacts']['windows']
    digest = hashlib.file_digest(release_zip.open('rb'), 'sha256').hexdigest()
    if digest != expected['sha256'] or release_zip.stat().st_size != expected['bytes']:
        raise ValueError('Reviewed desktop release checksum/size mismatch')
    if not re.fullmatch(r'0\.1\.0-beta\.[1-9][0-9]*', release['version']) or release['setCode'] != 'CBFRbeta':
        raise ValueError('Expected a checksum-verified CBFRbeta desktop Beta release')
    assets.mkdir(parents=True, exist_ok=True)
    target = assets/'chaosbrawl-resources.zip'
    counts = {'resources': 0, 'cardScripts': 0, 'cardImages': 0, 'tokens': 0, 'decks': 0}
    card_buffer = io.BytesIO()
    with zipfile.ZipFile(release_zip) as source, zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as out, zipfile.ZipFile(card_buffer, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as cards:
        for item in source.infolist():
            if item.is_dir():
                continue
            parts = Path(item.filename).parts
            if len(parts) < 2 or '..' in parts or Path(item.filename).is_absolute():
                raise ValueError('Invalid release path')
            name = '/'.join(parts[1:])
            mapped = None
            if name.startswith('res/cardsfolder/'):
                cards.writestr(name[len('res/cardsfolder/'):], source.read(item))
                counts['cardScripts'] += 1
                counts['resources'] += 1
                continue
            if name.startswith('res/') and not name.endswith(('.xcf', '.psd')):
                mapped = name
                counts['resources'] += 1
            elif name.startswith('Cache/pics/'):
                mapped = 'cache/'+name[len('Cache/'):]
                counts['cardImages' if '/cards/' in name else 'tokens'] += 1
            elif name.startswith('Decks/constructed/') and name.endswith('.dck'):
                mapped = 'data/decks/constructed/'+Path(name).name
                counts['decks'] += 1
            elif name in ['LICENSE', 'LICENSE.txt', 'NOTICE', 'NOTICE.txt']:
                mapped = name
            if mapped:
                out.writestr(mapped, source.read(item))
        if counts['decks'] != release['testDecks'] or counts['cardImages'] < 292 or counts['resources'] < 1000:
            raise ValueError('Release resources are incomplete')
        cards.close()
        out.writestr('res/cardsfolder/cardsfolder.zip', card_buffer.getvalue(), compress_type=zipfile.ZIP_STORED)
        out.writestr('data/preferences/forge.preferences', 'AUTO_UPDATE=none\nCHECK_SNAPSHOT_AT_STARTUP=false\nALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE=true\nDECKGEN_CARDBASED=false\n')
        out.writestr('data/adventure/settings.json', json.dumps({'plane': 'ChaosBrawl'}))
        out.writestr('chaosbrawl-bundle.json', json.dumps({'desktopSource': release['engineCommit'], 'mseSha256': release['mseSha256'], 'setCode': release['setCode'], **counts}, indent=2))
    digest = hashlib.file_digest(target.open('rb'), 'sha256').hexdigest()
    (assets/'chaosbrawl-resources.sha256').write_text(digest+'\n')
    print(json.dumps({'file': str(target), 'bytes': target.stat().st_size, 'sha256': digest, **counts}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--desktop-zip', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--assets', type=Path, default=Path(__file__).resolve().parent.parent/'forge-gui-android/assets')
    args = parser.parse_args()
    bundle(args.desktop_zip, args.manifest, args.assets)
