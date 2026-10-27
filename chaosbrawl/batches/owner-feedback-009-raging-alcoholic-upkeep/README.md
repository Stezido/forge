# Raging Alcoholic upkeep correction

Source: the registered website Beta source remains unchanged at commit `effc00ffc91f470e38ebfbee9e4dd85fb0c4f1e9`, archive SHA-256 `bb38863fab8371fec03be2197e3f18de47a3f8f7e4e2a6b77890a8c71faa364c`. The card rule was already correct; its executable upkeep ability omitted the fallback sacrifice when the Beer cost could not be paid.

Affected card: cb-089 Raging Alcoholic (native UID `87416760907504197865981889081810`). Forge now sacrifices a Beer token as the upkeep cost, or sacrifices Raging Alcoholic when no Beer can be sacrificed.

Regression: `ragingAlcoholicRequiresBeerAtUpkeepOrIsSacrificed` checks both lack of Beer and successful Beer payment using the real custom printing and upkeep phase.

Observed validation: `bash chaosbrawl/forge.sh test` passed all 505 `ChaosBrawlTest` cases.
