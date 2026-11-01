# Sharely

**Borrow what you need. Share what you own.**

Sharely is a community item-sharing Android application developed for **COMP9003 – Object Oriented Programming** at the University of Sydney. It helps people list useful belongings, discover items available nearby, request short-term borrowing, and coordinate handovers between owners and borrowers.

Built primarily in **Java** with **Android Studio** and **Firebase**, Sharely demonstrates object-oriented modelling alongside a working multi-user mobile experience.

## Features

- **Accounts:** Register and sign in using Firebase Authentication.
- **Discover items:** Browse listings, search by keyword, and apply category or location-related filters.
- **Create listings:** Add descriptions, categories, rates, availability windows, and photographs selected from the device.
- **Owner-specific controls:** Identify your own listings, add photographs to published items, and manage incoming requests. Owner-only and borrower-only actions are shown appropriately.
- **Availability and bookings:** Request suitable time ranges, review incoming requests, and track bookings by status.
- **Pickup and return workflow:** Coordinate accepted bookings, pickup/return one-time password (OTP) verification, and condition photographs.
- **Communication:** Message other users and view relevant notifications.
- **Community trust:** Ask and answer public listing questions, report inappropriate listings or accounts, and leave eligible reviews.
- **Activity and profile:** Review current or previous borrowing activity and manage account information.

The main navigation uses five areas: **Home, Browse, Activity, Messages, and Me**.

> **Prototype scope:** Sharely is a university project, not a public marketplace. Payment-related screens are simulations; the app does not process real payments. A working Firebase environment is required for cloud-connected features.

## Technology

| Component | Technology |
| --- | --- |
| Android application | Java, Android SDK, XML layouts |
| Development environment | Android Studio, Gradle |
| Authentication | Firebase Authentication |
| Shared application data | Cloud Firestore |
| Listing and condition images | Firebase Storage |
| Booking/OTP backend | Firebase Cloud Functions (deployed project environment) |
| Image display | Glide |
| Automated testing | JUnit and Android testing tools |
| Source control | Git and GitHub |

## Repository layout

The final repository may contain additional support files. Its principal directories are:

```text
Sharely/
├── app/
│   ├── src/main/java/com/example/sharely/   # Android activities, fragments, models and services
│   ├── src/main/res/                      # Layouts, colours, drawable resources and menus
│   └── src/test/                          # Local unit tests
├── gradle/                                # Gradle wrapper and version configuration
├── gradlew                                # macOS/Linux Gradle launcher
├── gradlew.bat                            # Windows Gradle launcher
├── build.gradle.kts
├── settings.gradle.kts
├── firebase.json                          # Firebase configuration, where applicable
└── README.md
```

Backend code and rule files, if included, should be reviewed alongside the deployed Firebase project. Local environment files and private credentials are not part of the public source repository.

## Getting started

### Prerequisites

- A recent Android Studio version compatible with this project's Android Gradle Plugin and SDK settings.
- An Android emulator or supported Android phone, with the appropriate SDK installed through Android Studio.
- A Firebase project configured for the app's Android application ID, **`com.example.sharely`**.
- Access to the corresponding **Firebase Authentication, Firestore, Storage, and deployed Cloud Functions** required for the complete transaction workflow.

### 1. Clone and open the project

```bash
git clone https://github.com/ryana242/Sharely.git
cd Sharely
```

Open the **Sharely** folder in Android Studio and let Gradle finish syncing.

### 2. Configure Firebase

Place the project's appropriate **`google-services.json`** under `app/` if it is not already present:

```text
app/google-services.json
```

The Firebase configuration must correspond to the app's application ID and the backend environment you are authorised to use. A newly created Firebase project will not automatically contain Sharely's collections, security rules, indexes or deployed callable functions. Do not commit service-account private keys or administrator credentials.

### 3. Build and test

From the repository root, use the Gradle wrapper:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

To run the app, start an emulator in Android Studio and click **Run** with the **app** configuration selected. Alternatively, install the generated APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The `adb` command requires Android SDK platform-tools on your `PATH`.

## Suggested demonstration

Use two different authenticated accounts, preferably on separate emulators or phones:

1. **Owner:** Publish a listing with availability and a photo.
2. **Borrower:** Find the listing and request an available period.
3. **Owner:** Accept the request.
4. **Both:** Open the accepted transaction and follow the pickup process, including condition photography and OTP verification.
5. **Both:** Complete the return process and verify the booking moves into history.
6. **Both:** Demonstrate messages, questions, owner-only controls, and reviews where eligible.

Do not display real OTP values or sensitive user information in public screenshots or videos. Function availability and authorisation are controlled by the configured Firebase backend.

## Project team

| Team member | Main areas of contribution |
| --- | --- |
| **Fardin Selim Khan** | Search and discovery, availability and booking logic, Java tests, integration and cross-device testing |
| **Ryana Binte Imtiaz** | Android UI, authentication/listing flows, profiles, messaging, photos, usability and visual polish |
| **Lingyu Cheng (Leo)** | Firebase data persistence, backend functions, security rules, notifications, transaction integration and backend fixes |

Features were integrated and tested collaboratively. The Git `.mailmap` file, if present, consolidates historical contributor identities for local Git reports; it does not alter commit history.

## Known limitations

- Real payments, insurance and formal disputes are not implemented.
- Backend-dependent workflows require the team’s deployed Firebase configuration and permissions.
- This prototype is intended for coursework demonstration rather than unrestricted production deployment.
- Successful local builds and unit tests are not substitutes for real-device, multi-account integration tests.

## Acknowledgements and academic integrity

This project was developed as university coursework. The team used generative-AI tools for learning, brainstorming, candidate code, debugging assistance and writing support during development; suggested outputs were reviewed and tested by the team. See the submitted group reflection and the unit's required AI-use disclosure for details.
