## 2024-05-23 - Accessibility contentDescriptions in Jetpack Compose
**Learning:** Setting `contentDescription = null` on an `Icon` in Jetpack Compose means the icon won't be read out directly. However, for interactive buttons (like `TvIconButton` or custom components based on `Button`), this means there's no semantic label if it only contains the icon!
**Action:** When adding informative icons to clickable containers, ALWAYS set a `contentDescription` referencing a localized string in `strings.xml`. Ensure `contentDescription = null` is only used for purely decorative icons.

## 2026-09-06 - Replacing custom Empty States with TvEmptyState
**Learning:** Found custom empty states implementations across the app, such as in `DownloadsScreen.kt`. Using custom empty states often skips critical built-in accessibility features (like `liveRegion = LiveRegionMode.Polite`) and leads to design inconsistencies.
**Action:** Replaced `DownloadsEmptyState` with the standard `TvEmptyState` component. Moving forward, prioritize standard empty state components like `TvEmptyState` or `AppMessageState` which enforce cohesive design and accessibility semantics automatically.

## 2024-10-24 - Avoiding Pseudo-Icons for Accessibility
**Learning:** Found usage of hardcoded text strings (like `Text("II")`) acting as icons (for Pause buttons) within playback controls. This breaks accessibility as screen readers announce the literal string (e.g. "I, I") rather than a meaningful action, and it fails to visually match standard design system iconography.
**Action:** Replaced text-based pseudo-icons with proper semantic `Icon` components (e.g., `Icons.Default.Pause`) combined with localized accessibility strings (e.g., `contentDescription = stringResource(R.string.player_pause)`). Always use explicit standard icons rather than text approximations for UI controls.
