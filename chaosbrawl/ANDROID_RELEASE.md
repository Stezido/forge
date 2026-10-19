# Chaos Brawl Forge Android beta.1

Signed APK distribution uses the existing private GitHub → approved-playtester website handoff, with exact matching GPL source. No store, public repository change or paid Actions.

Application ID `forge.app.chaosbrawl`, version `0.1.0-beta.1`, versionCode `10001`. Android8/API26 minimum; target SDK35. Android11+ and 6GB RAM recommended. Includes arm64-v8a, armeabi-v7a, x86 and x86_64 native libraries. The owner’s physical target is Android15 RedMagic10Pro; that phone has not been connected/tested here.

## Resources and storage

`package_android_resources.py` consumes the checksum-verified Windows beta.3 ZIP and manifest. It packages 55863 resource source files, 34369 card scripts in Forge’s native cardsfolder.zip format, 314 custom images, 17 token images and 41 seed decks. The reviewed CBFRbeta engine/resources and ChaosBrawl Adventure/limited settings are preserved.

Launcher verifies/extracts the bundle on a worker thread before starting Main. The serialized installer validates digest and paths, replaces resources per file, seeds only missing user files and writes completion last. App data/cache remain in the separate sandbox. No broad media/storage permission or upstream self-installer provider. Custom builds skip upstream updates; future APKs are installed through the browser with the same signing identity. Uninstalling removes app-owned saves.

## Reproduce and sign

- JDK21, Maven3.9.11, Android SDK platform35/build-tools35.0.0.
- Install the upstream Android plugin JAR/POM from https://github.com/Card-Forge/android-maven-plugin/releases/tag/4.6.2, following Forge’s Android workflows. JAR SHA `a14059c05dbb984ecef3e992fd67df1b65ddc07cd3e1dc09527a163c00dde415`; POM SHA `38e2d3c4f5cb2412f15e0a832398c49a6ac6b856a3fc7a4608d08a0409967330`.
- `python3 chaosbrawl/package_android_resources.py --desktop-zip <verified-beta3.zip> --manifest <beta3-release-manifest.json>`.
- `mvn -B -pl forge-gui-android -am -Pchaosbrawl-android -DskipTests -Dandroid.manifest.versionCodeUpdateFromVersion=false package`.
- `python3 chaosbrawl/sign_android.py --apk <unsigned.apk> --output <release.apk> --sdk <sdk-root> --java <jdk-root>`.
- The stable private identity is outside Git at `~/.config/chaosbrawl/android-signing`, directory0700/files0600. Passwords use environment references rather than command arguments/logs. Preserve an encrypted owner backup to retain in-place updates. Do not include the signing directory in artifacts.
- `python3 chaosbrawl/package_android_source.py --output <Android-Source.zip>` archives a clean tracked commit plus the exact resource ZIP/digest used by the APK. SDK/JDK/Maven remain external prerequisites. Recipients sign their own builds with their own key.

## Verified release

Exact pins, certificate, counts and test matrix: [android-verification.json](releases/android-verification.json). Source/engine commit `4280a7309e097bee3b0ba2b84e2bf59f04c03e0d`. The APK is 358030858 bytes; source ZIP613973635 bytes. Their GitHub asset IDs are654005440 and654005441. The public signing certificate remains stable.

- Full seven-module compile/package; APK identity/API26, v2 signature and 16-KiB ZIP alignment pass.
- Resource digest/traversal, seed persistence, resource upgrade and restart checks pass.
- 466 custom engine and seven limited regressions pass.
- Compressed mobile bundle: actual shared-engine image lookup resolves292 printings, zero missing.
- Android8/API26 x86 and Android15/API35 x86_64 software emulators install/unpack with41 decks and enter native card loading. Screenshots: [API26](releases/android-api26-card-loading.png), [API35](releases/android-api35-card-loading.png). This is startup evidence, not a completed Android game or performance benchmark.
- APK and matching-source provider pins and anonymous credential-free CDN ranges pass via the website handoff implementation.

## Remaining acceptance (#57)

Physical RedMagic installation/update, touch layouts, save/deck persistence, complete human-versus-AI matches, Adventure/Sealed/Draft play and older-phone performance remain manual tester checks. Keep #57 open until the required real-device evidence exists. Engine/source/build distribution is GPLv3-or-later with retained notices; artwork rights are separate.


## Pending playtester fixes (source only)

The next explicitly requested download must first rebuild the reviewed desktop resource package from the accumulated fixes, then feed its checksum-verified manifest/ZIP into package_android_resources.py. Do not reuse beta.3 resources for a new APK: they contain the old Bottle Bash and Temptress scripts, old booster sheets and unframed token illustrations. The bundler accepts subsequent checksum-verified 0.1.0 Beta versions with CBFRbeta. Keep Android minSdk26 and the stable signing identity/version-code progression.

Mobile/Android compilation passes with constant-time identical-token stack appends. This removes repeated full-chain traversal during field rebuilds; performance on the RedMagic 10 Pro and older physical devices still needs measurement. No replacement APK/Windows ZIP is published by this source-only work.


## Owner-requested 0.1.0-beta.4

The owner requested a coordinated new release after the card, token, limited and Obsession AI fixes. Android versionName is0.1.0-beta.4 and versionCode10004, retaining forge.app.chaosbrawl, minSdk26 and the existing release certificate. Rebuild resources from the new reviewed Windows beta.4 package, not beta.3. Matching source and signed APK publication are being prepared; the beta.1 section above remains historical evidence.


Beta.4 publication completed for APK/source at forge-android-v0.1.0-beta.4. Exact hashes, asset IDs and upgrade observations are recorded in releases/android-beta4-verification.json. Both API26/API35 emulators installed over beta.1, updated to the exact new resource digest and retained a synthetic player deck; Expired/Bock token bytes match the corrected native full-card renders. API35 native card loading was observed. This is not a physical-device performance or completed-game acceptance claim.
