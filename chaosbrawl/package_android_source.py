#!/usr/bin/env python3
"""Archive exact tracked source plus the generated resource inputs used in the APK."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent.parent


def package(output):
    subprocess.run(['git', 'diff', '--exit-code', 'HEAD', '--'], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    if output.exists():
        raise ValueError('Refusing to overwrite source archive')
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as temporary:
        archive = Path(temporary)/'source.zip'
        subprocess.run(['git', 'archive', '--format=zip', '--output', str(archive), 'HEAD'], cwd=ROOT, check=True)
        with zipfile.ZipFile(archive) as source, zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as target:
            for info in source.infolist():
                target.writestr(info, source.read(info))
            for name in ['chaosbrawl-resources.zip', 'chaosbrawl-resources.sha256']:
                path = ROOT/'forge-gui-android/assets'/name
                target.write(path, 'forge-gui-android/assets/'+name)
            target.writestr('ANDROID-SOURCE.txt', f'''Corresponding source for Chaos Brawl Forge Android 0.1.0-beta.1
Engine/source commit: {commit}
Forge is GPLv3-or-later; preserve LICENSE and contributor/dependency notices.
Build instructions: chaosbrawl/ANDROID_RELEASE.md
The exact generated resource inputs used in the APK are included in
forge-gui-android/assets. No signing keys or passwords are included.
Build your own unsigned APK, then sign with your own key. Your signature cannot
update the owner's installed package; use a separate application ID or uninstall
after backing up your own saves. This archive includes no user save records.
''')
    return {'engineCommit': commit, 'filename': output.name, 'bytes': output.stat().st_size, 'sha256': hashlib.file_digest(output.open('rb'), 'sha256').hexdigest()}


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    result = package(args.output)
    args.output.with_suffix('.json').write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps(result))
