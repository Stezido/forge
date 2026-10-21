# Chaos Brawl fork branches

The canonical Chaos Brawl integration branch is `main-custom` in the private repository `Stezido/Chaosbrawl-Forge`. Start new custom feature branches from current `private/main-custom` and target their review PRs to `main-custom`. Use the verified private remote; some checkouts retain a legacy `origin` pointing to `Stezido/forge`.

Preserve `main` as the upstream-sync baseline. Here upstream means the base project `Card-Forge/forge`, not this private fork. Updating upstream and merging those updates into `main-custom` are separate operations; test custom compatibility before integration. The baseline includes the fork's initial import and is not automatically the latest upstream revision.

`main-custom` supersedes `chaosbrawl/windows-playtest-001` as the current custom integration target. Keep historical feature, release and stacked review branches/PRs intact. Release tags and published binary/source assets remain immutable; integration does not authorize another downloadable release.

Read `/home/stefan/.codex/RTK.md` and prefix shell commands with `rtk`. Follow `chaosbrawl/CARD_FEEDBACK.md` for paired MSE/source, executable rules, native images and AI checks. Preserve existing profiles, decks, installations and unrelated dirty work. Keep private Forge Actions disabled; run checks locally in isolated worktrees.

# Chaos Brawl Forge downloadable releases

For card feedback, follow `chaosbrawl/CARD_FEEDBACK.md` and the companion website `docs/card-feedback-mse-forge.md`. Check the authoritative development MSE source and adapt/test Forge together; direct owner chat is valid feedback scope. Preserve printed rules when fixing AI or engine behavior. New downloadable releases still require the explicit request described below.

Commit, push and merge approved card, image, metadata, packaging and engine changes normally after appropriate checks. Create, upload or publish a new downloadable release only when the owner explicitly requests a new downloadable release. A milestone, merge, individual fix, set-code change or website deployment is not implicit release authorization. Accumulate changes in the repository and reuse existing binary/source assets until explicitly told to make a new download release. Do not add recurring approval questions; simply omit the unrequested release step. Suggestions for a release are welcome for substantial batches or milestones; small changes remain commits/merges and do not warrant a new release. This supersedes earlier milestone-based wording.

The most recent owner-requested publication is coordinated Windows/Android 0.1.0-beta.4. Beta uses CBFRbeta; CBT remains a compatibility alias and CBFR is reserved for the full released set. Check current release manifests/history for later versions; a historical release request never authorizes another release. Preserve edition metadata, cache filenames, decks and verification consistently.

Keep existing branches/PRs, upstream behavior and user profiles/decks. Use the Windows package's actual Forge image lookup audit before creating its ZIP. Do not enable private Forge Actions or billable infrastructure.
