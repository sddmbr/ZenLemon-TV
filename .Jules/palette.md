## 2024-05-23 - Accessibility contentDescriptions in Jetpack Compose
**Learning:** Setting `contentDescription = null` on an `Icon` in Jetpack Compose means the icon won't be read out directly. However, for interactive buttons (like `TvIconButton` or custom components based on `Button`), this means there's no semantic label if it only contains the icon!
**Action:** When adding informative icons to clickable containers, ALWAYS set a `contentDescription` referencing a localized string in `strings.xml`. Ensure `contentDescription = null` is only used for purely decorative icons.

## 2026-09-06 - Replacing custom Empty States with TvEmptyState
**Learning:** Found custom empty states implementations across the app, such as in `DownloadsScreen.kt`. Using custom empty states often skips critical built-in accessibility features (like `liveRegion = LiveRegionMode.Polite`) and leads to design inconsistencies.
**Action:** Replaced `DownloadsEmptyState` with the standard `TvEmptyState` component. Moving forward, prioritize standard empty state components like `TvEmptyState` or `AppMessageState` which enforce cohesive design and accessibility semantics automatically.

## 2024-11-20 - Replace hardcoded pseudo-icons with vector Icons
**Learning:** Found instances where hardcoded text strings (like "II" for Pause) were used instead of proper icons within button layouts, severely hurting accessibility and screen reader output.
**Action:** Replaced text-based pseudo-icons with Compose `Icon` components using proper `imageVector` (e.g., `Icons.Default.Pause`) and accurate `contentDescription` via `stringResource(R.string.player_pause)`.
