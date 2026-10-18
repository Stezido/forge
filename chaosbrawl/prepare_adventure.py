#!/usr/bin/env python3
"""Prepare an isolated local Adventure workspace; never packages or publishes a release."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sys
from card_images import card_image_filenames

ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--shared-runtime',type=Path,required=True,help='Reviewed native UID renders and token cache from the migrated engine workspace')
    args=parser.parse_args()
    shared=args.shared_runtime.resolve()
    batch=ROOT/'chaosbrawl/batches/owner-decisions-001'
    source=json.loads((batch/'source.json').read_text())
    cards={}
    for path in sorted((ROOT/'chaosbrawl/batches').glob('*/source.json')):cards.update({c['id']:c for c in json.loads(path.read_text())['cards']})
    ordered=[cards[cid] for cid in sorted(cards)]
    if len(ordered)!=314:raise ValueError('Unexpected reviewed source record count')
    filenames=card_image_filenames(ordered,ROOT/'forge-gui/res/editions/Chaos Brawl Local Test.txt')
    native=shared/'rendered'/source['sourceSha256']
    for card in ordered:
        if not (native/(card['nativeUid']+'.png')).is_file():raise ValueError('Missing reviewed render: '+card['id'])
    base=ROOT/'.chaosbrawl-local/adventure'
    run=base/'run';run.mkdir(parents=True,exist_ok=True)
    resources=run/'res';resources.mkdir(exist_ok=True)
    def link(src,dest):
        if dest.is_symlink():
            if dest.resolve()!=src.resolve():raise ValueError('Unexpected resource link: '+str(dest))
        elif dest.exists():raise ValueError('Refusing to replace existing resource: '+str(dest))
        else:dest.symlink_to(src.resolve(),target_is_directory=src.is_dir())
    for path in (ROOT/'forge-gui/res').iterdir():
        if path.name!='adventure':link(path,resources/path.name)
    adventure=resources/'adventure';adventure.mkdir(exist_ok=True)
    for path in (ROOT/'forge-gui/res/adventure').iterdir():link(path,adventure/path.name)
    plane=adventure/'ChaosBrawl';plane.mkdir(exist_ok=True)
    for path in (ROOT/'forge-gui/res/adventure/Shandalar').iterdir():
        if path.name!='config.json':link(path,plane/path.name)
    config=json.loads((ROOT/'forge-gui/res/adventure/common/config.json').read_text())
    config['allowedEvents']=['CBFRbeta']
    config['eventFormats']=['Draft','Sealed']
    (plane/'config.json').write_text(json.dumps(config,ensure_ascii=False,indent=2)+'\n')
    (run/'forge.profile.properties').write_text(f'userDir={base}/user\ncacheDir={base}/cache\ndecksDir={base}/decks\ndecksConstructedDir={base}/decks/constructed\n')
    prefs=base/'user/preferences';prefs.mkdir(parents=True,exist_ok=True)
    pref=prefs/'forge.preferences'
    if not pref.exists():pref.write_text('AUTO_UPDATE=none\nCHECK_SNAPSHOT_AT_STARTUP=false\nALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE=true\nDECKGEN_CARDBASED=false\n')
    settings=base/'user/adventure';settings.mkdir(parents=True,exist_ok=True)
    settings_file=settings/'settings.json'
    if not settings_file.exists():settings_file.write_text(json.dumps({'plane':'ChaosBrawl','width':1280,'height':720,'videomode':'720p'})+'\n')
    pictures=base/'cache/pics/cards/CBFRbeta';pictures.mkdir(parents=True,exist_ok=True)
    for card in ordered:shutil.copyfile(native/(card['nativeUid']+'.png'),pictures/filenames[card['id']])
    tokens=base/'cache/pics/tokens';tokens.mkdir(parents=True,exist_ok=True)
    for path in (shared/'cache/pics/tokens').glob('*.png'):shutil.copyfile(path,tokens/path.name)
    decks=base/'decks/constructed';decks.mkdir(parents=True,exist_ok=True)
    for path in (shared/'decks/constructed').glob('Chaos Brawl Beta *.dck'):shutil.copyfile(path,decks/path.name)
    print('Prepared isolated Adventure profile:',run)
    print('Plane ChaosBrawl uses Shandalar resources and only CBFRbeta Draft/Sealed events; other planes and user saves unchanged.')
    print('Launch locally with -Dforge.chaosbrawl=true -Dsentry.enabled=false; this command makes no release archive or upload.')

if __name__=='__main__':main()
