package com.mbougar.swapware.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserRating(
    val ratedUserId: String = "",
    val raterUserId: String = "",
    val adId: String = "",
    val conversationId: String = "",
    val ratingValue: Int = 0,
    @ServerTimestamp
    val timestamp: Date? = null,
)