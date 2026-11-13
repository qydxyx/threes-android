# Changelog

## v1.2.0 (versionCode 7)

### Fixes & Physics
- Refactored drag gesture handling in `GameBoardView` to use `awaitEachGesture`, `awaitTouchSlopOrCancellation`, `drag`, and `VelocityTracker` for accurate slop tracking and velocity flick commits (`>= 700 px/s` with `>= 10%` offset).
- Fixed inner board cell pixel measurement by moving `BoxWithConstraints` calculation inside the `16.dp + 8.dp` padded playfield.
- Flattened Layer 2 overlay with `key(tile.id)` and `zIndex` ordering so sliding cards never rebuild mid-move or lose occlusion order.
- Added undo snapshot persistence (`previousSnapshot`) in `PreferencesRepository`, `Tile.syncMaxId` synchronization across snapshot tiles, and animation flag cleanup on save/undo.
- Fixed `ThreesViewModel.resetHighScore()` to clear `highScore`, `baselineHighScore`, and `hasBrokenRecordInSession`, and staggered post-merge game-over/new-record audio cleanly.
- Refactored `SoundEngine` to `SoundPool` backed by atomically cached 44.1kHz mono WAV assets with click-free envelopes and full 14-character voice support.

### UI, Accessibility & Responsiveness
- Enabled `enableEdgeToEdge()`, safe drawing insets (`safeDrawingPadding()`), and adaptive landscape/tablet two-pane layout; unlocked screen orientation in `AndroidManifest.xml`.
- Added incoming edge slide-in animations, font-scale normalized tile typography (`BoxWithConstraints` + `fontScale`), proportional `CharacterFaces` strokes, and complete blink + gaze animations.
- Localized all UI strings with English (`values/strings.xml`) and Simplified Chinese (`values-zh-rCN/strings.xml`), added Night mode card tokens, scroll-safe dialogs, reset high score confirmation, and `CastBookDialog` tap-bounce debouncing.

### Build, Signing & Tests
- Configured R8 minification + resource shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`) with clean `proguard-rules.pro`.
- Added conditional release `signingConfigs` support via environment variables or root `keystore.properties`, plus GitHub Actions CI & signed Release workflows.
- Expanded `GameEngineTest` and added `ThreesViewModelTest` covering score reset, undo/high-score reversion, game-over persistence, and bonus card hints.
