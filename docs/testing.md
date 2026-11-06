# Testing Strategy & Execution Guide

This document defines the comprehensive automated testing architecture, source set organization, and execution procedures for the **FaceRecognition** application.

---

## 1. Architecture & Testing Stack

| Layer | Framework / Library | Primary Purpose | Source Set |
| :--- | :--- | :--- | :--- |
| **Unit Testing** | JUnit 4 (`4.13.2`) + [`MainDispatcherRule`](file:///C:/Users/admin/FaceRecognition/app/src/test/java/com/example/detectionexample/util/MainDispatcherRule.kt) | Isolated testing of business logic, ViewModels, and repositories | `src/test/java/` |
| **Coroutines & Flows** | `kotlinx-coroutines-test:1.7.1` + `app.cash.turbine:1.0.0` | Deterministic scheduling of coroutines, `StateFlow`, and `SharedFlow` | `src/test/java/` |
| **Mocking & Fakes** | `io.mockk:mockk:1.13.10` | Constructor fakes and selective dependency mocking | `src/test/java/` |
| **Host-Side UI** | `org.robolectric:robolectric:4.14.1` | Running Compose UI tests directly on JVM without an emulator | `src/test/java/` |
| **Dependency Injection** | Hilt Testing (`2.60.1`) + [`HiltTestRunner`](file:///C:/Users/admin/FaceRecognition/app/src/androidTest/java/com/example/detectionexample/HiltTestRunner.kt) | Component test scoping and test application instantiation | `src/androidTest/java/` |
| **Screenshot Tests** | `com.android.compose.screenshot:0.0.1-alpha16` | Composable preview rendering and visual regression detection | `src/screenshotTest/kotlin/` |
| **End-to-End (E2E)** | Compose UI Test + `androidx.test.uiautomator:2.3.0` | Verification of complete user flows, system dialogs, and activity lifecycles | `src/androidTest/java/` |
| **Code Coverage** | JaCoCo (`0.8.11`) | Line, branch, and instruction coverage metrics | `build/reports/jacoco/` |

---

## 2. Test Suites & File Structure

### Local Unit & Repository Tests (`app/src/test/java/`)
- [`RecognizedPersonRepositoryTest`](file:///C:/Users/admin/FaceRecognition/app/src/test/java/com/example/detectionexample/repository/RecognizedPersonRepositoryTest.kt):
  - Validates person registration and clearing.
  - Tests Euclidean distance nearest-neighbor calculations in `findNearest(embeddings, threshold)`.
  - Verifies filtering when distance exceeds matching threshold.
- [`DatastoreViewModelTest`](file:///C:/Users/admin/FaceRecognition/app/src/test/java/com/example/detectionexample/viewmodels/DatastoreViewModelTest.kt):
  - Validates `addPerson`, `isRegisteredPersonEmpty`, and `removeRegisteredPerson`.
  - Uses [`MainDispatcherRule`](file:///C:/Users/admin/FaceRecognition/app/src/test/java/com/example/detectionexample/util/MainDispatcherRule.kt) to manage virtual coroutine dispatching.
- [`NavigatorTest`](file:///C:/Users/admin/FaceRecognition/app/src/test/java/com/example/detectionexample/navigation/NavigatorTest.kt):
  - Validates Navigation 3 top-level route switching.
  - Tests multi-backstack state persistence.
  - Enforces the "exit through home" back press navigation pattern.

### Local Compose UI & Robolectric Tests (`app/src/test/java/`)
- Configured via [`robolectric.properties`](file:///C:/Users/admin/FaceRecognition/app/src/test/resources/robolectric.properties) with `sdk=34` and `includeAndroidResources = true` in [`app/build.gradle`](file:///C:/Users/admin/FaceRecognition/app/build.gradle#L67-L71).
- [`NavigationSuiteComposeTest`](file:///C:/Users/admin/FaceRecognition/app/src/test/java/com/example/detectionexample/compose/NavigationSuiteComposeTest.kt):
  - Verifies `NavigationSuiteScaffold` interactive tab transitions between Camera and Video.
- [`AdaptiveWindowSizeTest`](file:///C:/Users/admin/FaceRecognition/app/src/test/java/com/example/detectionexample/compose/AdaptiveWindowSizeTest.kt):
  - Tests layout responsiveness across canonical Material 3 window size classes:
    - **Compact**: 400dp width (phones)
    - **Medium**: 610dp width (foldables / tablets in portrait)
    - **Expanded**: 900dp width (desktop / large landscape displays)

### Compose Preview Screenshot Tests (`app/src/screenshotTest/kotlin/`)
- [`ExamplePreviewScreenshotTest`](file:///C:/Users/admin/FaceRecognition/app/src/screenshotTest/kotlin/com/example/detectionexample/ExamplePreviewScreenshotTest.kt):
  - Annotated with `@PreviewTest`.
  - **Screen-level 9-size matrix**: Tests compact, medium, and expanded combinations (widths: 400, 610, 900 dp; heights: 400, 500, 1000 dp).
  - **Font scale variations**: Mobile preview rendered at 1.5x font scale.
  - **Component-level previews**: Action buttons and interactive controls across typography scales.
- **Reference Image Location**:
  - Golden images are stored under `app/src/screenshotTest/reference/`.

### Instrumented & End-to-End Tests (`app/src/androidTest/java/`)
- Configured with custom test runner [`HiltTestRunner`](file:///C:/Users/admin/FaceRecognition/app/src/androidTest/java/com/example/detectionexample/HiltTestRunner.kt) pointing to `HiltTestApplication`.
- Host debug activity [`HiltTestActivity`](file:///C:/Users/admin/FaceRecognition/app/src/debug/java/com/example/detectionexample/HiltTestActivity.kt) declared in [`app/src/debug/AndroidManifest.xml`](file:///C:/Users/admin/FaceRecognition/app/src/debug/AndroidManifest.xml).
- [`FaceRecognitionE2ETest`](file:///C:/Users/admin/FaceRecognition/app/src/androidTest/java/com/example/detectionexample/FaceRecognitionE2ETest.kt):
  - Verifies application boot, Hilt injection, and initial view rendering on real devices or emulators.

---

## 3. Execution Commands

All commands should be executed from the repository root:

### 1. Run Local Unit & UI Tests
Executes all JUnit 4, Robolectric, and Compose behavior tests on the host JVM:
```bash
./gradlew testDebugUnitTest
```
*Report output:* `app/build/reports/tests/testDebugUnitTest/index.html`

### 2. Generate Code Coverage Report (JaCoCo)
Executes local unit tests across all 10 modules and compiles unified instruction/branch coverage metrics:
```bash
./gradlew jacocoRootReport
```
*Unified HTML report:* `build/reports/jacoco/jacocoRootReport/html/index.html`  
*Unified XML report:* `build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml`

Per-module report for `:app`:
```bash
./gradlew :app:jacocoTestReport
```

### 3. Screenshot Testing Workflow
To record or update golden reference images from previews:
```bash
./gradlew updateDebugScreenshotTest
```
To validate current code against the golden reference images:
```bash
./gradlew validateDebugScreenshotTest
```
*Report output (on failure/diff):* `app/build/reports/screenshotTest/`

### 4. Run Instrumented & E2E Tests (Device / Emulator)
Requires a connected physical Android device or running emulator:
```bash
./gradlew connectedDebugAndroidTest
```
*Report output:* `app/build/reports/androidTests/connected/index.html`

### 5. Full CI / Verification Pipeline
Run the combined verification suite locally:
```bash
./gradlew testDebugUnitTest jacocoTestReport
```
