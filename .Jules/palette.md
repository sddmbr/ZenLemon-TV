## 2026-08-11 - Add ARIA label to clear search button\n**Learning:** The clear search button in SearchInput.kt was missing a contentDescription for screen readers. Using conditional stringResource ensures we only provide a label when the button acts as a clear action.\n**Action:** Always check icon-only buttons for missing contentDescription, especially those that toggle between different icons/states (e.g. search vs close).

## 2026-08-11 - Add contentDescription to empty state lock icon
**Learning:** Empty state UI often uses icons (like Icons.Filled.Lock) which are read as decorative if contentDescription is null. For accessibility, when an icon communicates state, it must have a description.
**Action:** Always check icons used in empty/error states to ensure they provide context to screen readers, instead of relying solely on the text below them.

## 2026-08-11 - Add contentDescription to favorite star badge
**Learning:** Icon-only badges that communicate state (like a favorite star) on media cards were using `contentDescription = null`, causing screen readers to ignore them as decorative. For accessibility, they must use an existing string resource like `stringResource(R.string.a11y_favorite)` so screen readers announce the state appropriately, which is also read automatically if it is inside a clickable parent container.
**Action:** Always verify that informative status icons have a `contentDescription` rather than `null`. Use existing accessibility strings from `strings.xml`.

## 2026-08-11 - Unify Empty State Accessibility
**Learning:** Custom empty state layouts built from primitives often lack polite live regions for accessibility. Reusing existing components like `TvEmptyState` that inherently support `modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }` is crucial for ensuring screen readers announce dynamic state changes without jarring interruptions.
**Action:** Always prefer existing design system components (`TvEmptyState`, `AppMessageState`) over custom empty state layouts to inherit accessibility semantics. When forced to build custom dynamic UI, ensure `LiveRegionMode.Polite` is applied.
