# Changelog

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
