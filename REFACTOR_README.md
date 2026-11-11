# SpendWise: Compose structure and French localization

The UI is implemented in **Jetpack Compose**: the screen layouts belong in Kotlin, not `res/layout/*.xml`. Android XML is used for localizable strings, colours, manifest configuration, and file-provider paths.

## Folder responsibilities

- `ui/screens/auth/`: sign-in and account creation presentation.
- `ui/screens/navigation/`: tab navigation and orchestration only.
- `ui/screens/dashboard/`: main overview.
- `ui/screens/transactions/`: list, add/edit and details UIs.
- `ui/screens/budget/`: monthly budget and notification observer.
- `ui/screens/insights/`: spending charts.
- `ui/components/`: reusable Compose widgets, category presentation and voice note UI.
- `ui/theme/`: theme / typography; no business model definitions.
- `model/SpendTransaction.kt`: transaction data model, shared by the UI and persistence layer.
- `data/TransactionRepository.kt`: Room DAO interaction and entity mapping; Compose should not talk directly to Room.
- `data/local/`: Room entities, DAO, schema and migration.
- `auth/`: Firebase authentication state.
- `res/values/strings.xml`: fallback English display text.
- `res/values-fr/strings.xml`: French display text.

## Translation rules

Use `stringResource(R.string.some_key)` in `@Composable` UI and `context.getString(R.string.some_key)` in callbacks, view models and service/notification code. For argument interpolation, use parameterized resource strings (for example `stringResource(R.string.dashboard_signed_in, displayName)`); never build translated sentences by concatenating English fragments.

Categories remain stored with their original stable English identifiers. Call `localizedCategory(transaction.category)` **only when displaying**; do not use translated text as database keys or for category comparison. The transaction kind (`INCOME` / `EXPENSE`), dates (`yyyy-MM-dd`), Room schema, shared preference keys, and Firebase account data remain unchanged. Update both resource files when adding new user-facing UI text.

Android selects `values-fr` when the app's active language is French. This version **does not add an in-app language picker**. It follows the device/app language configured in Android Settings. On supported Android versions users can select a language per app if the app declares supported locales; see `res/xml/locales_config.xml` and manifest configuration.

## What to test in Android Studio

1. Sync Gradle and run **Build > Make Project**.
2. Test login, registration and password reset.
3. Test add/edit/delete transaction, persisted receipt capture/gallery, audio record/playback and re-open transaction details.
4. Test dashboard, filtering, monthly budgets and budget notifications.
5. Change system/app language to French, reopen, and check all screens, category display, dates, dialogues and notifications. Repeat in English.

**Build verification limitation:** The refactor archive was created without an Android SDK/emulator, so it was not compiled or run here. Review build output in Android Studio before merging, and test on a feature branch.
