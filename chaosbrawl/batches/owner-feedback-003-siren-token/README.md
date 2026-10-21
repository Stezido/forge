# Issue #84: Nagging Siren Twin token face

Adds native token cb-315 to the reviewed source: blue 1/1 Siren with Flying, named Nagging Siren Twin. Its executable token already existed and remains unchanged; preparation now binds its script to the approved native full-card render. All 315 source faces were rendered through native MSE without warnings. Seven existing token records also gain their explicit native colors from the corrected website importer; no printed rules or executable behavior change.

Same-name tokens use a distinct card-cache filename, preserving the parent’s existing `Nagging Siren Twin.full.png` image key. Runtime token lookup uses `chaosbrawl_nagging_siren_twin.png`. The package image auditor now expects 18 bundled token faces.

Source archive hash and website commit are pinned in source.json; older batches remain intact. The existing real-engine test additionally verifies the token’s blue color, Siren subtype and 1 toughness, alongside Flying and the absence of an ETB loop. No new APK/ZIP is published by this change.
