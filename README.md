# 🏢 Rent Collection Manager

> An Android app for property rent management with **real-time sync**, **offline support**, and **multi-user collaboration** — built with modern Jetpack Compose + Firebase stack.

---

## 📋 Table of Contents

- [Getting Started](#getting-started)
- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Data Models](#data-models)
- [Firestore Database Structure](#firestore-database-structure)
- [Navigation Flow](#navigation-flow)
- [Feature Guide — Screen by Screen](#feature-guide)
- [Firebase Setup](#firebase-setup)
- [First-Time App Setup](#first-time-app-setup)
- [Default Credentials](#default-credentials)
- [Offline Support](#offline-support)
- [Known Implementation Notes](#known-implementation-notes)

---

<a id="getting-started"></a>
## 🚀 Getting Started

### Prerequisites

| Tool | Required Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 or newer |
| Android SDK | API 34 (Android 14) installed |
| Gradle | 8.x (bundled via wrapper — no manual install needed) |
| Firebase account | Free Spark plan is sufficient |

> Android Studio installs JDK and Android SDK automatically. Make sure **SDK Platform API 34** and **Build Tools 34.x** are installed via `SDK Manager → SDK Platforms`.

---

### Step 1 — Clone the repository

```bash
git clone https://github.com/your-username/RentCollection.git
cd RentCollection
```

---

### Step 2 — Firebase setup

This app requires a Firebase project. Follow the [Firebase Setup](#-firebase-setup-required-before-building) section below for detailed steps. In summary:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable **Authentication → Email/Password**
3. Create a **Firestore database** (region: `asia-south1` or nearest)
4. Register the Android app with package name `com.rentcollection`
5. Download `google-services.json` and place it at `app/google-services.json`

> `google-services.json` is excluded from version control (`.gitignore`). Every developer must add their own copy.

---

### Step 3 — Open in Android Studio

1. Launch Android Studio
2. `File → Open` → select the `RentCollection` folder
3. Wait for Gradle sync to complete (first sync downloads dependencies — takes 2–5 minutes)
4. If sync fails: `File → Invalidate Caches → Invalidate and Restart`, then sync again

---

### Step 4 — Configure local.properties

Android Studio creates this file automatically. Verify it contains your SDK path:

```
sdk.dir=/Users/<your-username>/Library/Android/sdk        # macOS
sdk.dir=C\:\\Users\\<your-username>\\AppData\\Local\\Android\\Sdk  # Windows
```

> `local.properties` is excluded from version control — never commit it.

---

### Step 5 — Build the project

**Option A — Android Studio (recommended)**
```
Build → Make Project  (or Ctrl+F9 / Cmd+F9)
```

**Option B — Command line**
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

Build output: `app/build/outputs/apk/debug/app-debug.apk`

---

### Step 6 — Run the project

**On a physical device**
1. Enable **Developer Options** on the device: `Settings → About Phone → tap Build Number 7 times`
2. Enable **USB Debugging**: `Developer Options → USB Debugging → On`
3. Connect via USB — Android Studio auto-detects the device
4. Click the **Run** button (green triangle) in Android Studio

**On an emulator**
1. `Device Manager → Create Virtual Device`
2. Select a device (e.g. Pixel 6) with **API 34** system image
3. Click **Run**

> Minimum supported device: Android 8.0 (API 26)

---

### Step 7 — First-time in-app setup

1. On the Login screen, tap **"Create Default Accounts"** — this creates two Firebase Auth users
2. Log in with `owner@rent.com` / `Rent@1234`
3. Three seed buildings are created automatically on first login
4. Start adding floors, houses, and tenants

See [Default Credentials](#-default-credentials) for the full list.

---

<a id="overview"></a>
## 🌟 Overview

**RentCollection** is a two-user Android app (Owner + Father) to track monthly rent collection across multiple **buildings → floors → houses**.

```
What it solves:
  ✅ Know at a glance how much rent is collected vs pending this month
  ✅ Record payments with mode (Cash / UPI / Bank / Cheque)
  ✅ Track tenants per house with contact info
  ✅ Both users see live updates instantly via Firebase real-time sync
  ✅ Works offline — syncs changes when reconnected
```

---

<a id="tech-stack"></a>
## 🛠 Tech Stack

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│   Jetpack Compose  +  Material 3  +  Navigation Compose │
├─────────────────────────────────────────────────────────┤
│                   STATE MANAGEMENT                       │
│        MVVM  +  StateFlow  +  Hilt ViewModels           │
├─────────────────────────────────────────────────────────┤
│                     DATA LAYER                           │
│       Repositories  +  Kotlin Coroutines / Flow          │
├─────────────────────────────────────────────────────────┤
│                   BACKEND / CLOUD                        │
│   Firebase Firestore (real-time)  +  Firebase Auth       │
└─────────────────────────────────────────────────────────┘
```

| Category | Library / Tool | Version |
|---|---|---|
| Language | Kotlin | 1.9.22 |
| UI Framework | Jetpack Compose + Material3 | BOM 2024.02.00 |
| Architecture | MVVM + Clean Architecture | — |
| State | Kotlin StateFlow / Flow | 1.7.3 |
| DI | Hilt | 2.50 |
| Navigation | Navigation Compose | 2.7.7 |
| Database | Firebase Firestore | BOM 32.7.2 |
| Authentication | Firebase Auth | BOM 32.7.2 |
| Min SDK | Android 8.0 (API 26) | — |
| Target SDK | Android 14 (API 34) | — |

---

<a id="architecture"></a>
## 🏗 Architecture

The app follows **MVVM + Clean Architecture** with clear separation into three layers:

```
╔══════════════════════════════════════════════════════════════╗
║                     UI LAYER (Compose)                       ║
║  ┌──────────┐  ┌─────────┐  ┌──────────┐  ┌─────────────┐  ║
║  │ Splash   │  │  Login  │  │Dashboard │  │  Buildings  │  ║
║  │ Screen   │  │ Screen  │  │ Screen   │  │  Screen     │  ║
║  └────┬─────┘  └────┬────┘  └────┬─────┘  └──────┬──────┘  ║
║       │             │            │                │          ║
║  ┌────▼─────┐  ┌────▼────┐  ┌────▼─────┐  ┌──────▼──────┐  ║
║  │ Splash   │  │  Auth   │  │Dashboard │  │ Buildings   │  ║
║  │ViewModel │  │ViewMdl  │  │ViewModel │  │ ViewModel   │  ║
║  └────┬─────┘  └────┬────┘  └────┬─────┘  └──────┬──────┘  ║
╠═══════╪═════════════╪════════════╪════════════════╪══════════╣
║       │    DOMAIN / REPOSITORY LAYER               │          ║
║  ┌────▼─────────────▼────────────▼────────────────▼──────┐  ║
║  │   AuthRepository  │  BuildingRepository               │  ║
║  │   TenantRepository│  PaymentRepository                │  ║
║  └────────────────────────────────────────────────────────┘  ║
╠══════════════════════════════════════════════════════════════╣
║                   DATA LAYER (Firebase)                      ║
║  ┌───────────────────┐    ┌──────────────────────────────┐  ║
║  │  Firebase Auth    │    │      Firebase Firestore       │  ║
║  │  (Email/Password) │    │   (Real-time + Offline sync)  │  ║
║  └───────────────────┘    └──────────────────────────────┘  ║
╚══════════════════════════════════════════════════════════════╝
```

### How data flows

```
User taps "Record Payment"
         │
         ▼
  RecordPaymentScreen  ──(collectAsState)──▶  RecordPaymentViewModel
         │                                            │
         │  ◀── UiState (loading/success/error) ──────┘
         │                                            │
         │                                   PaymentRepository
         │                                            │
         │                                  Firestore (upsert)
         │                                            │
         ▼                                            │
  All listeners (DashboardVM, HistoryVM)  ◀── real-time snapshot update
```

---

<a id="project-structure"></a>
## 📁 Project Structure

```
app/src/main/java/com/rentcollection/
│
├── MainActivity.kt                    ← Single activity, hosts NavHost
├── RentCollectionApp.kt               ← @HiltAndroidApp entry point
│
├── di/
│   └── AppModule.kt                   ← Hilt module: provides FirebaseAuth, Firestore
│
├── data/
│   ├── model/                         ← Pure Kotlin data classes (no Android deps)
│   │   ├── User.kt
│   │   ├── Building.kt
│   │   ├── Floor.kt
│   │   ├── House.kt                   ← Contains List<RentChange>
│   │   ├── Tenant.kt
│   │   ├── Payment.kt                 ← Denormalized (includes house/building/tenant names)
│   │   └── RentChange.kt             ← Embedded in House for rent history
│   │
│   └── repository/                    ← All Firebase calls live here
│       ├── AuthRepository.kt
│       ├── BuildingRepository.kt      ← Handles buildings + floors + houses
│       ├── TenantRepository.kt
│       └── PaymentRepository.kt
│
├── ui/
│   ├── theme/
│   │   ├── AppColors.kt               ← Named color constants (Green, Red, Amber)
│   │   ├── Color.kt                   ← Material color roles
│   │   ├── Theme.kt                   ← Light/Dark MaterialTheme setup
│   │   └── Type.kt                    ← Typography scale
│   │
│   ├── navigation/
│   │   ├── Screen.kt                  ← Sealed class with all route strings
│   │   └── NavGraph.kt                ← NavHost with all composable destinations
│   │
│   ├── components/
│   │   └── CommonComponents.kt        ← LoadingScreen, ErrorMessage, EmptyState,
│   │                                     ShimmerBox, SectionHeader
│   │
│   └── screens/
│       ├── splash/        SplashScreen + SplashViewModel
│       ├── auth/          LoginScreen + AuthViewModel
│       ├── main/          MainScreen  + MainViewModel   (3-tab host)
│       ├── dashboard/     DashboardScreen + DashboardViewModel
│       ├── buildings/     BuildingsScreen + BuildingDetailScreen + ViewModels
│       ├── house/         HouseDetailScreen + HouseDetailViewModel
│       ├── history/       PaymentHistoryScreen + PaymentHistoryViewModel
│       └── addedit/       AddEdit{Building,Floor,House,Tenant} + RecordPayment
│                          (10 files: 5 screens + 5 ViewModels)
│
└── utils/
    └── Extensions.kt                  ← Double.toRupees(), Int.toMonthName(), etc.
```

---

<a id="data-models"></a>
## 🗂 Data Models

### Entity Relationship

```
Building (1) ──────── (N) Floor (1) ──────── (N) House
                                                    │
                                                    ├── (0..1) active Tenant
                                                    │         (isActive = true)
                                                    │
                                                    └── (N) Payment
                                                            (one per month ideally)
```

### Model Details

```
┌─────────────────────────────────────────────────────┐
│ Building                                             │
│   buildingId   : String  (Firestore doc ID)         │
│   name         : String                             │
│   address      : String                             │
│   description  : String                             │
│   createdAt    : Date    (@ServerTimestamp)         │
│   createdBy    : String  (userId)                   │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Floor                                                │
│   floorId      : String  (Firestore doc ID)         │
│   floorNumber  : Int     (0=Ground, 1=First, etc.)  │
│   floorName    : String  (e.g. "Ground Floor")      │
│   buildingId   : String  (parent ref)               │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ House                                                │
│   houseId      : String  (Firestore doc ID)         │
│   houseNumber  : String  (e.g. "1A", "G-1")         │
│   rentAmount   : Double  (current default rent)     │
│   depositAmount: Double                             │
│   floorId      : String                             │
│   buildingId   : String                             │
│   rentChanges  : List<RentChange>  ← rent history   │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ RentChange  (embedded in House.rentChanges)          │
│   amount              : Double                      │
│   effectiveFromMonth  : Int   (1-12)                │
│   effectiveFromYear   : Int                         │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Tenant                                               │
│   tenantId       : String  (Firestore doc ID)       │
│   houseId        : String                           │
│   buildingId     : String                           │
│   name           : String                           │
│   phone          : String                           │
│   email          : String                           │
│   alternatePhone : String                           │
│   moveInDate     : String  (e.g. "2024-01-15")      │
│   moveOutDate    : String  (blank if active)        │
│   isActive       : Boolean ← only 1 active per house│
│   notes          : String                           │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Payment                                              │
│   paymentId      : String  (Firestore doc ID)       │
│   houseId        : String                           │
│   buildingId     : String                           │
│   tenantId       : String                           │
│   amount         : Double                           │
│   month          : Int     (1-12)                   │
│   year           : Int                              │
│   paidDate       : String                           │
│   paymentMode    : String  (Cash/UPI/Bank/Cheque)   │
│   notes          : String                           │
│   isReceived     : Boolean                          │
│   recordedBy     : String  (display name)           │
│   recordedByEmail: String  (audit trail)            │
│   recordedAt     : Date    (@ServerTimestamp)       │
│   ── Denormalized fields (for display, no joins) ── │
│   houseNumber    : String                           │
│   buildingName   : String                           │
│   tenantName     : String                           │
│   tenantPhone    : String                           │
│   floorId        : String                           │
│   floorName      : String                           │
└─────────────────────────────────────────────────────┘
```

> **Why denormalized Payment?** Firestore doesn't support JOIN queries. Payment documents store building/house/tenant names so the history screen loads instantly without additional fetches.

---

<a id="firestore-database-structure"></a>
## 🔥 Firestore Database Structure

```
Firestore Root
│
├── users/
│   └── {userId}/
│       ├── name        : "Owner"
│       ├── email       : "owner@rent.com"
│       └── role        : "owner" | "viewer"
│
├── buildings/
│   └── {buildingId}/
│       ├── name        : "Building A"
│       ├── address     : "..."
│       ├── createdAt   : Timestamp
│       ├── createdBy   : userId
│       │
│       └── floors/
│           └── {floorId}/
│               ├── floorNumber : 0
│               ├── floorName   : "Ground Floor"
│               ├── buildingId  : "..."
│               │
│               └── houses/
│                   └── {houseId}/
│                       ├── houseNumber  : "G-1"
│                       ├── rentAmount   : 8000.0
│                       ├── depositAmount: 16000.0
│                       ├── floorId      : "..."
│                       ├── buildingId   : "..."
│                       └── rentChanges  : [ {amount, month, year}, ... ]
│
├── tenants/
│   └── {tenantId}/
│       ├── houseId     : "..."
│       ├── buildingId  : "..."
│       ├── name        : "Ravi Kumar"
│       ├── phone       : "+91 98765 43210"
│       ├── isActive    : true
│       └── moveInDate  : "2024-03-01"
│
└── payments/
    └── {paymentId}/
        ├── houseId     : "..."
        ├── buildingId  : "..."
        ├── month       : 4
        ├── year        : 2025
        ├── amount      : 8000.0
        ├── paymentMode : "Cash"
        ├── isReceived  : true
        ├── recordedBy  : "Owner"
        └── recordedAt  : Timestamp
```

### Collection Group Queries Used

| Query | Purpose |
|---|---|
| `collectionGroup("houses")` | Dashboard — get all houses across all buildings |
| `collectionGroup("floors")` | Dashboard — get all floor names for house lookup |

---

<a id="navigation-flow"></a>
## 🗺 Navigation Flow

```
App Launch
    │
    ▼
┌──────────┐
│  SPLASH  │  ← Checks Firebase Auth state (1–2s)
└────┬─────┘
     │
     ├── Not logged in ──────────────────────────────────┐
     │                                                   │
     ▼                                                   ▼
┌──────────┐                                      ┌──────────┐
│  LOGIN   │ ──── successful login ──────────────▶│   MAIN   │
└──────────┘                                      └────┬─────┘
                                                       │
                          ┌────────────────────────────┤
                          │            │               │
                          ▼            ▼               ▼
                    ┌──────────┐ ┌──────────┐ ┌──────────────┐
                    │DASHBOARD │ │BUILDINGS │ │   PAYMENT    │
                    │  (Tab 0) │ │  (Tab 1) │ │  HISTORY     │
                    └────┬─────┘ └────┬─────┘ │  (Tab 2)     │
                         │            │        └──────────────┘
                         │            ▼
                         │     ┌──────────────┐
                         │     │  BUILDING    │  ← expandable floors/houses
                         │     │  DETAIL      │
                         │     └──────┬───────┘
                         │            │
                         │            ▼
                         │     ┌──────────────┐
                         └────▶│    HOUSE     │  ← tenant info + payment status
                               │    DETAIL    │
                               └──────┬───────┘
                                      │
                                      ▼
                               ┌──────────────┐
                               │   RECORD     │  ← log cash/UPI/bank/cheque
                               │   PAYMENT    │
                               └──────────────┘

Add/Edit Screens (accessible via FAB / edit icons):
  ┌────────────────────────────────────────────┐
  │  AddEditBuilding   AddEditFloor            │
  │  AddEditHouse      AddEditTenant           │
  └────────────────────────────────────────────┘
```

### Screen Routes Reference

| Screen | Route |
|---|---|
| Splash | `splash` |
| Login | `login` |
| Main (tab host) | `main` |
| Dashboard | `dashboard` |
| Buildings | `buildings` |
| Payment History | `payment_history` |
| Building Detail | `building_detail/{buildingId}` |
| House Detail | `house_detail/{buildingId}/{floorId}/{houseId}` |
| Add/Edit Building | `add_edit_building?buildingId={buildingId}` |
| Add/Edit Floor | `add_edit_floor/{buildingId}?floorId={floorId}` |
| Add/Edit House | `add_edit_house/{buildingId}/{floorId}?houseId={houseId}` |
| Add/Edit Tenant | `add_edit_tenant/{houseId}/{buildingId}?tenantId={tenantId}` |
| Record Payment | `record_payment/{houseId}/{buildingId}/{floorId}` |

---

<a id="feature-guide"></a>
## 📱 Feature Guide — Screen by Screen

### 1. Splash Screen
```
Purpose : Auth check on launch
Flow    : Checks FirebaseAuth.currentUser
          → logged in  : navigate to Main (skip login)
          → logged out : navigate to Login
```

---

### 2. Login Screen
```
Purpose : Email/password authentication
Features:
  • Email + password fields
  • "Create Default Accounts" button  ← one-time setup
      Creates owner@rent.com (owner role)
      Creates father@rent.com (viewer role)
      Password for both: Rent@1234
  • Error messages for wrong credentials
  • Loading state during sign-in
```

---

### 3. Main Screen (Tab Host)
```
Purpose : Tab navigation container
Tabs    : [Dashboard] [Buildings] [Payment History]
Extras  : Top app bar with user name + logout button
          Tab indicator with animated pager
```

---

### 4. Dashboard Screen
```
Purpose : Monthly collection overview at a glance

┌──────────────────────────────────────────────┐
│  April 2025   ▼ (month picker)               │
│  All Buildings ▼ (building filter)           │
│                                              │
│  ████████████░░░░░  75% Collected            │
│  Collected: ₹48,000 / Total: ₹64,000         │
│                                              │
│  PENDING (4)                                 │
│  ┌────────────────────────────────────────┐  │
│  │ House G-2 · Building A                 │  │
│  │ Ravi Kumar · ₹8,000   [Mark as Paid]   │  │
│  └────────────────────────────────────────┘  │
│  COLLECTED (8)                               │
│  ┌────────────────────────────────────────┐  │
│  │ House 1A · Building B  ✓ Cash          │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘

Key Features:
  ✅ Month/year picker dialog
  ✅ Filter by specific building or "All Buildings"
  ✅ Progress bar (collected ÷ total houses)
  ✅ "Mark as Paid" quick action from dashboard
  ✅ Undo "mark as paid" with snackbar
  ✅ Shimmer skeleton loading state
```

---

### 5. Buildings Screen
```
Purpose : View all buildings with monthly stats

┌──────────────────────────────────────────────┐
│  🔍 Search buildings...                      │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │  Building A                            │  │
│  │  Sector 12, Hyderabad                  │  │
│  │  April 2025: 5/8 collected             │  │
│  │  ██████░░░  62%                        │  │
│  └────────────────────────────────────────┘  │
│  [+ Add Building]  (FAB)                     │
└──────────────────────────────────────────────┘

Key Features:
  ✅ Search/filter buildings by name
  ✅ Per-building payment progress for current month
  ✅ Tap to open Building Detail
  ✅ FAB to add a new building
```

---

### 6. Building Detail Screen
```
Purpose : Drill down into floors and houses

┌──────────────────────────────────────────────┐
│  ← Building A                    [Edit] [⋮]  │
│                                              │
│  ▼ Ground Floor        [Bulk Mark Paid]      │
│    ┌──────────────────────────────────────┐  │
│    │ 🔴  G-1  Ravi Kumar  ₹8,000         │  │
│    │ 🟢  G-2  Suresh B    ₹7,500  ✓ UPI  │  │
│    └──────────────────────────────────────┘  │
│  ▶ First Floor                               │
│                                              │
│  [+ Add Floor]  (FAB)                        │
└──────────────────────────────────────────────┘

Key Features:
  ✅ Expandable floor sections (tap to expand/collapse)
  ✅ 🔴 Red = pending, 🟢 Green = paid per house
  ✅ "Bulk Mark Floor Paid" button per floor
  ✅ Add floor / add house buttons
  ✅ Tap house to open House Detail
  ✅ Long-press to edit/delete floor or house
```

---

### 7. House Detail Screen
```
Purpose : Individual unit with tenant and payment info

┌──────────────────────────────────────────────┐
│  ← House G-1 · Ground Floor                 │
│                                              │
│  TENANT                                      │
│  👤 Ravi Kumar                               │
│  📞 +91 98765 43210  [call]                  │
│  📅 Move-in: 01 Mar 2024                     │
│                                              │
│  APRIL 2025                    ◀ ▶           │
│  Status: PENDING                             │
│  Expected: ₹8,000                            │
│                                              │
│  [Record Payment]  [Edit Tenant]             │
│                                              │
│  PAYMENT HISTORY                             │
│  Mar 2025  ₹8,000  Cash   ✓ Received         │
│  Feb 2025  ₹8,000  UPI    ✓ Received         │
└──────────────────────────────────────────────┘

Key Features:
  ✅ One-tap dial to tenant phone
  ✅ Month selector (◀ ▶ arrows)
  ✅ Current month payment status (pending/paid)
  ✅ Record new payment or update existing
  ✅ Full payment history list
  ✅ Edit tenant info
  ✅ Move-out tenant (marks isActive = false)
```

---

### 8. Record Payment Screen
```
Purpose : Log a rent payment for a specific month

┌──────────────────────────────────────────────┐
│  ← Record Payment                           │
│  House G-1 · Building A                      │
│                                              │
│  Month: April 2025    [Change Month]         │
│  Tenant: Ravi Kumar                          │
│  Expected: ₹8,000                            │
│                                              │
│  Amount:  [8000        ]                     │
│  Mode:    ● Cash  ○ UPI  ○ Bank  ○ Cheque   │
│  Date:    [26 Apr 2025 ]                     │
│  Notes:   [            ]                     │
│                                              │
│  [Save Payment]                              │
└──────────────────────────────────────────────┘

Key Features:
  ✅ Pre-fills expected rent amount
  ✅ Payment mode selection (Cash/UPI/Bank Transfer/Cheque)
  ✅ Date picker for actual payment date
  ✅ Upsert logic: updates if payment exists for that month
  ✅ Audit: records who logged it + timestamp
```

---

### 9. Payment History Screen
```
Purpose : Full ledger of all recorded payments

┌──────────────────────────────────────────────┐
│  All Buildings ▼    April 2025 ▼             │
│                                              │
│  26 Apr 2025                                 │
│  ┌──────────────────────────────────────┐    │
│  │ House G-1 · Building A              │    │
│  │ Ravi Kumar · ₹8,000 · Cash          │    │
│  │ Recorded by: Owner                  │    │
│  └──────────────────────────────────────┘    │
│                                              │
│  25 Apr 2025                                 │
│  ┌──────────────────────────────────────┐    │
│  │ House 1A · Building B               │    │
│  │ Suresh B · ₹7,500 · UPI             │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘

Key Features:
  ✅ Filter by building
  ✅ Filter by month/year
  ✅ Shows who recorded each payment (audit trail)
  ✅ Sorted by date descending (latest first)
  ✅ Pull-to-refresh
```

---

### 10. Add/Edit Screens

| Screen | Fields |
|---|---|
| **AddEditBuilding** | Name, Address, Description |
| **AddEditFloor** | Floor Number, Floor Name |
| **AddEditHouse** | House Number, Rent Amount, Deposit Amount |
| **AddEditTenant** | Name, Phone, Alternate Phone, Email, Move-in Date, Move-out Date, Notes |

All add/edit screens:
- Pre-fill fields when editing (optional `?id=` param in route)
- Show loading spinner during save
- Navigate back on success
- Show error snackbar on failure

---

<a id="firebase-setup"></a>
## 🔥 Firebase Setup (Required before building)

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project (e.g. "RentCollectionApp")
3. Disable Google Analytics (optional)

### 2. Enable Authentication

```
Firebase Console → Build → Authentication
→ Get started
→ Sign-in method → Email/Password → Enable → Save
```

### 3. Enable Firestore

```
Firebase Console → Build → Firestore Database → Create database
→ Region: asia-south1 (Mumbai) or nearest to you
→ Start in test mode (you'll deploy proper rules next)
```

### 4. Add Android App

```
Project settings (⚙️) → Add app → Android
Package name: com.rentcollection
Download google-services.json
Place it at: app/google-services.json  (replace the placeholder)
```

### 5. Deploy Firestore Security Rules

```bash
# Install Firebase CLI (once)
npm install -g firebase-tools
firebase login

# From project root
firebase use --add        # select your Firebase project
firebase deploy --only firestore:rules
```

### 6. Build and Run

```bash
# Option 1: Android Studio → Run
# Option 2: Command line
./gradlew assembleDebug
```

---

<a id="first-time-app-setup"></a>
## 🚀 First-Time App Setup

After building and installing:

1. Launch the app — you'll see the **Login** screen
2. Tap **"Create Default Accounts"**
   - Creates `owner@rent.com` with role `owner`
   - Creates `father@rent.com` with role `viewer`
   - Password for both: **`Rent@1234`**
3. Log in with either account
4. Three seed buildings (A, B, C) are auto-created on first login
5. Start adding floors, houses, and tenants

> **Tip:** Both users can use the app simultaneously — changes appear in real-time for both.

---

<a id="default-credentials"></a>
## 🔑 Default Credentials

| User | Email | Password | Role |
|---|---|---|---|
| Owner | `owner@rent.com` | `Rent@1234` | owner |
| Father | `father@rent.com` | `Rent@1234` | viewer |

> Change passwords in Firebase Console → Authentication after first login.

---

<a id="offline-support"></a>
## 📡 Offline Support

```
Online  ──▶ Real-time Firestore sync (both users see changes instantly)
             │
Offline ──▶ Firestore offline persistence (reads cached data)
             Changes are queued locally
             │
Back online ▶ Queued writes sync automatically to Firestore
```

Firestore offline persistence is enabled in `AppModule.kt`:
```kotlin
FirebaseFirestore.getInstance().apply {
    firestoreSettings = firestoreSettings {
        isPersistenceEnabled = true
    }
}
```

---

## 🔒 Security Rules Summary

The `firestore.rules` file enforces:

```
/users/{userId}        → only that user can read/write their own profile
/buildings/**          → any authenticated user can read/write
/tenants/**            → any authenticated user can read/write
/payments/**           → any authenticated user can read/write
```

No role-based access restrictions currently — both owner and viewer have equal write access.

---

<a id="known-implementation-notes"></a>
## 🧰 Known Implementation Notes

### Boolean Serialization (Firestore)
Fields prefixed with `is` (like `isActive`, `isReceived`) require special handling to serialize correctly with Firestore:
```kotlin
// ✅ Correct
@get:JvmName("getIsActive")
var isActive: Boolean = true

// ❌ Problematic — Firestore may not serialize isXxx fields on data classes
val isActive: Boolean = true
```

### Payment ID Assignment
Payment documents use Firestore's auto-generated IDs. After creation, the ID is assigned back via `.copy()`:
```kotlin
val doc = paymentsCollection.add(payment).await()
payment.copy(paymentId = doc.id)
// Note: @DocumentId on val fields is unreliable for this use case
```

### Currency Formatting
Indian Rupees formatted via extension function in `Extensions.kt`:
```kotlin
Double.toRupees()     // → "₹8,000"
Int.toMonthName()     // → "Apr"
Int.toFullMonthName() // → "April"
```

---

## 📂 File Quick Reference

| What you want to change | File to look at |
|---|---|
| Add a new screen | `NavGraph.kt`, `Screen.kt`, new screen + VM files |
| Change colors / theme | `AppColors.kt`, `Theme.kt` |
| Add a new Firestore field | Model file → Repository → ViewModel → Screen |
| Change navigation logic | `NavGraph.kt` |
| Add a new payment mode | `RecordPaymentScreen.kt`, `RecordPaymentViewModel.kt` |
| Change seed buildings | `MainViewModel.kt` (seedBuildings function) |
| Change default accounts | `AuthViewModel.kt` (seedUsers function) |
| Firestore security rules | `firestore.rules` |
| DI / dependency wiring | `AppModule.kt` |

---

*Built with Kotlin + Jetpack Compose + Firebase — targeting Android 8.0+*
# RentCollection
