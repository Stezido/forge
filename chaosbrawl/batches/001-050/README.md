# Batch 01 — cb-001 to cb-050

`source.json` is a reviewed metadata snapshot derived directly from the registered Beta MSE. It records the website commit, archive hash and native UIDs. The MSE in the website repository remains authoritative. A changed source hash prevents preparation until the differences have been reviewed and migrated.

The executable definitions are `forge-gui/res/cardsfolder/custom/chaosbrawl_cb-NNN.txt`. Supporting Beer variants and the flying Nagging Siren Twin are in `forge-gui/res/tokenscripts/`. The CBT edition lists exactly these 50 normal cards. Source labels such as Kegbearer's old longer name and the Feuerball damage-source name are implemented as self-references; no unrelated source card is dealt damage.

Celestial Waters uses the owner-confirmed special order: choose/move counters, then check Drunken tolerance, then perform ordinary untapping. Obsession adds an attack requirement without choosing a defender or waiving attack costs; its attack event creates a normal queued counter-removal trigger. Food is retained when explicitly printed on a creature, without adding Food to the global list granted by changeling.

After verification, `scripts.json` pins the executable files used by `chaosbrawl/prepare.py`. Preparation uses native full-card MSE renders rather than treating illustration-only assets as full cards. Runtime profiles, caches, decks and rendered images stay under ignored `.chaosbrawl-local`.
