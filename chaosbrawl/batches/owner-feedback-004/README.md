# Debt Collector: Treasure recipient

Owner-reported engine bug on cb-146, native UID
19677003063545642001668771848106. Checked the registered Beta archive
on website main commit b17d1134efba6ea80177b6625246ff45cccb95b0,
SHA-256 a661450cec9bad0acdd1c878e57ebcff67a2d726eca6e5df0b432216c7b08a15.

The authoritative rule already says: “When Debt Collector enters the battlefield,
create a Treasure token for each creature target opponent controls.” Thus the
ability controller creates the tokens; the opponent is targeted solely to
determine the number. Native CARDNAME automation and Limited-Rating: 74
remain correct. MSE, printed rules, art, set eligibility and ratings are unchanged.

Forge TokenEffect defaults to its target players when TokenOwner is absent.
Specify TokenOwner$ You on this card's existing token effect, preserving its
opponent target and resolution-time creature count. No global engine or
upstream card behavior changes. No new downloadable release is authorized;
published beta.5 and the owner's playtest installation remain unchanged.
