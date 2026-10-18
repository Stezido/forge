#!/usr/bin/env python3
"""Prepare an isolated cumulative Beta package from reviewed source batches."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile
from card_images import card_image_filenames

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / '.chaosbrawl-local'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--website', type=Path, default=Path('/home/stefan/github/Chaosbrawl-Website'))
    parser.add_argument('--mse', type=Path, default=Path('/mnt/c/MTG/Full-Magic-Pack-main-new-version/mse.com'))
    parser.add_argument('--batch', default=sorted(p.parent.name for p in (ROOT/'chaosbrawl/batches').glob('*/source.json'))[-1])
    parser.add_argument('--render', action='store_true', help='Render the complete canonical source with native MSE')
    args = parser.parse_args()
    batch = ROOT / 'chaosbrawl/batches' / args.batch
    if batch.parent != ROOT/'chaosbrawl/batches' or not (batch/'source.json').is_file():
        raise ValueError('Unknown reviewed batch')
    pinned = json.loads((batch/'source.json').read_text())
    website = args.website.resolve()
    source = website / pinned['sourcePath']
    if sha(source.read_bytes()) != pinned['sourceSha256']:
        raise ValueError('Canonical Beta differs from this package. Review its changes before refreshing the pin.')
    manifest = json.loads((website/'sets/the-foul-realms/manifest.json').read_text())
    variant = next(v for v in manifest['variants'] if v['id'] == pinned['variant'])
    if variant['sourceSha256'] != pinned['sourceSha256']:
        raise ValueError('Registered source differs from the reviewed batch.')
    reviewed = json.loads((batch/'scripts.json').read_text())
    for entry in reviewed['files']:
        if sha((ROOT/entry['path']).read_bytes()) != entry['sha256']:
            raise ValueError('Executable scripts changed: run checks and review before refreshing their hashes: '+entry['path'])
    RUNTIME.mkdir(exist_ok=True)
    current = RUNTIME/'current-beta.json'
    subprocess.run([sys.executable, str(website/'scripts/import-mse.py'), '--variant', pinned['variant'],
                    '--output', str(current), '--metadata-only'], check=True)
    live = {c['id']:c for c in json.loads(current.read_text())}
    cards_by_id = {}
    documents = []
    pending = set()
    token_bindings = {}
    batches = sorted(p for p in (ROOT/'chaosbrawl/batches').glob('*/source.json') if p.parent.name <= args.batch)
    for previous in batches:
        document = json.loads(previous.read_text())
        documents.append(document)
        for card in document['cards']:
            previous_card = cards_by_id.get(card['id'])
            if previous_card and previous_card['nativeUid'] != card['nativeUid']:
                raise ValueError('A later batch reassigned a native identity: '+card['id'])
            cards_by_id[card['id']] = card
        pending.update(document.get('pendingCardIds', []))
        pending.difference_update(document.get('resolvedCardIds', []))
        token_bindings.update(document.get('tokenBindings', {}))
    cards = [cards_by_id[cid] for cid in sorted(cards_by_id)]
    for card in cards:
        expected = {k:v for k,v in card.items() if k != 'nativeUid'}
        if live.get(card['id']) != expected:
            raise ValueError('Source no longer matches a reviewed card: '+card['id'])
    cards = [c for c in cards if c['id'] not in pending]
    images = RUNTIME/'rendered'/pinned['sourceSha256']
    if args.render:
        images.mkdir(parents=True, exist_ok=True)
        if not args.mse.is_file():
            raise ValueError('Native MSE missing; pass --mse or render UID-named PNGs into '+str(images))
        def win(path):
            return subprocess.check_output(['wslpath','-w',str(path)], text=True).strip()
        with (images/'mse-render.log').open('wb') as log:
            subprocess.run([str(args.mse), '--export-images', win(source), win(images/'{card.uid}.png')],
                           stdout=log, stderr=subprocess.STDOUT, check=True, timeout=600)
        if (images/'mse-render.log').stat().st_size:
            raise ValueError('MSE reported warnings; inspect the local render log.')
    cards_dir = RUNTIME/'cache/pics/cards/CBT'
    token_dir = RUNTIME/'cache/pics/tokens'
    for folder in [cards_dir, token_dir, RUNTIME/'decks/constructed', RUNTIME/'run', RUNTIME/'cache/skins']:
        folder.mkdir(parents=True, exist_ok=True)
    names = {c['name'] for c in cards}
    filenames = card_image_filenames(cards, ROOT/'forge-gui/res/editions/Chaos Brawl Local Test.txt')
    for card in cards:
        original = images/(card['nativeUid']+'.png')
        if not original.is_file():
            raise ValueError('Missing native full render; use --render: '+card['name'])
        shutil.copyfile(original, cards_dir/filenames[card['id']])
    # Illustrations for source-defined tokens are copied byte-for-byte from the MSE.
    identity = {c['name']:c for c in pinned['supportingTokens']}
    scripts = [('Beer','chaosbrawl_beer'),('Hangry Siren','chaosbrawl_hangry_siren'),
               ('Another Brother','chaosbrawl_another_brother'),('Vengeful Gnome Pack','chaosbrawl_vengeful_gnome_pack'),
               ('Stinky Elemental','chaosbrawl_stinky_elemental'),
               ('Vengeful Gnome Pack','chaosbrawl_vengeful_gnome_pack_haste')]
    scripts += [(n+' Beer','chaosbrawl_'+n.lower()+'_beer') for n in ['Expired','Zwickl','Wheat','Dark','Pils','Bock']]
    by_id = {c['id']:c for c in cards}
    scripts += [(by_id[cid]['name'], script) for cid, script in token_bindings.items() if cid in by_id]
    with zipfile.ZipFile(source) as archive:
        sys.path.insert(0, str(website/'scripts/mse'))
        from archive import card_sections, read_archive
        native = card_sections(read_archive(source)[0])
        for name, script in scripts:
            if not (ROOT/'forge-gui/res/tokenscripts'/(script+'.txt')).exists():
                continue
            member = native[identity[name]['nativeUid']][2]['image']
            (token_dir/(script+'.png')).write_bytes(archive.read(member))
    generated = {cid for document in documents for cid in document.get('generatedOnlyIds', [])}
    decks = []
    active_deck = None
    for number, document in enumerate(documents, 1):
        eligible = [c for c in document['cards'] if c['id'] not in generated
                    and c['id'] not in pending and not c.get('token')]
        if not eligible:
            continue
        active_deck = f'Chaos Brawl Beta Batch {number:02d}'
        decks.append((active_deck, eligible, 1, 10))
        decks += [(f'Chaos Brawl Beta {number:02d} - Group {n//10+1}', eligible[n:n+10], 4, 6)
                  for n in range(0, len(eligible), 10)]
    for name, entries, copies, lands in decks:
        lands = max(lands, math.ceil((60 - copies * len(entries)) / 5))
        lines = ['[metadata]', 'Name='+name, '[Main]']
        lines += [f'{copies} {c["name"]}|CBT' for c in entries]
        lines += [f'{lands} {land}' for land in ['Plains','Island','Swamp','Mountain','Forest']]
        (RUNTIME/'decks/constructed'/f'{name}.dck').write_text('\n'.join(lines)+'\n')
    run = RUNTIME/'run'
    resources = run/'res'
    if not resources.exists():
        resources.symlink_to(ROOT/'forge-gui/res', target_is_directory=True)
    elif not resources.is_symlink() or resources.resolve() != ROOT/'forge-gui/res':
        raise ValueError('Refusing to replace an unrelated resource directory.')
    (run/'forge.profile.properties').write_text(f'userDir={RUNTIME}/user\ncacheDir={RUNTIME}/cache\ndecksDir={RUNTIME}/decks\ndecksConstructedDir={RUNTIME}/decks/constructed\n')
    # Independent variants of official names require this in the owned test profile.
    # Keep any unrelated existing local preferences.
    prefs=RUNTIME/'user/preferences/forge.preferences'
    prefs.parent.mkdir(parents=True,exist_ok=True)
    settings=prefs.read_text().splitlines() if prefs.exists() else []
    settings=[line for line in settings if not line.startswith('ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE=')]
    prefs.write_text('\n'.join(settings+['ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE=true'])+'\n')
    (RUNTIME/'active-deck.txt').write_text(active_deck+'.dck\n')
    print(f'Prepared {len(cards)} reviewed Beta records ({len(names)} names), native full renders and {len(decks)} batch decks.')


if __name__ == '__main__':
    main()
