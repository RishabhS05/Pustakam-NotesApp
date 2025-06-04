package com.app.pustakam.android.hardware.location

import android.location.Location

import kotlinx.coroutines.flow.Flow

interface LocationClient {
   suspend fun getLastLocation() : Location
    suspend fun  getCurrentLocation(): Location
     fun getLocationUpdates(interval : Long) : Flow<Location>
     class LocationException(message: String) :  Exception()
 }
