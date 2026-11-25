package com.carbonprint.app.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.carbonprint.app.CarbonPrintApp
import com.carbonprint.app.domain.battery.BatteryMonitor
import com.carbonprint.app.domain.calculator.CalorieCalculator
import com.carbonprint.app.domain.calculator.CarbonEmissionCalculator
import com.carbonprint.app.domain.location.FilterResult
import com.carbonprint.app.domain.location.GpsLocationFilter
import com.carbonprint.app.domain.location.GpsPoint
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.domain.model.VehicleType
import com.carbonprint.app.domain.tracking.MotionSensorTracker
import com.carbonprint.app.domain.tracking.SensorSnapshot
import com.carbonprint.app.domain.tracking.StepTracker
import com.carbonprint.app.domain.tracking.TravelModeClassifier
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service responsible for active GPS trip tracking, location filtering,
 * real-time carbon emission calculation, and ongoing status notifications.
 */
class TripTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var currentTripId: Long = -1L
    private var currentMode: TransportMode = TransportMode.UNKNOWN
    private var currentSubMode: String = "AUTO"
    private var currentVehicleType: VehicleType? = null
    private var isAutoDetected: Boolean = false
    private var stepTracker: StepTracker? = null
    private var motionSensorTracker: MotionSensorTracker? = null
    private val travelModeClassifier = TravelModeClassifier()

    private var accumulatedDistanceMeters: Double = 0.0
    private var accumulatedCo2Grams: Double = 0.0
    private var accumulatedCo2SavedGrams: Double = 0.0
    private var accumulatedCalories: Double = 0.0
    private var elapsedDurationSeconds: Long = 0L
    private var walkingDurationSeconds: Long = 0L
    private var runningDurationSeconds: Long = 0L
    private var startBatteryPercentage: Int? = null
    private var lastValidLocation: Location? = null
    private var lastValidGpsPoint: GpsPoint? = null
    private var currentSpeedKmh: Double = 0.0
    private var maxSpeedKmh: Double = 0.0

    private var stillDebounceJob: Job? = null
    private var isStationaryPaused: Boolean = false

    companion object {
        private const val TAG = "TripTrackingService"

        const val ACTION_START_TRACKING = "com.carbonprint.app.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.carbonprint.app.ACTION_STOP_TRACKING"
        const val ACTION_PAUSE_TRACKING = "com.carbonprint.app.ACTION_PAUSE_TRACKING"
        const val ACTION_NOTIFY_STILL = "com.carbonprint.app.ACTION_NOTIFY_STILL"
        const val ACTION_RESUME_MOVEMENT = "com.carbonprint.app.ACTION_RESUME_MOVEMENT"
        const val ACTION_NOTIFY_ACTIVITY_DETECTED = "com.carbonprint.app.ACTION_NOTIFY_ACTIVITY_DETECTED"

        const val EXTRA_TRANSPORT_MODE = "extra_transport_mode"
        const val EXTRA_ACTIVITY_CONFIDENCE = "extra_activity_confidence"
        const val EXTRA_IS_AUTO_DETECTED = "extra_is_auto_detected"
        const val EXTRA_SUB_MODE = "extra_sub_mode"
        const val EXTRA_VEHICLE_TYPE = "extra_vehicle_type"

        // 2-minute stationary confirmation delay to prevent red lights/brief pauses from killing trips
        const val STILL_DEBOUNCE_DELAY_MILLIS = 120_000L

        // Observable StateFlows for UI synchronization
        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        private val _currentModeState = MutableStateFlow(TransportMode.UNKNOWN)
        val currentModeState: StateFlow<TransportMode> = _currentModeState.asStateFlow()

        private val _distanceMetersState = MutableStateFlow(0.0)
        val distanceMetersState: StateFlow<Double> = _distanceMetersState.asStateFlow()

        private val _durationSecondsState = MutableStateFlow(0L)
        val durationSecondsState: StateFlow<Long> = _durationSecondsState.asStateFlow()

        private val _co2GramsState = MutableStateFlow(0.0)
        val co2GramsState: StateFlow<Double> = _co2GramsState.asStateFlow()

        private val _co2SavedGramsState = MutableStateFlow(0.0)
        val co2SavedGramsState: StateFlow<Double> = _co2SavedGramsState.asStateFlow()

        private val _caloriesState = MutableStateFlow(0.0)
        val caloriesState: StateFlow<Double> = _caloriesState.asStateFlow()

        private val _speedKmhState = MutableStateFlow(0.0)
        val speedKmhState: StateFlow<Double> = _speedKmhState.asStateFlow()

        private val _stepCountState = MutableStateFlow(0)
        val stepCountState: StateFlow<Int> = _stepCountState.asStateFlow()

        private val _walkingStepCountState = MutableStateFlow(0)
        val walkingStepCountState: StateFlow<Int> = _walkingStepCountState.asStateFlow()

        private val _runningStepCountState = MutableStateFlow(0)
        val runningStepCountState: StateFlow<Int> = _runningStepCountState.asStateFlow()

        private val _walkingDurationSecondsState = MutableStateFlow(0L)
        val walkingDurationSecondsState: StateFlow<Long> = _walkingDurationSecondsState.asStateFlow()

        private val _runningDurationSecondsState = MutableStateFlow(0L)
        val runningDurationSecondsState: StateFlow<Long> = _runningDurationSecondsState.asStateFlow()

        private val _startBatteryPctState = MutableStateFlow<Int?>(null)
        val startBatteryPctState: StateFlow<Int?> = _startBatteryPctState.asStateFlow()

        private val _batteryDropPctState = MutableStateFlow<Int?>(null)
        val batteryDropPctState: StateFlow<Int?> = _batteryDropPctState.asStateFlow()

        private val _subModeState = MutableStateFlow("AUTO")
        val subModeState: StateFlow<String> = _subModeState.asStateFlow()

        fun startTracking(
            context: Context,
            mode: TransportMode,
            isAuto: Boolean = false,
            subMode: String = "AUTO",
            vehicleType: VehicleType? = null
        ) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_TRANSPORT_MODE, mode.name)
                putExtra(EXTRA_IS_AUTO_DETECTED, isAuto)
                putExtra(EXTRA_SUB_MODE, subMode)
                if (vehicleType != null) {
                    putExtra(EXTRA_VEHICLE_TYPE, vehicleType.name)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }

        fun notifyStillDetected(context: Context) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_NOTIFY_STILL
            }
            context.startService(intent)
        }

        fun notifyMovementResumed(context: Context, mode: TransportMode) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_RESUME_MOVEMENT
                putExtra(EXTRA_TRANSPORT_MODE, mode.name)
            }
            context.startService(intent)
        }

        fun notifyActivityDetected(context: Context, mode: TransportMode, confidence: Int = 100) {
            val intent = Intent(context, TripTrackingService::class.java).apply {
                action = ACTION_NOTIFY_ACTIVITY_DETECTED
                putExtra(EXTRA_TRANSPORT_MODE, mode.name)
                putExtra(EXTRA_ACTIVITY_CONFIDENCE, confidence)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        stepTracker = StepTracker(this).apply {
            onStepsChanged = { wSteps, rSteps ->
                _walkingStepCountState.value = wSteps
                _runningStepCountState.value = rSteps
                _stepCountState.value = wSteps + rSteps
            }
        }
        motionSensorTracker = MotionSensorTracker(this)
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val modeStr = intent.getStringExtra(EXTRA_TRANSPORT_MODE)
                val mode = TransportMode.fromString(modeStr)
                val auto = intent.getBooleanExtra(EXTRA_IS_AUTO_DETECTED, false)
                val subMode = intent.getStringExtra(EXTRA_SUB_MODE) ?: if (auto) "AUTO_${mode.name}" else "MANUAL_${mode.name}"
                val vehicleTypeStr = intent.getStringExtra(EXTRA_VEHICLE_TYPE)
                val vehicleType = vehicleTypeStr?.let { VehicleType.fromString(it) }
                handleStartTracking(mode, auto, subMode, vehicleType)
            }
            ACTION_STOP_TRACKING -> {
                handleStopTracking()
            }
            ACTION_PAUSE_TRACKING -> {
                pauseGpsUpdates()
            }
            ACTION_NOTIFY_STILL -> {
                handleStillDetected()
            }
            ACTION_RESUME_MOVEMENT -> {
                val modeStr = intent.getStringExtra(EXTRA_TRANSPORT_MODE)
                val mode = TransportMode.fromString(modeStr)
                handleMovementResumed(mode)
            }
            ACTION_NOTIFY_ACTIVITY_DETECTED -> {
                val modeStr = intent.getStringExtra(EXTRA_TRANSPORT_MODE)
                val mode = TransportMode.fromString(modeStr)
                val confidence = intent.getIntExtra(EXTRA_ACTIVITY_CONFIDENCE, 100)
                handleActivityDetected(mode, confidence)
            }
            else -> {
                if (_isTracking.value) {
                    // Update ongoing notification if restarted
                    updateNotification()
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun handleStillDetected() {
        if (!_isTracking.value) return

        // Only auto-detected trips are subject to stationary timeout debouncing.
        // User-initiated manual tracking is NOT auto-stopped at red lights or pauses.
        if (!isAutoDetected) {
            Log.d(TAG, "Manual tracking active; ignoring STILL transition at stop/signal.")
            return
        }

        if (stillDebounceJob?.isActive == true) {
            Log.d(TAG, "Already in stationary debounce window. Continuing countdown.")
            return
        }

        Log.i(TAG, "STILL transition detected. Starting $STILL_DEBOUNCE_DELAY_MILLIS ms debounce window.")
        isStationaryPaused = true
        updateNotification()

        stillDebounceJob = serviceScope.launch {
            delay(STILL_DEBOUNCE_DELAY_MILLIS)
            Log.i(TAG, "Stationary debounce timeout expired. Finalizing auto-tracked trip.")
            handleStopTracking()
        }
    }

    private fun handleMovementResumed(mode: TransportMode) {
        handleActivityDetected(mode, confidence = 100)
    }

    private fun handleActivityDetected(mode: TransportMode, confidence: Int) {
        if (!_isTracking.value) return

        if (mode == TransportMode.STILL) {
            handleStillDetected()
            return
        }

        if (stillDebounceJob?.isActive == true) {
            Log.i(TAG, "Movement resumed ($mode, $confidence%) within debounce window. Canceling stationary timeout.")
            stillDebounceJob?.cancel()
            stillDebounceJob = null
        }
        isStationaryPaused = false

        if (!isAutoDetected) {
            // For explicit manual tracking, keep user-selected mode without auto-overriding
            return
        }

        // Multimodal fusion classification
        val cadence = stepTracker?.getRecentStepCadence() ?: 0.0
        val variance = motionSensorTracker?.getCurrentVariance()
        val gyro = motionSensorTracker?.getCurrentGyroMagnitude()

        val snapshot = SensorSnapshot(
            detectedActivity = mode,
            activityConfidence = confidence,
            gpsSpeedKmh = if (lastValidGpsPoint != null) currentSpeedKmh else null,
            isGpsValid = lastValidGpsPoint != null,
            stepCadencePerSecond = cadence,
            accelVariance = variance,
            gyroMagnitude = gyro
        )

        val classification = travelModeClassifier.classify(snapshot)
        if (classification.isTransition && classification.confirmedMode != currentMode && classification.confirmedMode != TransportMode.UNKNOWN) {
            handleModeTransition(classification.confirmedMode, "AUTO_${classification.confirmedMode.name}")
        }
    }

    private fun handleModeTransition(newMode: TransportMode, newSubMode: String) {
        if (currentMode == newMode) return

        Log.i(TAG, "Mode transition on active trip id=$currentTripId: $currentMode -> $newMode ($newSubMode)")
        currentMode = newMode
        currentSubMode = newSubMode
        _currentModeState.value = newMode
        _subModeState.value = newSubMode

        stepTracker?.onActivityModeChanged(newMode)

        val tripId = currentTripId
        val app = application as CarbonPrintApp
        if (tripId > 0) {
            serviceScope.launch(Dispatchers.IO) {
                app.tripRepository.updateTripMode(tripId, newMode, newSubMode)
            }
        }
        updateNotification()
    }

    private fun handleStartTracking(
        mode: TransportMode,
        auto: Boolean,
        subMode: String = if (auto) "AUTO_${mode.name}" else "MANUAL_${mode.name}",
        vehicleType: VehicleType? = null
    ) {
        if (_isTracking.value) {
            if (currentMode != mode) {
                handleModeTransition(mode, subMode)
            }
            return
        }

        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot start tracking service: ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION not granted.")
            stopSelf()
            return
        }

        if (stillDebounceJob?.isActive == true) {
            stillDebounceJob?.cancel()
            stillDebounceJob = null
        }
        isStationaryPaused = false
        lastValidGpsPoint = null

        currentMode = mode
        currentSubMode = subMode
        currentVehicleType = vehicleType
        isAutoDetected = auto
        accumulatedDistanceMeters = 0.0
        accumulatedCo2Grams = 0.0
        accumulatedCo2SavedGrams = 0.0
        accumulatedCalories = 0.0
        elapsedDurationSeconds = 0L
        walkingDurationSeconds = 0L
        runningDurationSeconds = 0L
        lastValidLocation = null
        currentSpeedKmh = 0.0
        maxSpeedKmh = 0.0

        startBatteryPercentage = BatteryMonitor.getBatteryPercentage(this)
        _startBatteryPctState.value = startBatteryPercentage
        _batteryDropPctState.value = 0

        travelModeClassifier.reset(mode)
        stepTracker?.reset(mode)
        stepTracker?.startListening()
        motionSensorTracker?.startListening()

        _currentModeState.value = mode
        _subModeState.value = subMode
        _isTracking.value = true
        _distanceMetersState.value = 0.0
        _durationSecondsState.value = 0L
        _walkingDurationSecondsState.value = 0L
        _runningDurationSecondsState.value = 0L
        _co2GramsState.value = 0.0
        _co2SavedGramsState.value = 0.0
        _caloriesState.value = 0.0
        _speedKmhState.value = 0.0
        _walkingStepCountState.value = 0
        _runningStepCountState.value = 0
        _stepCountState.value = 0

        // Start Foreground immediately to satisfy Android background requirements
        val initialNotification = NotificationHelper.buildTrackingNotification(
            context = this,
            mode = mode,
            distanceMeters = 0.0,
            durationSeconds = 0L,
            co2Grams = 0.0,
            isAutoDetected = auto,
            stepCount = 0
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID_TRACKING,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_TRACKING, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            stopSelf()
            return
        }

        val app = application as CarbonPrintApp
        val startBat = startBatteryPercentage
        serviceScope.launch(Dispatchers.IO) {
            currentTripId = app.tripRepository.startTrip(
                mode = mode,
                subMode = subMode,
                walkingSteps = 0,
                runningSteps = 0,
                walkingDurationSeconds = 0L,
                runningDurationSeconds = 0L,
                startBatteryPct = startBat
            )
            Log.i(TAG, "Started trip tracking id=$currentTripId mode=$mode subMode=$subMode battery=$startBat%")
        }

        startGpsUpdates()
        startDurationTicker()
    }

    private fun startGpsUpdates() {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Cannot start GPS tracking: Location permission missing.")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(2.0f) // Jitter reduction filter
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    processNewLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates", e)
            stopSelf()
        }
    }

    private fun processNewLocation(location: Location) {
        val candidate = GpsPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            timestampMillis = location.time,
            speedMps = if (location.hasSpeed()) location.speed else null,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null
        )

        when (val result = GpsLocationFilter.evaluate(candidate, lastValidGpsPoint)) {
            is FilterResult.Accepted -> {
                val deltaDist = result.distanceMeters
                accumulatedDistanceMeters += deltaDist
                currentSpeedKmh = result.calculatedSpeedKmh
                if (currentSpeedKmh > maxSpeedKmh) maxSpeedKmh = currentSpeedKmh
                lastValidGpsPoint = result.point
                lastValidLocation = location

                // Multimodal sensor fusion check during GPS updates
                if (isAutoDetected) {
                    val cadence = stepTracker?.getRecentStepCadence() ?: 0.0
                    val variance = motionSensorTracker?.getCurrentVariance()
                    val gyro = motionSensorTracker?.getCurrentGyroMagnitude()
                    val snapshot = SensorSnapshot(
                        detectedActivity = currentMode,
                        activityConfidence = 80,
                        gpsSpeedKmh = currentSpeedKmh,
                        isGpsValid = true,
                        stepCadencePerSecond = cadence,
                        accelVariance = variance,
                        gyroMagnitude = gyro
                    )
                    val classification = travelModeClassifier.classify(snapshot)
                    if (classification.isTransition && classification.confirmedMode != currentMode && classification.confirmedMode != TransportMode.UNKNOWN) {
                        handleModeTransition(classification.confirmedMode, "AUTO_${classification.confirmedMode.name}")
                    }
                }

                val app = application as CarbonPrintApp
                val vehicleType = app.preferencesManager.selectedVehicleType
                val userWeight = app.preferencesManager.userWeightKg
                val effectiveVehicleType = currentVehicleType ?: if (currentSubMode.equals("MOTORCYCLE", ignoreCase = true)) VehicleType.MOTORCYCLE else vehicleType

                // Incremental segment emission and calorie accumulation
                val deltaCo2 = CarbonEmissionCalculator.calculateEmissionGrams(
                    mode = currentMode,
                    distanceMeters = deltaDist,
                    vehicleType = effectiveVehicleType
                )
                val deltaCo2Saved = CarbonEmissionCalculator.calculateSavedEmissionGrams(
                    mode = currentMode,
                    distanceMeters = deltaDist,
                    vehicleType = effectiveVehicleType
                )
                val deltaCal = CalorieCalculator.calculateCaloriesBurned(
                    mode = currentMode,
                    distanceMeters = deltaDist,
                    userWeightKg = userWeight
                )
                accumulatedCo2Grams += deltaCo2
                accumulatedCo2SavedGrams += deltaCo2Saved
                accumulatedCalories += deltaCal
            }
            is FilterResult.RejectedInaccurate -> {
                Log.v(TAG, "Rejected inaccurate GPS fix: accuracy=${result.accuracyMeters}m > ${result.thresholdMeters}m")
                return
            }
            is FilterResult.RejectedTimeAnomaly -> {
                Log.w(TAG, "Rejected chronological time anomaly: deltaMillis=${result.deltaMillis}")
                return
            }
            is FilterResult.RejectedJitter -> {
                Log.v(TAG, "Suppressed stationary GPS jitter: distance=${result.distanceMeters}m")
                return
            }
            is FilterResult.RejectedImplausibleSpeed -> {
                Log.w(TAG, "Rejected unrealistic GPS teleport: speed=${result.calculatedSpeedKmh}km/h distance=${result.distanceMeters}m")
                return
            }
        }

        val app = application as CarbonPrintApp

        // Step tracking for walking and running
        val currentWSteps = stepTracker?.getWalkingSteps() ?: 0
        val currentRSteps = stepTracker?.getRunningSteps() ?: 0
        val currentSteps = currentWSteps + currentRSteps

        _walkingStepCountState.value = currentWSteps
        _runningStepCountState.value = currentRSteps
        _stepCountState.value = currentSteps

        // Update StateFlows
        _distanceMetersState.value = accumulatedDistanceMeters
        _co2GramsState.value = accumulatedCo2Grams
        _co2SavedGramsState.value = accumulatedCo2SavedGrams
        _caloriesState.value = accumulatedCalories
        _speedKmhState.value = currentSpeedKmh

        // Persist waypoint and update progress in Room asynchronously
        val tripId = currentTripId
        val curWSteps = currentWSteps
        val curRSteps = currentRSteps
        val curWDuration = walkingDurationSeconds
        val curRDuration = runningDurationSeconds
        val curCo2 = accumulatedCo2Grams
        val curCo2Saved = accumulatedCo2SavedGrams
        val curCal = accumulatedCalories
        if (tripId > 0) {
            serviceScope.launch(Dispatchers.IO) {
                app.tripRepository.addWaypoint(
                    tripId = tripId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speedMps = location.speed,
                    accuracyMeters = location.accuracy,
                    timestamp = location.time
                )
                app.tripRepository.updateTripProgress(
                    tripId = tripId,
                    distanceMeters = accumulatedDistanceMeters,
                    durationSeconds = elapsedDurationSeconds,
                    currentSpeedKmh = currentSpeedKmh,
                    stepCount = currentSteps,
                    walkingSteps = curWSteps,
                    runningSteps = curRSteps,
                    walkingDurationSeconds = curWDuration,
                    runningDurationSeconds = curRDuration,
                    overrideCo2Grams = curCo2,
                    overrideCo2SavedGrams = curCo2Saved,
                    overrideCalories = curCal
                )
            }
        }

        updateNotification()
    }

    private fun startDurationTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                elapsedDurationSeconds++
                _durationSecondsState.value = elapsedDurationSeconds

                if (!isStationaryPaused) {
                    if (currentMode == TransportMode.WALKING) {
                        walkingDurationSeconds++
                        _walkingDurationSecondsState.value = walkingDurationSeconds
                    } else if (currentMode == TransportMode.RUNNING) {
                        runningDurationSeconds++
                        _runningDurationSecondsState.value = runningDurationSeconds
                    }
                }

                val wSteps = stepTracker?.getWalkingSteps() ?: 0
                val rSteps = stepTracker?.getRunningSteps() ?: 0
                _walkingStepCountState.value = wSteps
                _runningStepCountState.value = rSteps
                _stepCountState.value = wSteps + rSteps

                // Observe battery status every 30 seconds without frequent polling
                if (elapsedDurationSeconds % 30L == 0L) {
                    val currentBat = BatteryMonitor.getBatteryPercentage(this@TripTrackingService)
                    _batteryDropPctState.value = BatteryMonitor.calculateBatteryDrop(startBatteryPercentage, currentBat)
                }

                if (elapsedDurationSeconds % 5L == 0L) {
                    updateNotification()
                }
            }
        }
    }

    private fun updateNotification() {
        if (!_isTracking.value) return
        val notification = NotificationHelper.buildTrackingNotification(
            context = this,
            mode = currentMode,
            distanceMeters = accumulatedDistanceMeters,
            durationSeconds = elapsedDurationSeconds,
            co2Grams = _co2GramsState.value,
            isAutoDetected = isAutoDetected,
            isStationaryPaused = isStationaryPaused,
            stepCount = _stepCountState.value
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TRACKING, notification)
    }

    private fun pauseGpsUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        Log.i(TAG, "GPS updates paused to conserve battery while stationary.")
    }

    private fun handleStopTracking() {
        if (!_isTracking.value) {
            stopSelf()
            return
        }

        if (stillDebounceJob?.isActive == true) {
            stillDebounceJob?.cancel()
            stillDebounceJob = null
        }
        isStationaryPaused = false
        lastValidGpsPoint = null

        pauseGpsUpdates()
        tickerJob?.cancel()
        tickerJob = null
        stepTracker?.stopListening()
        motionSensorTracker?.stopListening()

        val finalWSteps = stepTracker?.getWalkingSteps() ?: 0
        val finalRSteps = stepTracker?.getRunningSteps() ?: 0
        val finalSteps = finalWSteps + finalRSteps
        val finalWDuration = walkingDurationSeconds
        val finalRDuration = runningDurationSeconds

        val endBatteryPercentage = BatteryMonitor.getBatteryPercentage(this)
        val finalBatteryDrop = BatteryMonitor.calculateBatteryDrop(startBatteryPercentage, endBatteryPercentage)
        _batteryDropPctState.value = finalBatteryDrop

        val avgSpeed = if (elapsedDurationSeconds > 0) (accumulatedDistanceMeters / 1000.0) / (elapsedDurationSeconds / 3600.0) else 0.0

        val tripId = currentTripId
        val lastLoc = lastValidLocation
        val app = application as CarbonPrintApp

        serviceScope.launch(Dispatchers.IO) {
            if (tripId > 0) {
                val completedTrip = app.tripRepository.completeTrip(
                    tripId = tripId,
                    endLat = lastLoc?.latitude,
                    endLng = lastLoc?.longitude,
                    stepCount = finalSteps,
                    walkingSteps = finalWSteps,
                    runningSteps = finalRSteps,
                    walkingDurationSeconds = finalWDuration,
                    runningDurationSeconds = finalRDuration,
                    averageSpeedKmh = avgSpeed,
                    maxSpeedKmh = maxSpeedKmh,
                    endBatteryPct = endBatteryPercentage,
                    batteryDropPct = finalBatteryDrop
                )

                if (completedTrip != null) {
                    // 1. Aggregate to daily summary
                    app.analyticsRepository.recordTripToDailySummary(completedTrip)

                    // 2. Update weekly goal progress
                    val updatedGoal = app.goalRepository.updateCurrentWeekGoalProgress()

                    // 3. Evaluate gamification badges against real cumulative database statistics
                    val newBadges = app.badgeRepository.evaluateAllBadgesFromDatabase(
                        isGoalAchieved = updatedGoal.isAchieved
                    )

                    for (badge in newBadges) {
                        NotificationHelper.showBadgeUnlockedNotification(
                            this@TripTrackingService,
                            badge.title,
                            badge.description
                        )
                    }
                }
            }

            _isTracking.value = false
            _subModeState.value = "AUTO"
            currentTripId = -1L
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Trip tracking stopped and finalized.")
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    override fun onDestroy() {
        stepTracker?.stopListening()
        motionSensorTracker?.stopListening()
        stillDebounceJob?.cancel()
        stillDebounceJob = null
        pauseGpsUpdates()
        tickerJob?.cancel()
        _isTracking.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
