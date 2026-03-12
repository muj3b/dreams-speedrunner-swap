# Changelog

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
