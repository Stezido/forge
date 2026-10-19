#!/usr/bin/env python3
"""Align/sign an APK using the stable, private Chaos Brawl release identity."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import subprocess


def run(command, env=None):
    result = subprocess.run([str(x) for x in command], env=env, text=True, capture_output=True)
    if result.returncode:
        # Signing tools can reflect credentials; never emit their output on errors.
        raise RuntimeError(f'{Path(command[0]).name} failed ({result.returncode}); private tool output withheld')
    return result.stdout


def sign(apk, output, sdk, java, signing):
    signing.mkdir(parents=True, exist_ok=True, mode=0o700)
    signing.chmod(0o700)
    key, password = signing/'release.p12', signing/'password'
    if key.exists() != password.exists():
        raise ValueError('Incomplete signing identity: restore the matching key/password backup')
    if not key.exists():
        fd = os.open(password, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        with os.fdopen(fd, 'w') as file:
            file.write(secrets.token_urlsafe(48))
        env = {**os.environ, 'CHAOSBRAWL_ANDROID_PASSWORD': password.read_text()}
        run([java/'bin/keytool', '-genkeypair', '-keystore', key, '-storetype', 'PKCS12', '-alias', 'chaosbrawl-release', '-keyalg', 'RSA', '-keysize', '4096', '-validity', '10000', '-dname', 'CN=Chaos Brawl Forge, OU=Private Playtest', '-storepass:env', 'CHAOSBRAWL_ANDROID_PASSWORD', '-keypass:env', 'CHAOSBRAWL_ANDROID_PASSWORD'], env)
    key.chmod(0o600)
    password.chmod(0o600)
    env = {**os.environ, 'JAVA_HOME': str(java), 'PATH': str(java/'bin')+os.pathsep+os.environ.get('PATH', ''), 'CHAOSBRAWL_ANDROID_PASSWORD': password.read_text()}
    tools = sdk/'build-tools/35.0.0'
    output.parent.mkdir(parents=True, exist_ok=True)
    aligned = output.with_suffix('.aligned.apk')
    if output.exists() or aligned.exists():
        raise ValueError('Refusing to overwrite existing APK')
    run([tools/'zipalign', '-P', '16', '-f', '4', apk, aligned], env)
    run([tools/'apksigner', 'sign', '--ks', key, '--ks-key-alias', 'chaosbrawl-release', '--ks-pass', 'env:CHAOSBRAWL_ANDROID_PASSWORD', '--key-pass', 'env:CHAOSBRAWL_ANDROID_PASSWORD', '--out', output, aligned], env)
    report = run([tools/'apksigner', 'verify', '--verbose', '--print-certs', output], env)
    badge = run([tools/'aapt', 'dump', 'badging', output], env)
    if "package: name='forge.app.chaosbrawl' versionCode='10005' versionName='0.1.0-beta.5'" not in badge or "sdkVersion:'26'" not in badge or 'Android Debug' in report:
        raise ValueError('APK identity, minimum API or certificate mismatch')
    cert = re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)', report)
    if not cert or 'Verified using v2 scheme (APK Signature Scheme v2): true' not in report:
        raise ValueError('APK signature not verified')
    run([tools/'zipalign', '-c', '-P', '16', '4', output], env)
    aligned.unlink()
    return {'filename': output.name, 'bytes': output.stat().st_size, 'sha256': hashlib.file_digest(output.open('rb'), 'sha256').hexdigest(), 'certificateSha256': cert.group(1), 'applicationId': 'forge.app.chaosbrawl', 'minSdk': 26, 'versionCode': 10005, 'version': '0.1.0-beta.5'}


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--apk', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--sdk', type=Path, required=True)
    p.add_argument('--java', type=Path, required=True)
    p.add_argument('--signing', type=Path, default=Path.home()/'.config/chaosbrawl/android-signing')
    args = p.parse_args()
    result = sign(args.apk, args.output, args.sdk, args.java, args.signing)
    args.output.with_suffix('.json').write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps(result))
