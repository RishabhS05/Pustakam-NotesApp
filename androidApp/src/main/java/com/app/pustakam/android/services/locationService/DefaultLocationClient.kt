package com.app.pustakam.android.services.locationService


import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.app.pustakam.android.extension.hasLocationPermission
import com.app.pustakam.android.hardware.location.LocationClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationClient {

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): Location =
        suspendCancellableCoroutine { continuation ->
            if (!context.hasLocationPermission()) {
                continuation.cancel(LocationClient.LocationException("Missing location permission"))
                return@suspendCancellableCoroutine
            }

            client.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        continuation.cancel(LocationClient.LocationException("Last location is null"))
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.cancel(exception)
                }
        }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location =
        suspendCancellableCoroutine { continuation ->
            if (!context.hasLocationPermission())
                continuation.cancel(LocationClient.LocationException("Missing location permission"))
            return@suspendCancellableCoroutine
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        continuation.cancel(
                            LocationClient.LocationException("Current location is null")
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.cancel(exception)
                }


        }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> = callbackFlow {
        // check for required permission
        if (!context.hasLocationPermission()) {
            throw LocationClient.LocationException("Missing location permission")
        }
        // location manager
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            throw LocationClient.LocationException("GPS is disabled")
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(interval)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.lastOrNull()?.let { location ->
                    launch { send(location) }
                }
            }
        }

        client.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            client.removeLocationUpdates(locationCallback)
        }
    }
}
