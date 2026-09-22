# Expanse Tracker — Product Requirements Document (PRD) & Architecture

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Database](https://img.shields.io/badge/Storage-Room%20(SQLite)-003B57?logo=sqlite&logoColor=white)](#)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%26%20Zero%20Ads-success)](#)

A modern, privacy-first, lightning-fast personal expense and budget management application built natively for Android using **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database**.

---

## 1. Executive Summary

Managing personal finances should be effortless, private, and distraction-free. **Expanse Tracker** is designed from the ground up as a zero-friction, offline-first personal finance tracker. It requires no login, connects to no third-party cloud servers, serves zero advertisements, and stores 100% of your financial transactions securely on your local device.

---

## 2. Problem Statement

Modern finance and expense tracking apps suffer from critical user-experience and privacy issues:

1. **Intrusive Advertisements & Bloat**:
   - Most free finance apps bombard users with interstitial video ads, full-screen pop-ups, and intrusive banner ads right when they are trying to quickly log a purchase.
   - Heavy upsells and subscription paywalls lock basic utility behind premium monthly tiers.

2. **Invasive Data Harvesting & Privacy Risks**:
   - Financial data is among the most sensitive personal information. Mainstream budgeting apps often upload users' daily transactions, purchase history, salary details, location tags, and payment habits to external cloud servers.
   - This data is frequently aggregated, analyzed, or shared with data brokers and advertisers to build consumer profiles.

3. **Mandatory Account Creation & Cloud Lock-in**:
   - Users are forced to provide phone numbers, email addresses, or link social accounts before they can even log a single grocery expense.
   - If the remote service suffers an outage or shuts down, users lose access to their historic spending data.

4. **Clunky & Slow Logging Workflows**:
   - Many apps take 5 to 6 taps through slow webviews or heavy cloud forms just to record a quick coffee purchase.

---

## 3. Our Solution

**Expanse Tracker** rejects the surveillance-based app model in favor of an **offline-first, user-owned architecture**:

- **100% Offline by Design**:
  - The application requires zero internet connectivity to function. Your transactions and budgets never leave your smartphone.
- **Zero Ads, Zero Trackers**:
  - No advertisement SDKs, no behavioral trackers, no telemetry, and no third-party analytics libraries are included in the codebase.
- **Instant Frictionless Logging**:
  - Direct integration with the Android mobile keyboard numeric pad and one-tap quick-add chips (`+₹50`, `+₹100`, `+₹500`, etc.) allow recording expenses in under 3 seconds.
- **Local Room Database**:
  - All records are saved in a high-performance, ACID-compliant local SQLite database via Android Jetpack Room.
- **Customizable Budgets & Smart Insights**:
  - Real-time monthly spending progress, visual category breakdowns, dynamic budget alerts, and date-range filtering.
- **Clean Material Design 3 UI**:
  - Elegant typography, smooth animations, dark mode support, and adaptive layouts following modern Google Material 3 standards.

---

## 4. Key Functional Features

| Module | Features & Capabilities |
|---|---|
| **Quick Expense Logging** | Native numeric keyboard, instant category tagging (Food, Transport, Bills, Shopping, Health, Entertainment, Utilities, Miscellaneous), payment mode selection (Cash, UPI, Card, Net Banking), quick amount increment chips (+50, +100, +500), optional notes, and custom date picking. |
| **Recurring Bills & Subscriptions** | Dedicated Bills tab to manage monthly subscriptions (Netflix, Rent, WiFi, Gym, Electricity). Tracks due dates, automatic payment status, total monthly subscription overhead, and one-tap "Mark Paid" expense logging. |
| **Budget & Limit Tracking** | Configurable overall monthly spending limit and per-category monthly limits with color-coded warning thresholds (Normal, Alert >80%, Exceeded >100%). |
| **Analytics & Visual Charts** | Interactive category distribution pie charts, weekly and monthly spending bar graphs, daily averages, and highest spending category detection. |
| **PDF Statement Generator** | Custom PDF report exporter using standard Android `PdfDocument`. Generates clean audit statements with customizable date range filters (This Month, Last 30 Days, This Week, Year to Date, or Custom Dates). |
| **Smart Daily Evening Reminder** | Local alarm notification (scheduled at 8:00 PM) prompting users to record daily expenses before bed. Works 100% offline via standard `AlarmManager` and `NotificationManager`. |
| **Search & Filtering** | Instant search by title/notes, filtering by time range (This Week, This Month, All Time) or specific category. |
| **Settings & Customization** | Multi-currency selector (₹ INR default, $, €, £, ¥, C$), custom category limit adjustments, local JSON data backup/restore, and local demo data purge. |

---

## 5. Technical Stack & Architecture

- **Operating System**: Android (minSdk: 26, targetSdk: 35)
- **Language**: Kotlin 2.0+
- **Architecture**: Clean Architecture / MVVM (Model-View-ViewModel)
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **State Management**: Kotlin Coroutines & `StateFlow` with unidirectional data flow
- **Local Database**: Android Jetpack Room with KSP (Kotlin Symbol Processing)
- **Navigation**: Compose Navigation with type-safe state routing

---

## 6. Product Limitations & Trade-Offs

Because the application strictly prioritizes user privacy and offline independence, the following intentional trade-offs exist:

1. **No Automatic Multi-Device Cloud Sync**:
   - Because data is kept strictly on-device with zero cloud servers, records do not automatically synchronize between multiple devices (e.g., between a phone and a tablet).
2. **User-Managed Data Lifecycle**:
   - There is no central server recovery. If the user uninstalls the app without exporting their backup or loses their device, the data cannot be remotely restored.
3. **No Automatic Bank SMS Scraping**:
   - Many apps request dangerous `READ_SMS` permissions to automatically parse bank OTPs and transaction alerts. Expanse Tracker intentionally omits SMS permissions to ensure complete security and Play Store compliance.
4. **Offline Currency Conversion**:
   - The app supports selecting different currency symbols (such as ₹, $, €), but does not perform live dynamic foreign exchange rate conversion since it does not connect to internet exchange rate APIs.

---

## 7. Recommended Roadmap (Exciting Features to Add)

To elevate the user experience even further, here are prioritized feature recommendations:

1. **Local Data Backup & CSV/PDF Export**:
   - Generate beautifully styled PDF monthly spending reports and export raw transaction tables to CSV/Excel for personal accounting.
2. **Biometric App Lock (Fingerprint / Face ID)**:
   - Provide an optional fingerprint or PIN lock screen before entering the app to protect sensitive financial views from prying eyes.
3. **Receipt & Bill Photo Attachments**:
   - Allow attaching photos of physical receipts or invoices to any expense using the zero-permission Android Photo Picker, storing images locally on internal storage.
4. **Recurring Expenses & Subscriptions**:
   - Schedule recurring bills (Rent, Netflix, Broadband, Electricity, Gym) that automatically prompt or log on their scheduled day of the month.
5. **Smart Evening Daily Reminder Notification**:
   - A lightweight local system alarm at 8:00 PM or 9:00 PM: *"Did you spend anything today? Log it in 5 seconds!"*
6. **Savings Goal / Wishlist Tracker**:
   - Set targeted savings pots (e.g., "New Laptop: ₹60,000", "Vacation Fund: ₹25,000") and allocate remaining monthly savings toward each goal.

---

## 8. Development & Build Instructions

```bash
# Clone the repository
git clone https://github.com/your-username/expanse-tracker.git
cd expanse-tracker

# Build the Debug APK using Gradle
gradle assembleDebug

# Output APK is located at:
# app/build/outputs/apk/debug/app-debug.apk
# or pre-built in /apk/ExpanseTracker.apk
```

---

<div align="center">

Developed with ❤️ by **QM Labs**

*Committed to building private, offline-first, and user-centric software.*

</div>
