# Changelog

## 4.3.3
- Added `Task Master Duo`, a second shared-body task mode where two groups can swap inside separate bodies at the same time.
- Added GUI/config/command support for selecting `Task Master Duo`, including its own stored default interval.
- Task competitions can now end cleanly with a task winner without showing runner-vs-hunter win/lose titles to the wrong body.
- Fixed second-body listener enforcement so inactive `Task Master Duo` players follow the same chat, movement, inventory, and damage restrictions as inactive runners.
- Kept Dream-only tracker compass rules scoped to Dream so `Task Master Duo` players are not treated like hunters with compasses.
- Updated voice chat mute handling so the second body in `Task Master Duo` is muted/unmuted correctly during swaps.

## 4.3.2
- Rolled the post-`4.3` task competition fixes into the release notes:
  - Fixed task-mode respawn handling so `Task Race` no longer leaks shared runner spawns across players.
  - Wired `task_manager.max_game_duration` into the actual round lifecycle so configured time limits now end the game.
  - Corrected task-mode disconnect pausing so it waits for the missing runners instead of auto-resuming too early.
  - Scoped Task Master start/end cleanup to actual participants, preventing multiverse/non-participant inventory wipes and lobby teleports.
  - Fixed Task Master reconnect handling so returning players no longer get stuck in the bedrock box.
  - Added clearer pre-round task difficulty control in the GUI/config flow.
  - Added player task rerolls with GUI + command support.
  - Fixed late-join task assignment so it no longer wipes existing task assignments.
- Added optional shared hunter control for Dream mode:
  - Hunters can now share a second body while runners keep using the main shared runner body.
  - Runner and hunter swap queues can operate independently, enabling true team-vs-team body swapping.
  - Added `swap.shared_hunter_control.{enabled,interval}` to config and surfaced the feature in the Swap Settings GUI.
  - `/swap help` and `/swap status` now report the shared hunter body state and active hunter timing.
- Finished the Dream-mode integration sweep for the new hunter body flow:
  - Tracker compasses now stay scoped to the active hunter when shared hunter control is enabled.
  - Rejoin, respawn, and world-change paths no longer hand tracking compasses to inactive hunters.
  - Inactive-hunter freeze/cage restrictions now match the existing inactive-runner behavior.
  - Voice-chat muting now respects both shared runners and shared hunters instead of only the runner side.
- Docs and release polish:
  - README and plugin metadata now describe the shared hunter body option alongside Task Master and Task Race.
  - Version bumped to `4.3.2`.

## 4.3
- Added a brand-new `Task Race` mode for 2+ simultaneous runners:
  - Runners keep their own bodies, inventories, and world state instead of sharing one swapped character.
  - The round now supports secret-task racing without periodic swaps or inactive-runner lockouts.
  - Existing swap-based `Task Master` mode remains available unchanged for sabotage-style shared-body play.
- Updated task competition support across the plugin:
  - Task detection and `/swap complete` now work in both task competition modes.
  - Task disconnect handling, late joiner support, and `end_when_one_left` logic now cover both task variants.
  - Voice chat muting and inactive-runner restrictions no longer incorrectly affect the no-swap mode.
- Improved the GUI and commands for production use:
  - Added `Task Race` to the mode selector and task hub.
  - The Task hub now makes you explicitly choose between swap-based `Task Master` and no-swap `Task Race`.
  - Main control screens now avoid offering misleading swap-only controls while `Task Race` is active.
  - Added `/swap mode taskrace` aliases and updated status/help output accordingly.
- Docs and release polish:
  - README and config comments now document the fourth gameplay mode.
  - Helper build/package scripts were updated for the 4.3 release.

## 4.1
- Finished the multiworld polish pass:
  - Added real assignment enforcement for session-world rules instead of only GUI filtering.
  - `/swap status` now reports session world and multiworld assignment settings.
  - Team management and Task runner management now show assignment-world context in the GUI.
  - Batch `/swap setrunners` and `/swap sethunters` now skip cross-world players when session-world restrictions are enabled and explain why.
  - The first valid runner assignment now anchors the session world when one has not been established yet.

## 4.0.9
- Added multiworld / Multiverse compatibility controls:
  - Runner respawns now prefer the active session world instead of falling back to the server's first world.
  - Optional post-respawn enforcement helps win conflicts when another plugin applies a second teleport after respawn.
  - Added a new Multiworld settings GUI with toggles for compatibility, respawn enforcement, session-world updates, and same-world team filtering.
  - Team selection menus can now focus on the admin's current world while still showing already-assigned off-world players for cleanup.
- Added `Multiverse-Core` and `Multiverse-Inventories` as soft dependencies.

## 4.0.8
- Fixed reconnect reliability in live games (1.21+):
  - Team membership checks are now UUID-safe, preventing stale player-reference issues after disconnect/rejoin.
  - Rejoined players are re-synchronized into runner/hunter lists from config assignments.
  - Auto-resume now only triggers when the game was paused due to disconnects and required teams are online.
  - Queue coherence now rebinds the active runner to the current online player instance.

## 3.2.0
- Settings GUI
  - Added Experimental Intervals toggle (allow <30s and >max with red beta warnings)
  - Added Reset Interval button (restores this mode’s default)
  - Added Save as Mode Default (sets current interval as this mode’s default)
  - Added Apply Mode Default on Switch toggle (auto-apply on Dream/Sapnap switch when game not running)
  - Added ±5s buttons next to Swap Interval in Settings
  - Added extra warning when interval <15s (may impact performance)
- Interval policy
  - setSwapInterval now clamps based on experimental toggle
  - Unified max interval accessor and defaults (swap.max_interval default is 90)
- Per-mode defaults
  - Config keys swap.default_intervals.{dream|sapnap}
  - Optional auto-apply on mode change
- Docs
  - README updated; new config options documented
- Build
  - Version bumped to 3.2.0
