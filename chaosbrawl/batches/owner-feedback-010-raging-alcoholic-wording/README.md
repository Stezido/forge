# Raging Alcoholic printed wording

Website source: [PR #142](https://github.com/Stezido/Chaosbrawl-Website/pull/142), commit `c5e7eff61c0064eca9562cf9cb5fac05abd94907`, registered Beta MSE SHA-256 `7027b01ed00fec137aa33f8c46fc2134c701b5d8011f9620e635c44c8e4fdce6`. The source snapshot records only cb-089 with its existing native UID.

Forge's printed Oracle text now matches the website card: `Upkeep — sacrifice a beer`. Its executable upkeep ability is unchanged: sacrificing a Beer keeps Raging Alcoholic; otherwise Raging Alcoholic is sacrificed. Attacking still creates two Beer tokens.

The existing real-engine regression covers payment and fallback sacrifice. `bash chaosbrawl/forge.sh test` passed all 505 `ChaosBrawlTest` cases on this branch. No downloadable release is part of this change.
