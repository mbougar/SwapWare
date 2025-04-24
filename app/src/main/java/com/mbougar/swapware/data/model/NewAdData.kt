package com.mbougar.swapware.data.model

import android.net.Uri

data class NewAdData(
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUri: Uri?
)