# AGENTS.md

Welcome to the **FaceRecognition** repository. This document outlines architectural context, key conventions, and developer/agent workflows.

---

## 1. Project Overview & Architecture

- **Application**: Android Facial Detection & Recognition Utility.
- **Project Structure & Modularization**:
  - `:app`: Application entry point, Hilt root, and Navigation 3 destination coordinator.
  - `:core:common`: Shared coroutine dispatchers, executors, and common extensions.
  - `:core:model`: Pure domain entities (`Person`, `Recognition`, `TrackedRecognition`) and serialization.
  - `:core:data`: DataStore preferences and face embedding repositories.
  - `:core:ml`: LiteRT runtime (`CompiledModel`), BlazeFace, MobileNet SSD, MobileFaceNet, and OpenCV detectors.
  - `:core:camera`: CameraX processing, Grafika OpenGL pipeline, frame analysis (`AnalysisViewModel`), shaders, and overlays.
  - `:core:ui`: Material 3 design system tokens, typography, shared adaptive previews, and permission dialogs.
  - `:feature:detection`: Live camera detection and face recognition screen (`CameraPreview`).
  - `:feature:video`: Video player analysis and batch processing screen (`VideoPlayer`).
  - `:feature:settings`: Hyperparameter and person database management dialogs.
- **Tech Stack**:
  - **Compile SDK**: `37` | **Target SDK**: `37` | **Min SDK**: `24`
  - **Android Gradle Plugin (AGP)**: `9.1.0` | **Gradle**: `9.3.1`
  - **Kotlin**: `2.2.10` | **JVM Target**: `17`
  - **UI Toolkit**: 100% Declarative Jetpack Compose (Material 3 + Adaptive Navigation Suite).
  - **Navigation**: Jetpack Navigation 3 (`androidx.navigation3:1.2.0`) with multi-backstack state holder.
  - **Predictive Back**: AndroidX NavigationEvent (`androidx.navigationevent:1.2.0`).
  - **On-Device Machine Learning**: Google LiteRT runtime (`com.google.ai.edge.litert:2.3.0`) using bundled `CompiledModel` with GPU and CPU delegates (BlazeFace, MobileNet SSD, MobileFaceNet).
  - **Dependency Injection**: Google Dagger Hilt (`2.60.1`).
  - **Storage**: Scoped Storage with DataStore Preferences (`androidx.datastore:1.1.3`) and MediaStore.

---

## 2. Testing Strategy & Commands

Automated testing is organized across local JVM unit tests, host-side Robolectric Compose tests, Compose preview screenshot testing, instrumented E2E tests, and JaCoCo coverage.

For detailed test architecture, file structures, reference image locations, and Gradle execution tasks, see:
👉 **[Testing Strategy & Execution Guide](file:///C:/Users/admin/FaceRecognition/docs/testing.md)**

### Quick Test Commands

- **Run all local tests:**
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Generate JaCoCo coverage report:**
  ```bash
  ./gradlew jacocoTestReport
  ```
- **Update screenshot reference images:**
  ```bash
  ./gradlew updateDebugScreenshotTest
  ```
- **Validate screenshot tests:**
  ```bash
  ./gradlew validateDebugScreenshotTest
  ```
- **Run device instrumented tests:**
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```

---

## 3. Important Implementation Guidelines for Agents

- **Gradle Execution**: When executing Gradle wrapper commands on Windows environments, use `BypassSandbox: true` due to daemon loopback socket requirements.
- **Dependency Injection**: Use constructor injection where possible. When adding new ViewModels, annotate with `@HiltViewModel`. For instrumented tests, use [`HiltTestRunner`](file:///C:/Users/admin/FaceRecognition/app/src/androidTest/java/com/example/detectionexample/HiltTestRunner.kt) and `@HiltAndroidTest`.
- **Navigation 3**: Screens are modeled as `@Serializable` subclasses of `NavKey`. Update [`Navigator.kt`](file:///C:/Users/admin/FaceRecognition/app/src/main/java/com/example/detectionexample/navigation/Navigator.kt) and [`NavigationState.kt`](file:///C:/Users/admin/FaceRecognition/app/src/main/java/com/example/detectionexample/navigation/NavigationState.kt) when introducing top-level tabs or nested flows.
- **Play Store Policies**: Strictly avoid declaring broad storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` on API > 28). Use Android Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia()`) and app-specific external storage (`context.getExternalFilesDir`).
