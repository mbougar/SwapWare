package com.mbougar.swapware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.local.PoblacionDao
import com.mbougar.swapware.data.local.PoblacionLocation
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val ads: List<Ad> = emptyList(),
    val currentUserId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedCategory: String? = null,
    val userPoblacionForFilter: PoblacionLocation? = null,
    val filterDistanceKm: Float? = null,
    val locationSearchQuery: String = "",
    val locationSuggestions: List<PoblacionLocation> = emptyList()
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val adRepository: AdRepository,
    private val poblacionDao: PoblacionDao,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedCategoryFlow = MutableStateFlow<String?>(null)
    private val _userPoblacionForFilterFlow = MutableStateFlow<PoblacionLocation?>(null)
    private val _filterDistanceKmFlow = MutableStateFlow<Float?>(null)
    private val _locationSearchQueryFlow = MutableStateFlow("")


    init {
        _uiState.update { it.copy(currentUserId = authRepository.getCurrentUser()?.uid) }
        loadAds()

        viewModelScope.launch {
            _locationSearchQueryFlow
                .debounce(300)
                .filter { it.length > 1 }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    try {
                        flowOf(poblacionDao.searchPoblaciones(query, limit = 10))
                    } catch (e: Exception) {
                        flowOf(emptyList<PoblacionLocation>())
                    }
                }
                .collect { suggestions ->
                    _uiState.update { it.copy(locationSuggestions = suggestions) }
                }
        }
    }

    private fun loadAds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            adRepository.getAds()
                .combine(_selectedCategoryFlow) { adsResult, category -> Pair(adsResult, category) }
                .combine(_userPoblacionForFilterFlow) { (adsResult, category), poblacion -> Triple(adsResult, category, poblacion) }
                .combine(_filterDistanceKmFlow) { (adsResult, category, poblacion), distance -> Quadruple(adsResult, category, poblacion, distance) }
                .collect { (adsResult, category, userPoblacion, distanceKm) ->
                    _uiState.update { it.copy(isLoading = false) }

                    adsResult.onSuccess { ads ->
                        var filteredAds = ads
                        if (category != null) {
                            filteredAds = filteredAds.filter { ad -> ad.category.equals(category, ignoreCase = true) }
                        }

                        if (userPoblacion != null && distanceKm != null) {
                            filteredAds = filteredAds.filter { ad ->
                                ad.sellerLatitude != null && ad.sellerLongitude != null &&
                                        userPoblacion.latitude != 0.0 && userPoblacion.longitude != 0.0 &&
                                        LocationUtils.calculateDistanceKm(
                                            userPoblacion.latitude, userPoblacion.longitude,
                                            ad.sellerLatitude, ad.sellerLongitude
                                        ) <= distanceKm
                            }
                        }
                        _uiState.update { it.copy(ads = filteredAds, error = null, selectedCategory = category, userPoblacionForFilter = userPoblacion, filterDistanceKm = distanceKm) }
                    }.onFailure { throwable ->
                        _uiState.update { it.copy(error = throwable.message ?: "An unknown error occurred", ads = emptyList()) }
                    }
                }
        }
    }

    fun filterByCategory(category: String?) {
        _selectedCategoryFlow.value = category
    }

    fun setDistanceFilterLocation(poblacion: PoblacionLocation?) {
        _userPoblacionForFilterFlow.value = poblacion
        _uiState.update { it.copy(userPoblacionForFilter = poblacion, locationSearchQuery = poblacion?.getDisplayName() ?: "", locationSuggestions = emptyList()) }
    }

    fun setFilterDistanceKm(distance: Float?) {
        _filterDistanceKmFlow.value = distance
    }

    fun onFilterLocationSearchQueryChanged(query: String) {
        _uiState.update { it.copy(locationSearchQuery = query) }
        if (query.isBlank() || query.length <= 1) {
            _uiState.update { it.copy(locationSuggestions = emptyList()) }
        } else {
            _locationSearchQueryFlow.value = query
        }
        if (_uiState.value.userPoblacionForFilter?.getDisplayName() != query) {
            _userPoblacionForFilterFlow.value = null
        }
    }


    fun toggleFavorite(adId: String) {
        viewModelScope.launch {
            val currentAds = _uiState.value.ads
            val adToToggle = adRepository.getAdById(adId)
            adToToggle?.let {
                val newFavStatus = !it.isFavorite
                adRepository.toggleFavorite(adId, newFavStatus)
                _uiState.update { currentState ->
                    currentState.copy(
                        ads = currentState.ads.map { currentAd ->
                            if (currentAd.id == adId) currentAd.copy(isFavorite = newFavStatus) else currentAd
                        }
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = adRepository.refreshAds()
            if (result.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }
}