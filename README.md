# CarbonPrint: A Personal Carbon Footprint Tracker Using Smartphone Activity Recognition

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2.10-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)
![Room Database](https://img.shields.io/badge/Database-Room%20(Offline--First)-orange.svg)
![Tests](https://img.shields.io/badge/Tests-88%20Passed-brightgreen.svg)

CarbonPrint is a personal carbon footprint tracking application designed to automatically classify daily human mobility, compute carbon emissions ($g\text{ CO}_2$), track physical fitness metrics, and encourage sustainable commuting through gamification, weekly carbon goals, and real-time transit analytics.

---

## 🏛️ System Architecture & Core Modules

CarbonPrint is built following modern Android Architecture Components and clean MVVM principles. The project is organized into four main functional modules/layers:

```
app/src/main/java/com/carbonprint/app/
├── data/           # 1. DATA LAYER
│   ├── local/      # Room DB, Entity models, Safe Migrations (v1->v2->v3), DAOs
│   ├── preferences/# Encrypted & typed user preferences (vehicle, baseline, weight)
│   └── repository/ # Single source of truth for Trips, Analytics, Goals & Badges
│
├── domain/         # 2. DOMAIN & SENSOR FUSION LAYER
│   ├── tracking/   # Multimodal TravelModeClassifier, MotionSensorTracker, StepTracker, StateMachine
│   ├── location/   # GpsLocationFilter (jitter/anomaly/teleport filtering), RouteProcessor
│   ├── battery/    # Zero-overhead BatteryMonitor & drain rate analytics
│   ├── calculator/ # IPCC/DEFRA CarbonEmissionCalculator, CalorieCalculator
│   └── manager/    # Dynamic BadgeEvaluator & weekly Goal management
│
├── service/        # 3. BACKGROUND SERVICES & RECEIVERS LAYER
│   ├── TripTrackingService.kt       # Foreground Service with ongoing notification
│   ├── ActivityRecognitionManager.kt# Google Play Services AR transition & periodic updates
│   ├── ActivityTransitionReceiver.kt# BroadcastReceiver for physical mobility transitions
│   └── worker/     # WorkManager daily rollups & goal reminders
│
└── ui/             # 4. PRESENTATION & COMPOSE UI LAYER
    ├── screens/    # Dashboard, Live Tracking, History, Trip Details, Analytics, Goals, Badges, Settings
    ├── components/ # Custom Gauges, Interactive Route Maps, Donut & Bar Charts, Metric Cards
    ├── navigation/ # Jetpack Compose Navigation & Bottom Navigation Bar
    └── theme/      # Material 3 Dynamic Theming & Typography
```

---

## 🚀 Key Features

### 1. Multimodal Automatic Travel-Mode Detection
- Automatically detects and tracks **Walking**, **Running**, **Cycling**, **In-Vehicle** (Car, Bus, Transit), and **Still** (Stationary).
- Intelligently fuses:
  - Google Play Services Activity Recognition API (confidence scoring).
  - GPS speed and location accuracy.
  - Accelerometer dynamic variance ($\sigma^2$) to separate vehicular road rumble from human heel strikes.
  - Gyroscope rotational angular velocity ($\text{rad/s}$).
  - Hardware step-counter cadence ($\text{steps/s}$).
- **Cycling vs. Walking Disambiguation**: Ensures cycling at 10–35 km/h with 0 steps is never misclassified as walking.
- **Walking vs. Running Differentiation**: Separates cadence ($> 2.2\text{ steps/s}$), speed ($> 7.5\text{ km/h}$), and impact variance ($\sigma^2 > 6.0$).

### 2. Activity Transitions & Stationary Debounce
- **Seamless In-Trip Transitions**: Smoothly switches between walking and vehicular travel within the same trip without splitting records or creating duplicate trips.
- **Stationary Debounce (120 seconds)**: Temporary pauses at traffic lights, pedestrian crossings, or bus stops do not terminate active tracking.
- **Temporary GPS Loss Protection**: In tunnels, subways, or indoor structures, tracking remains active using step counting and activity recognition.

### 3. Interactive Route Mapping
- Integrates OpenStreetMap (`osmdroid`) with zero API key dependencies.
- Displays recorded journeys with chronological polylines, start/destination pins, and auto-centering bounds.

### 4. High-Accuracy Hardware Step Counting
- Employs hardware `Sensor.TYPE_STEP_COUNTER` with baseline subtraction.
- Strictly classifies steps between walking and running; ignores steps during vehicular travel.
- Resilient against device reboots and hardware counter resets.

### 5. Zero-Overhead Battery Efficiency Monitoring
- Uses Android's sticky system broadcast `Intent.ACTION_BATTERY_CHANGED` without background polling.
- Records starting battery %, ending battery %, total decrease, and hourly drain rate ($\%/\text{hr}$).

### 6. Carbon Emissions & Gamification
- Calculates emitted $g\text{ CO}_2$ and avoided carbon emissions relative to internal combustion baseline vehicles.
- Weekly carbon budgets and milestone badges dynamically evaluated against cumulative Room database metrics.

---

## 🧪 Comprehensive Unit Testing

The project contains **88 passing unit tests** verifying all domain logic, sensor fusion, filters, and mathematical models:

- `AutomaticTravelModeDetectionTest`: 11 tests covering all activities, transitions, and GPS loss.
- `TrackingStateMachineTest`: 11 tests verifying state lifecycles and duplicate trip prevention.
- `StepCountAccuracyTest`: 11 tests validating biomechanical step counting and edge cases.
- `GpsLocationFilterTest`: 10 tests verifying jitter rejection, speed limits, and accuracy boundaries.
- `CarbonEmissionCalculatorTest`: 8 tests verifying IPCC/DEFRA emission formulas.
- `TravelModeMetricsTest`: 8 tests for mode metric aggregation.
- `CalorieCalculatorTest`: 7 tests validating MET energy expenditure.
- `RouteProcessorTest`: 6 tests validating path bounding boxes and simplification.
- `BatteryEfficiencyMonitoringTest`: 5 tests covering drain rate and multi-trip daily/weekly summaries.
- `BadgeEvaluatorTest` & `GoalCalculationTest`: 7 tests for gamification rules.
- `TrackingPermissionsTest`: 4 tests for Android 10–15 permission compliance.

---

## 🛠️ Build and Installation

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17+ (JDK 21/25 supported)
- Android SDK Platform 35
- Physical device running Android 10 (API 29) to Android 15 (API 35)

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Compiling the Debug APK
```bash
./gradlew assembleDebug
```
The output APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing via USB (ADB)
```bash
adb devices
adb install -r -d -t "app/build/outputs/apk/debug/app-debug.apk"
adb shell monkey -p com.carbonprint.app 1
```

---

## 📄 License
This project is developed as an academic capstone project.
