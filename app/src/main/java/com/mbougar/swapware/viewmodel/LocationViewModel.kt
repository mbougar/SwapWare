package com.mbougar.swapware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.local.PoblacionDao
import com.mbougar.swapware.data.local.PoblacionLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para manejar la lógica de búsqueda de ubicaciones.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationViewModel @Inject constructor(
    private val poblacionDao: PoblacionDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PoblacionLocation>>(emptyList())
    val searchResults: StateFlow<List<PoblacionLocation>> = _searchResults.asStateFlow()

    private val _selectedPoblacion = MutableStateFlow<PoblacionLocation?>(null)
    val selectedPoblacion: StateFlow<PoblacionLocation?> = _selectedPoblacion.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { query -> query.length > 1 }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    try {
                        flowOf(poblacionDao.searchPoblaciones("$query%"))
                    } catch (e: Exception) {
                        flowOf(emptyList<PoblacionLocation>())
                    }
                }
                .collect { results ->
                    _searchResults.value = results
                }
        }
    }

    /**
     * Se llama cuando el texto de búsqueda cambia.
     * @param query El nuevo texto de búsqueda.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank() || query.length <=1) {
            _searchResults.value = emptyList()
        }
    }

    /**
     * Se llama cuando el usuario selecciona una población de la lista.
     * @param poblacion La población seleccionada.
     */
    fun onPoblacionSelected(poblacion: PoblacionLocation?) {
        _selectedPoblacion.value = poblacion
        _searchQuery.value = poblacion?.getDisplayName() ?: ""
        _searchResults.value = emptyList()
    }

    /**
     * Limpia la selección de población y el texto de búsqueda.
     */
    fun clearSelection() {
        _selectedPoblacion.value = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
}