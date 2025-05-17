package com.mbougar.swapware.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.NewAdData
import com.mbougar.swapware.data.repository.AdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddAdUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AddAdViewModel @Inject constructor(
    private val adRepository: AdRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAdUiState())
    val uiState: StateFlow<AddAdUiState> = _uiState.asStateFlow()

    fun addAd(
        title: String,
        description: String,
        priceStr: String,
        category: String,
        imageUri: Uri?
    ) {
        val price = priceStr.toDoubleOrNull()
        if (title.isBlank() || description.isBlank() || price == null || category.isBlank()) {
            _uiState.value = AddAdUiState(error = "Please fill all fields correctly.")
            return
        }
        if (price <= 0) {
            _uiState.value = AddAdUiState(error = "Price must be positive.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddAdUiState(isLoading = true)

            val adData = NewAdData(
                title = title,
                description = description,
                price = price,
                category = category,
                imageUri = imageUri
            )

            val result = adRepository.addAd(adData)

            if (result.isSuccess) {
                _uiState.value = AddAdUiState(isSuccess = true)
            } else {
                _uiState.value = AddAdUiState(error = result.exceptionOrNull()?.message ?: "Failed to add ad.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddAdUiState()
    }
}