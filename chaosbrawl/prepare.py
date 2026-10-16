#!/usr/bin/env python3
"""Prepare the isolated Beta batch from its pinned canonical MSE source."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import unicodedata
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BATCH = ROOT / 'chaosbrawl/batches/001-050'
RUNTIME = ROOT / '.chaosbrawl-local'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--website', type=Path, default=Path('/home/stefan/github/Chaosbrawl-Website'))
    parser.add_argument('--mse', type=Path, default=Path('/mnt/c/MTG/Full-Magic-Pack-main-new-version/mse.com'))
    parser.add_argument('--render', action='store_true', help='Render the complete canonical source using native MSE')
    args = parser.parse_args()
    website = args.website.resolve()
    pinned = json.loads((BATCH / 'source.json').read_text())
    source = website / pinned['sourcePath']
    if sha(source.read_bytes()) != pinned['sourceSha256']:
        raise ValueError('The canonical Beta changed. Review and migrate its differences before preparing this batch.')
    manifest = json.loads((website / 'sets/the-foul-realms/manifest.json').read_text())
    variant = next(v for v in manifest['variants'] if v['id'] == pinned['variant'])
    if variant['sourceSha256'] != pinned['sourceSha256']:
        raise ValueError('Registered source differs from the reviewed batch.')
    reviewed = json.loads((BATCH / 'scripts.json').read_text())
    for entry in reviewed['files']:
        if sha((ROOT / entry['path']).read_bytes()) != entry['sha256']:
            raise ValueError('Executable scripts changed: run tests and review before refreshing their hashes.')
    RUNTIME.mkdir(exist_ok=True)
    current = RUNTIME / 'current-beta.json'
    subprocess.run([sys.executable, str(website / 'scripts/import-mse.py'), '--variant', pinned['variant'],
                    '--output', str(current), '--metadata-only'], check=True)
    live = {c['id']:c for c in json.loads(current.read_text())}
    for card in pinned['cards']:
        expected = {k:v for k,v in card.items() if k != 'nativeUid'}
        if live.get(card['id']) != expected:
            raise ValueError('Imported source no longer matches the reviewed card: ' + card['id'])
    images = RUNTIME / 'rendered' / pinned['sourceSha256']
    if args.render:
        images.mkdir(parents=True, exist_ok=True)
        if not args.mse.is_file():
            raise ValueError('Native MSE executable missing; pass --mse or render UID-named PNGs into ' + str(images))
        def win(path):
            return subprocess.check_output(['wslpath','-w',str(path)], text=True).strip()
        with (images / 'mse-render.log').open('wb') as log:
            subprocess.run([str(args.mse), '--export-images', win(source), win(images / '{card.uid}.png')],
                           stdout=log, stderr=subprocess.STDOUT, check=True, timeout=600)
        if (images / 'mse-render.log').stat().st_size:
            raise ValueError('MSE reported warnings. Inspect the local render log before using the package.')
    cards_dir = RUNTIME / 'cache/pics/cards/CBT'
    token_dir = RUNTIME / 'cache/pics/tokens'
    for folder in [cards_dir, token_dir, RUNTIME/'decks/constructed', RUNTIME/'run', RUNTIME/'cache/skins']:
        folder.mkdir(parents=True, exist_ok=True)
    for card in pinned['cards']:
        original = images / (card['nativeUid'] + '.png')
        if not original.is_file():
            raise ValueError('Missing full card render. Run this helper with --render first: ' + card['name'])
        name = unicodedata.normalize('NFD', card['name'])
        name = ''.join(c for c in name if not unicodedata.combining(c) and c not in '\"/:?')
        shutil.copyfile(original, cards_dir / (name + '.full.png'))
    # Token cache uses extracted illustration bytes; card faces above are native full renders.
    identity = {c['name']:c for c in pinned['supportingTokens']}
    with zipfile.ZipFile(source) as archive:
        sys.path.insert(0, str(website / 'scripts/mse'))
        from archive import card_sections, read_archive
        text, _ = read_archive(source)
        native = card_sections(text)
        for name, script in [('Beer','chaosbrawl_beer'), *[(n+' Beer','chaosbrawl_'+n.lower()+'_beer') for n in ['Expired','Zwickl','Wheat','Dark','Pils','Bock']]]:
            entry = identity[name]
            member = native[entry['nativeUid']][2]['image']
            (token_dir / (script + '.png')).write_bytes(archive.read(member))
    cards = pinned['cards']
    decks = [('Chaos Brawl Beta Batch 01', cards, 1, 10)]
    decks += [(f'Chaos Brawl Beta 01 - Group {n//10+1}', cards[n:n+10], 4, 6) for n in range(0,50,10)]
    for name, entries, copies, lands in decks:
        lines = ['[metadata]', 'Name='+name, '[Main]']
        lines += [f'{copies} {c["name"]}|CBT' for c in entries]
        lines += [f'{lands} {land}' for land in ['Plains','Island','Swamp','Mountain','Forest']]
        (RUNTIME/'decks/constructed'/f'{name}.dck').write_text('\n'.join(lines)+'\n')
    run = RUNTIME / 'run'
    resources = run / 'res'
    if not resources.exists():
        resources.symlink_to(ROOT/'forge-gui/res', target_is_directory=True)
    elif not resources.is_symlink() or resources.resolve() != ROOT/'forge-gui/res':
        raise ValueError('Refusing to replace an unrelated runtime resource directory.')
    (run/'forge.profile.properties').write_text(f'userDir={RUNTIME}/user\ncacheDir={RUNTIME}/cache\ndecksDir={RUNTIME}/decks\ndecksConstructedDir={RUNTIME}/decks/constructed\n')
    print('Prepared 50 reviewed Beta cards, native full renders and six isolated test decks.')

if __name__ == '__main__':
    main()
