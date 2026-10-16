# Beta migration

Canonical source: the registered development MSE in `Stezido/Chaosbrawl-Website`. The website catalogue remains published Alpha; this migration explicitly reads Beta.

| Batch | Range | Status |
| --- | --- | --- |
| 01 | cb-001–cb-050 | 50 definitions written; behavior verification in progress |
| 02 | cb-051–cb-100 | Pending |
| 03 | cb-101–cb-150 | Pending |
| 04 | cb-151–cb-200 | Pending |
| 05 | cb-201–cb-250 | Pending |
| 06 | cb-251–cb-300 | Pending |
| 07 | cb-301–cb-311 | Pending |

Supporting tokens are implemented with their first dependent card, without consuming the batch's normal-card slots. Final batch accounting will distinguish token printings from playable cards; native UIDs/stable IDs are never reassigned.

The first ten-card baseline passed its 12 original integration tests before migration. The expanded suite verifies all 50 cards against the reviewed MSE metadata, plus real game effects, payments and timing. Latest completed expanded run: 84 of 85 checks passed; the Overdose end-step case is being corrected/verified. Full native MSE rendering completed successfully for all 311 source records without warnings. Desktop packaging and a completed AI smoke game are still pending.

Owner decisions for batch 01:

- Celestial Waters moves counters before untapping and before Drunken-to-stun conversion. It is a special turn action, not a normal upkeep stack trigger. The isolated engine test exercises that exact order.
- Buy a Round costs {1}{W}; Nutshot overload puts two stun counters.
- Brie-lliant Trapper is a legendary Horror Food creature without Artifact. Scavenger does not gain Food.
- Chaotic Kitchen makes one Food token and grants {2}, {T}, sacrifice for 3 life.

Food Coma and Inspiring Bard share names and mechanics with upstream cards. Forge uses their existing definitions; the metadata comparison accounts for the standard Food reminder and the source's extra words in Inspiring Bard's trigger. Their Beta full-card images still come from the canonical MSE. The copied Explosive Singularity ability on Feuerball, Junge! uses CARDNAME as its damage source, preserving the intended self-reference without rewriting the MSE Oracle field.

The private repository begins with a complete source snapshot at `aa1ec33c`, corresponding to public fork source `7f16ea60` and upstream `34a8662b`. This avoids importing the entire upstream history from the earlier shallow checkout. Original license/notices and the public fork/PR remain intact. New migration work is private; GitHub Actions is disabled in this private repository and validation runs locally.
