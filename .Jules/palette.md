## 2026-08-11 - Add ARIA label to clear search button\n**Learning:** The clear search button in SearchInput.kt was missing a contentDescription for screen readers. Using conditional stringResource ensures we only provide a label when the button acts as a clear action.\n**Action:** Always check icon-only buttons for missing contentDescription, especially those that toggle between different icons/states (e.g. search vs close).

## 2026-08-11 - Add contentDescription to empty state lock icon
**Learning:** Empty state UI often uses icons (like Icons.Filled.Lock) which are read as decorative if contentDescription is null. For accessibility, when an icon communicates state, it must have a description.
**Action:** Always check icons used in empty/error states to ensure they provide context to screen readers, instead of relying solely on the text below them.
