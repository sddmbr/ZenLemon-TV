## 2026-08-11 - Add ARIA label to clear search button\n**Learning:** The clear search button in SearchInput.kt was missing a contentDescription for screen readers. Using conditional stringResource ensures we only provide a label when the button acts as a clear action.\n**Action:** Always check icon-only buttons for missing contentDescription, especially those that toggle between different icons/states (e.g. search vs close).

## 2026-08-11 - Add contentDescription to empty state lock icon
**Learning:** Empty state UI often uses icons (like Icons.Filled.Lock) which are read as decorative if contentDescription is null. For accessibility, when an icon communicates state, it must have a description.
**Action:** Always check icons used in empty/error states to ensure they provide context to screen readers, instead of relying solely on the text below them.
## 2024-05-18 - Missing Accessibility for Status Badges
**Learning:** Found a pattern in this app's components (`Cards.kt`, `AppMediaCards.kt`) where informative status badge icons (e.g., favorite stars) were using `contentDescription = null`. Since these icons denote item state and are not purely decorative, screen reader users were not informed when an item was a favorite.
**Action:** Always verify that icons used as status indicators have a meaningful `contentDescription` (such as using `stringResource(R.string.a11y_favorite)`) instead of `null`.
