# Chronicle

Personal expense tracker for Android, built with Kotlin and Jetpack Compose on Firebase.

## Features

- Accounts (cash, bank, credit card, FDR, DPS) with balances, transfers and transaction fees
- Expenses and income with searchable categories, filters and date ranges
- Auto-logging from bank SMS alerts using per-account rules (expense, income or transfer)
- Credit card settlement and payment-due reminders
- Events: group the expenses of a trip or occasion
- Debt: track what you lent and borrowed per person, with repayments, history and archive
- Google sign-in; data syncs through Firestore and works offline

## Build

1. Create a Firebase project with Google sign-in and Firestore enabled.
2. Register an Android app with package `com.chronicle.expense` and your signing SHA-1.
3. Download `google-services.json` into `app/` (it is not committed).
4. Deploy the security rules: `firebase deploy --only firestore:rules`
5. Build: `./gradlew :app:assembleRelease`

Security rules are in [firestore.rules](firestore.rules): each user reaches only their own data,
and every write must match the shape the app produces.
