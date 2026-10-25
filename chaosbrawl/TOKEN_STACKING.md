# Automatic token stacks — website issue #121

Source change for Desktop and Android/mobile; no new downloadable release.

## Behavior

Settings → token stack threshold defaults to 3. Choose 0 to disable, or
2/3/4/5/8/10/20/50/100. This is independent of the existing non-token permanent
stacking settings. Changes take effect on the next battlefield refresh.

At the threshold a compatible group occupies one card with an ×N badge.
Below the threshold tokens have separate card positions. The engine still owns
N separate permanents, with their original IDs, abilities, triggers and targets.
Desktop uses the existing count badge renderer. Mobile uses a high-contrast
18-point badge with a 32-unit touch height.

- Desktop: click a card for one token; click its badge to choose a specific token
  (the chooser also permits inspection); Shift-click for the stack. Existing
  right-click count actions remain available.
- Mobile: tap the card for one token, tap the badge to inspect/choose a specific
  token, or flick for the existing multi-card stack selection. Long press retains
  the zoom carousel, including every hidden token.
- Actions are sent to the existing game controller, which checks current legality.
  A stack never performs a bulk engine mutation.

Card updates schedule a regroup, including changes to counters, damage, temporary
abilities and control, not just changes to tapped state or battlefield membership.
Updates are coalesced to avoid rebuilding for every token in a batch.

## Safety policy

The shared matcher compares all card view properties, plus current-face properties.
Only current-state identity is removed: its contents and face enum are compared
instead. CardType and ManaCost require value comparisons because they have identity
equality. Alternate states retain identity comparison, conservatively preventing
many complex tokens from collapsing. Unknown new view properties default to
participating in equality. Different images, owners/controllers, counters, colors,
types, keywords, power/toughness, damage, sickness, chosen values, and displayed
ability/effect text cannot share a stack.

Face-down tokens, tokens with attachments or prepared/exile ghosts, and combatants
are never collapsed. Attachment/ghost layouts remain independent of stack counts.
Rules-only engine state is never removed or combined by the visual layer.

## Validation

`mvn -pl forge-gui-desktop,forge-gui-mobile -am -Dtest=TokenStacksTest
-Dsurefire.failIfNoSpecifiedTests=false package` builds both clients and runs the
five targeted tests (5 passed, 0 failures); both client packages succeeded. The tests cover independently created real engine cards,
separate IDs, activated abilities/keywords, tap changes, counter/state changes,
attachments/chosen values, threshold boundaries/disable, and 1,000 token views.

Measured grouping passes: final run 15.966 ms; earlier run below.

Initial measured grouping pass: 1,000 real token views, 999 compatibility checks,
22.249 ms on this WSL host, excluding card creation, painting and GPU work. At the
default threshold the eligible group has one visible card rather than 1,000.
This is a grouping microbenchmark, not a measured frame-rate improvement.

Limitations: both clients still allocate and maintain every card panel; Desktop
still positions all panels and Mobile keeps all cards in the zoom carousel.
Mixed non-equivalent populations retain the existing search-based grouping cost.
No physical Android frame-rate, native Windows interaction, completed-game,
or touch-device usability result is claimed. Those checks remain manual:
create/remove 2/3/8/1,000 Beer/Food/creature tokens; change a token's counters,
attachments, temporary abilities, control, sickness, tapped and combat state;
inspect and act on specific IDs; verify disable and screen-size readability.
