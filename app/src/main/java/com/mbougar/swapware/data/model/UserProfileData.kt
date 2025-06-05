package com.mbougar.swapware.data.model

data class UserProfileData(
    val userId: String = "",
    val displayName: String? = null,
    val profilePictureUrl: String? = null,
    var totalRatingPoints: Long = 0,
    var numberOfRatings: Long = 0,
    var averageRating: Float = 0.0f
)