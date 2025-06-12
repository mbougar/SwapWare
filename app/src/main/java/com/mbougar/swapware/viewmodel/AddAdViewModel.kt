package com.mbougar.swapware.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.local.PoblacionDao
import com.mbougar.swapware.data.local.PoblacionLocation
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
import javax.inject.Inject

/**
 * Guarda el estado de la pantalla de "Añadir Anuncio".
 */
data class AddAdUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val selectedPoblacion: PoblacionLocation? = null,
)

/**
 * Eventos que el ViewModel puede enviar a la UI, como mostrar un mensaje.
 */
sealed class AddAdScreenEvent {
    data class ShowSnackbar(val message: String, val durationLong: Boolean = false) : AddAdScreenEvent()
}

/**
 * ViewModel para la pantalla de añadir un nuevo anuncio.
 */
@HiltViewModel
class AddAdViewModel @Inject constructor(
    private val adRepository: AdRepository,
    private val poblacionDao: PoblacionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAdUiState())
    val uiState: StateFlow<AddAdUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddAdScreenEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _locationSearchQuery = MutableStateFlow("")
    val locationSearchQuery: StateFlow<String> = _locationSearchQuery.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<PoblacionLocation>>(emptyList())
    val locationSuggestions: StateFlow<List<PoblacionLocation>> = _locationSuggestions.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    /**
     * Se llama cuando el usuario cambia el texto en el buscador de ubicación.
     * @param query El nuevo texto.
     */
    fun onLocationSearchQueryChanged(query: String) {
        _locationSearchQuery.value = query
        searchJob?.cancel()

        if (query.length > 1) {
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                try {
                    val results = poblacionDao.searchPoblaciones("$query%", limit = 10)
                    _locationSuggestions.value = results
                    Log.d("AddAdViewModel", "Search for '$query' found ${results.size} suggestions.")
                } catch (e: Exception) {
                    _locationSuggestions.value = emptyList()
                    Log.e("AddAdViewModel", "Error searching poblaciones for '$query'", e)
                }
            }
        } else {
            _locationSuggestions.value = emptyList()
        }

        if (_uiState.value.selectedPoblacion?.getDisplayName() != query) {
            _uiState.update { it.copy(selectedPoblacion = null) }
        }
    }

    /**
     * Se llama cuando el usuario selecciona una ubicación de la lista de sugerencias.
     * @param poblacion La ubicación seleccionada.
     */
    fun onPoblacionSelected(poblacion: PoblacionLocation) {
        _uiState.update { it.copy(selectedPoblacion = poblacion) }
        _locationSearchQuery.value = poblacion.getDisplayName()
        _locationSuggestions.value = emptyList()
        searchJob?.cancel()
    }

    /**
     * Crea y publica un nuevo anuncio.
     * @param title El título del anuncio.
     * @param description La descripción.
     * @param priceStr El precio como texto.
     * @param category La categoría.
     * @param imageUri La URI de la imagen seleccionada (si la hay).
     */
    fun addAd(
        title: String,
        description: String,
        priceStr: String,
        category: String,
        imageUri: Uri?
    ) {
        val selectedPoblacion = _uiState.value.selectedPoblacion
        val price = priceStr.toDoubleOrNull()

        if (title.isBlank() || description.isBlank() || price == null || category.isBlank() || selectedPoblacion == null) {
            _uiState.update { it.copy(error = "Please fill all fields correctly, including selecting a valid location from suggestions.") }
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
                sellerLocation = selectedPoblacion.getDisplayName(),
                sellerLatitude = selectedPoblacion.latitude,
                sellerLongitude = selectedPoblacion.longitude
            )

            val result = adRepository.addAd(adData)

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to add ad.") }
            }
        }
    }

    /**
     * Resetea el estado de la pantalla a sus valores iniciales.
     */
    fun resetState() {
        _uiState.value = AddAdUiState()
        _locationSearchQuery.value = ""
        _locationSuggestions.value = emptyList()
    }

    /**
     * Limpia el mensaje de error después de haberlo mostrado.
     */
    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Limpia el flag de éxito después de haberlo usado (por ejemplo, para navegar).
     */
    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}