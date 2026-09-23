## 2024-05-23 - Accessibility contentDescriptions in Jetpack Compose
**Learning:** Setting `contentDescription = null` on an `Icon` in Jetpack Compose means the icon won't be read out directly. However, for interactive buttons (like `TvIconButton` or custom components based on `Button`), this means there's no semantic label if it only contains the icon!
**Action:** When adding informative icons to clickable containers, ALWAYS set a `contentDescription` referencing a localized string in `strings.xml`. Ensure `contentDescription = null` is only used for purely decorative icons.

## 2026-09-06 - Replacing custom Empty States with TvEmptyState
**Learning:** Found custom empty states implementations across the app, such as in `DownloadsScreen.kt`. Using custom empty states often skips critical built-in accessibility features (like `liveRegion = LiveRegionMode.Polite`) and leads to design inconsistencies.
**Action:** Replaced `DownloadsEmptyState` with the standard `TvEmptyState` component. Moving forward, prioritize standard empty state components like `TvEmptyState` or `AppMessageState` which enforce cohesive design and accessibility semantics automatically.

## 2024-05-28 - Replaced hardcoded text pseudo-icons with semantic Icons
**Learning:** Hardcoding string characters like 'II' for pause buttons does not provide appropriate semantic meaning for screen readers. Using these text hacks skips essential accessibility affordances and creates an inconsistent visual experience compared to native UI elements.
**Action:** Replaced instances of 'II' text in the Player Controls with properly localized `Icons.Default.Pause` along with appropriate `contentDescription`. Ensure all core actions use standard material icons instead of text shapes.

## 2024-05-18 - Avoid pseudo-icons
**Learning:** Found instances where plain text like "II" were being used in Compose as a pause button icon, which provides zero accessibility semantics (no contentDescription can be attached easily to just Text serving as icon role without overriding semantics entirely) and looks visually inconsistent.
**Action:** Replaced Text("II") pseudo-icon with a standard Compose `Icon` from `Icons.Default.Pause`, providing `contentDescription = stringResource(R.string.player_pause)` to ensure screen readers narrate the button's action rather than reading literal characters.
## 2024-06-25 - Replace transport button text icons with standard material icons
**Learning:** Found instances where plain text like "\u23EA" and "\u23E9" were being used in Compose as rewind and fast forward button icons, which provides poor visual consistency and is less semantic than standard icons.
**Action:** Replaced Text("\u23EA") and Text("\u23E9") pseudo-icons with standard Compose `Icon` from `Icons.Default.FastRewind` and `Icons.Default.FastForward`, maintaining the existing contentDescription on the parent button and using `contentDescription = null` for the icon itself to avoid double-reading by screen readers.
