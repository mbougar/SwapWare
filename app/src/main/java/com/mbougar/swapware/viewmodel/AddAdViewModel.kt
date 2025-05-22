package com.mbougar.swapware.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mbougar.swapware.data.model.NewAdData
import com.mbougar.swapware.data.repository.AdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class AddAdUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val sellerLocation: String = "",
    val isFetchingLocation: Boolean = false,
    val locationPermissionRequested: Boolean = false
)

sealed class AddAdScreenEvent {
    data class ShowSnackbar(val message: String, val durationLong: Boolean = false) : AddAdScreenEvent()
    object RequestLocationPermission : AddAdScreenEvent()
}


@HiltViewModel
class AddAdViewModel @Inject constructor(
    private val adRepository: AdRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAdUiState())
    val uiState: StateFlow<AddAdUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddAdScreenEvent>()
    val eventFlow = _eventFlow.asSharedFlow()


    fun onSellerLocationChange(location: String) {
        _uiState.update { it.copy(sellerLocation = location) }
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            viewModelScope.launch {
                _eventFlow.emit(AddAdScreenEvent.RequestLocationPermission)
            }
            return
        }


        _uiState.update { it.copy(isFetchingLocation = true) }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(application, Locale.getDefault())
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses?.isNotEmpty() == true) {
                        val address = addresses[0]
                        val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Unknown area"
                        _uiState.update { it.copy(sellerLocation = city) }
                        Log.d("AddAdViewModel", "Current location city: $city")
                    } else {
                        viewModelScope.launch { _eventFlow.emit(AddAdScreenEvent.ShowSnackbar("Could not determine city from current location.")) }
                    }
                } catch (e: Exception) {
                    viewModelScope.launch { _eventFlow.emit(AddAdScreenEvent.ShowSnackbar("Error determining city. Please enter manually.")) }
                    Log.e("AddAdViewModel", "Geocoder error from current location", e)
                }
            } else {
                viewModelScope.launch { _eventFlow.emit(AddAdScreenEvent.ShowSnackbar("Failed to get current location. Ensure location services are enabled or enter manually.", true)) }
                Log.d("AddAdViewModel", "getCurrentLocation returned null.")
            }
            _uiState.update { it.copy(isFetchingLocation = false) }
        }.addOnFailureListener { exception ->
            viewModelScope.launch { _eventFlow.emit(AddAdScreenEvent.ShowSnackbar("Error fetching current location: ${exception.message}")) }
            Log.e("AddAdViewModel", "getCurrentLocation failed", exception)
            _uiState.update { it.copy(isFetchingLocation = false) }
        }.addOnCanceledListener {
            viewModelScope.launch { _eventFlow.emit(AddAdScreenEvent.ShowSnackbar("Location fetch canceled.")) }
            _uiState.update { it.copy(isFetchingLocation = false) }
        }
    }


    fun addAd(
        title: String,
        description: String,
        priceStr: String,
        category: String,
        imageUri: Uri?
    ) {
        val currentSellerLocation = _uiState.value.sellerLocation
        val price = priceStr.toDoubleOrNull()

        if (title.isBlank() || description.isBlank() || price == null || category.isBlank() || currentSellerLocation.isBlank()) {
            _uiState.update { it.copy(error = "Please fill all fields correctly, including location.") }
            return
        }
        if (price <= 0) {
            _uiState.update { it.copy(error = "Price must be positive.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val adData = NewAdData(
                title = title,
                description = description,
                price = price,
                category = category,
                imageUri = imageUri,
                sellerLocation = currentSellerLocation
            )

            val result = adRepository.addAd(adData)

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to add ad.") }
            }
        }
    }

    fun resetState() {
        _uiState.value = AddAdUiState()
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}