package com.github.abusalam.gps_coordinates

import android.annotation.SuppressLint
import android.app.Application
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.abusalam.gps_coordinates.data.AppDatabase
import com.github.abusalam.gps_coordinates.data.Coordinate
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

data class LocationState(
    val latitude: Double? = null,
    val longitude: Double? = null
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.coordinateDao()

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    val savedCoordinates: StateFlow<List<Coordinate>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveCoordinate(label: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            dao.insert(Coordinate(label = label, latitude = lat, longitude = lon))
        }
    }

    fun deleteCoordinate(coordinate: Coordinate) {
        viewModelScope.launch {
            dao.delete(coordinate.id)
        }
    }

    fun updateCoordinateLabel(coordinate: Coordinate, newLabel: String) {
        viewModelScope.launch {
            dao.update(coordinate.copy(label = newLabel))
        }
    }

    fun togglePin(coordinate: Coordinate) {
        viewModelScope.launch {
            dao.update(coordinate.copy(isPinned = !coordinate.isPinned))
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        callbackFlow {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    for (location in result.locations) {
                        trySend(LocationState(location.latitude, location.longitude))
                    }
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
        }.catch { e ->
            // Handle exceptions if needed
        }.onEach { location ->
            _locationState.value = location
        }.launchIn(viewModelScope)
    }
}
