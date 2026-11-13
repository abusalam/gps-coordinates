package com.github.abusalam.gps_coordinates

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.abusalam.gps_coordinates.data.AppDatabase
import com.github.abusalam.gps_coordinates.data.BackupInfo
import com.github.abusalam.gps_coordinates.data.BackupManager
import com.github.abusalam.gps_coordinates.data.BackupResult
import com.github.abusalam.gps_coordinates.data.Coordinate
import com.github.abusalam.gps_coordinates.data.RestoreResult
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.util.Locale
import kotlin.math.abs

data class LocationState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val satellitesCount: Int = 19
)

data class NearbyWaypoint(
    val coordinate: Coordinate,
    val distanceMeters: Float
)

data class GnssSatelliteInfo(
    val id: Int,
    val constellation: String, // "GPS", "GLONASS", "GALILEO", "BEIDOU"
    val azimuth: Float, // 0..360 degrees
    val elevation: Float, // 0..90 degrees (90 is directly overhead)
    val snr: Float = 30f,
    val usedInFix: Boolean = true
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val database = AppDatabase.getDatabase(application)
    private val dao = database.coordinateDao()

    val backupManager = BackupManager(application, dao)

    private val prefs = application.getSharedPreferences("gps_coordinates_prefs", Context.MODE_PRIVATE)

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

    private val _satellites = MutableStateFlow(getDefaultSatellites())
    val satellites: StateFlow<List<GnssSatelliteInfo>> = _satellites.asStateFlow()

    private var sensorListener: SensorEventListener? = null
    private var gnssCallback: GnssStatus.Callback? = null

    val savedCoordinates: StateFlow<List<Coordinate>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _backupInfo = MutableStateFlow(BackupInfo())
    val backupInfo: StateFlow<BackupInfo> = _backupInfo.asStateFlow()

    private val _autoRestoreMessage = MutableStateFlow<String?>(null)
    val autoRestoreMessage: StateFlow<String?> = _autoRestoreMessage.asStateFlow()

    init {
        viewModelScope.launch {
            refreshBackupInfo()
            val autoRestoreResult = backupManager.checkAndAutoRestore()
            if (autoRestoreResult.success && autoRestoreResult.count > 0) {
                _autoRestoreMessage.value = "Automatically restored ${autoRestoreResult.count} waypoints from device backup."
            }
            refreshBackupInfo()
        }
    }

    fun startCompassUpdates() {
        if (sensorListener != null || sensorManager == null) return

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        if (rotationSensor != null) {
            sensorListener = object : SensorEventListener {
                private val rotationMatrix = FloatArray(9)
                private val orientation = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val azimuthRad = orientation[0]
                        var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                        azimuthDeg = (azimuthDeg + 360f) % 360f
                        _compassHeading.value = azimuthDeg
                    } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                        val azimuth = (event.values[0] + 360f) % 360f
                        _compassHeading.value = azimuth
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopCompassUpdates() {
        sensorListener?.let {
            sensorManager?.unregisterListener(it)
            sensorListener = null
        }
    }

    @SuppressLint("MissingPermission")
    fun registerGnssStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && locationManager != null && gnssCallback == null) {
            try {
                val callback = object : GnssStatus.Callback() {
                    override fun onSatelliteStatusChanged(status: GnssStatus) {
                        val count = status.satelliteCount
                        val list = mutableListOf<GnssSatelliteInfo>()
                        for (i in 0 until count) {
                            val constellation = when (status.getConstellationType(i)) {
                                GnssStatus.CONSTELLATION_GPS -> "GPS"
                                GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
                                GnssStatus.CONSTELLATION_GALILEO -> "GALILEO"
                                GnssStatus.CONSTELLATION_BEIDOU -> "BEIDOU"
                                else -> "GNSS"
                            }
                            list.add(
                                GnssSatelliteInfo(
                                    id = status.getSvid(i),
                                    constellation = constellation,
                                    azimuth = status.getAzimuthDegrees(i),
                                    elevation = status.getElevationDegrees(i),
                                    snr = status.getCn0DbHz(i),
                                    usedInFix = status.usedInFix(i)
                                )
                            )
                        }
                        if (list.isNotEmpty()) {
                            _satellites.value = list
                            _locationState.value = _locationState.value.copy(
                                satellitesCount = list.count { it.usedInFix }.coerceAtLeast(list.size)
                            )
                        }
                    }
                }
                gnssCallback = callback
                locationManager.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper()))
            } catch (_: SecurityException) {
                // Permission not granted or fallback to defaults
            } catch (_: Exception) {}
        }
    }

    private fun getDefaultSatellites(): List<GnssSatelliteInfo> {
        return listOf(
            GnssSatelliteInfo(2, "GPS", 42f, 65f, 38f),
            GnssSatelliteInfo(5, "GPS", 115f, 45f, 32f),
            GnssSatelliteInfo(12, "GPS", 210f, 75f, 41f),
            GnssSatelliteInfo(15, "GPS", 290f, 30f, 28f),
            GnssSatelliteInfo(21, "GPS", 335f, 55f, 35f),
            GnssSatelliteInfo(29, "GPS", 185f, 25f, 24f),
            GnssSatelliteInfo(3, "GLONASS", 65f, 50f, 33f),
            GnssSatelliteInfo(8, "GLONASS", 145f, 70f, 39f),
            GnssSatelliteInfo(19, "GLONASS", 240f, 40f, 30f),
            GnssSatelliteInfo(24, "GLONASS", 310f, 60f, 37f),
            GnssSatelliteInfo(7, "GALILEO", 20f, 80f, 42f),
            GnssSatelliteInfo(11, "GALILEO", 95f, 35f, 29f),
            GnssSatelliteInfo(18, "GALILEO", 160f, 55f, 36f),
            GnssSatelliteInfo(26, "GALILEO", 275f, 68f, 40f)
        )
    }

    fun refreshBackupInfo() {
        viewModelScope.launch {
            _backupInfo.value = backupManager.getBackupInfo()
        }
    }

    fun dismissAutoRestoreMessage() {
        _autoRestoreMessage.value = null
    }

    var isAutoRestoreEnabled: Boolean
        get() = backupManager.isAutoRestoreEnabled
        set(value) {
            backupManager.isAutoRestoreEnabled = value
        }

    var isAutoBackupEnabled: Boolean
        get() = backupManager.isAutoBackupEnabled
        set(value) {
            backupManager.isAutoBackupEnabled = value
        }

    var proximityThresholdMeters: Float
        get() = prefs.getFloat("key_proximity_threshold", 25.0f)
        set(value) = prefs.edit().putFloat("key_proximity_threshold", value).apply()

    var isProximityAlertEnabled: Boolean
        get() = prefs.getBoolean("key_proximity_alert_enabled", true)
        set(value) = prefs.edit().putBoolean("key_proximity_alert_enabled", value).apply()

    fun findNearbyWaypoint(lat: Double, lon: Double, customThreshold: Float? = null): NearbyWaypoint? {
        if (!isProximityAlertEnabled) return null
        val all = savedCoordinates.value
        if (all.isEmpty()) return null

        val threshold = customThreshold ?: proximityThresholdMeters
        var closest: Coordinate? = null
        var minDistance = Float.MAX_VALUE
        val results = FloatArray(1)

        for (coord in all) {
            Location.distanceBetween(lat, lon, coord.latitude, coord.longitude, results)
            val dist = results[0]
            if (dist < minDistance) {
                minDistance = dist
                closest = coord
            }
        }

        return if (closest != null && minDistance <= threshold) {
            NearbyWaypoint(closest, minDistance)
        } else null
    }

    fun backupNow(onResult: ((BackupResult) -> Unit)? = null) {
        viewModelScope.launch {
            val result = backupManager.createBackup()
            refreshBackupInfo()
            onResult?.invoke(result)
        }
    }

    fun restoreBackup(replaceExisting: Boolean = false, onResult: ((RestoreResult) -> Unit)? = null) {
        viewModelScope.launch {
            val backupFile = backupManager.getLatestBackupFile()
            if (backupFile == null) {
                onResult?.invoke(RestoreResult(false, "No backup file found on device", 0))
                return@launch
            }
            val result = backupManager.restoreFromFile(backupFile, replaceExisting)
            refreshBackupInfo()
            onResult?.invoke(result)
        }
    }

    fun restoreFromInputStream(inputStream: InputStream, replaceExisting: Boolean = false, onResult: ((RestoreResult) -> Unit)? = null) {
        viewModelScope.launch {
            val result = backupManager.restoreFromInputStream(inputStream, replaceExisting)
            refreshBackupInfo()
            onResult?.invoke(result)
        }
    }

    fun getBackupFileForSharing(): File? {
        return backupManager.getLatestBackupFile()
    }

    private fun triggerAutoBackupIfNeeded() {
        if (backupManager.isAutoBackupEnabled) {
            viewModelScope.launch {
                backupManager.createBackup()
                refreshBackupInfo()
            }
        }
    }

    fun saveCoordinate(
        label: String,
        lat: Double,
        lon: Double,
        alt: Double? = null,
        acc: Float? = null,
        tag: String? = null
    ) {
        viewModelScope.launch {
            dao.insert(
                Coordinate(
                    label = label,
                    latitude = lat,
                    longitude = lon,
                    altitude = alt,
                    accuracy = acc,
                    tag = tag
                )
            )
            triggerAutoBackupIfNeeded()
        }
    }

    fun overwriteCoordinate(
        existing: Coordinate,
        newLabel: String,
        lat: Double,
        lon: Double,
        alt: Double? = null,
        acc: Float? = null,
        tag: String? = null
    ) {
        viewModelScope.launch {
            dao.update(
                existing.copy(
                    label = newLabel,
                    latitude = lat,
                    longitude = lon,
                    altitude = alt,
                    accuracy = acc,
                    tag = tag,
                    timestamp = System.currentTimeMillis()
                )
            )
            triggerAutoBackupIfNeeded()
        }
    }

    fun deleteCoordinate(coordinate: Coordinate) {
        viewModelScope.launch {
            dao.delete(coordinate.id)
            triggerAutoBackupIfNeeded()
        }
    }

    fun updateCoordinateLabel(coordinate: Coordinate, newLabel: String) {
        viewModelScope.launch {
            dao.update(coordinate.copy(label = newLabel))
            triggerAutoBackupIfNeeded()
        }
    }

    fun togglePin(coordinate: Coordinate) {
        viewModelScope.launch {
            dao.update(coordinate.copy(isPinned = !coordinate.isPinned))
            triggerAutoBackupIfNeeded()
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        callbackFlow {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    trySend(
                        LocationState(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = if (location.hasAltitude()) location.altitude else null,
                            accuracy = if (location.hasAccuracy()) location.accuracy else null,
                            speed = if (location.hasSpeed()) location.speed * 3.6f else null, // m/s to km/h
                            bearing = if (location.hasBearing()) location.bearing else null,
                            satellitesCount = _satellites.value.count { it.usedInFix }.coerceAtLeast(_locationState.value.satellitesCount)
                        )
                    )
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                close(e)
            }

            awaitClose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }.catch {
            // Handle exceptions if needed
        }.onEach { location ->
            _locationState.value = location
        }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        stopCompassUpdates()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssCallback != null) {
            locationManager?.unregisterGnssStatusCallback(gnssCallback!!)
            gnssCallback = null
        }
    }

    companion object {
        fun formatDMS(latitude: Double?, longitude: Double?): String {
            if (latitude == null || longitude == null) return "N/A"
            val latDMS = toDMS(latitude, isLatitude = true)
            val lonDMS = toDMS(longitude, isLatitude = false)
            return "DMS: $latDMS, $lonDMS"
        }

        private fun toDMS(coordinate: Double, isLatitude: Boolean): String {
            val absolute = abs(coordinate)
            val degrees = absolute.toInt()
            val minutesNotTruncated = (absolute - degrees) * 60
            val minutes = minutesNotTruncated.toInt()
            val seconds = (minutesNotTruncated - minutes) * 60
            val direction = if (isLatitude) {
                if (coordinate >= 0) "N" else "S"
            } else {
                if (coordinate >= 0) "E" else "W"
            }
            return String.format(Locale.US, "%d° %02d' %04.1f\" %s", degrees, minutes, seconds, direction)
        }
    }
}
