package com.mbougar.swapware.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "poblaciones",
    primaryKeys = ["poblacionName", "provincia"],
    indices = [Index(value = ["poblacionName"])]
)
data class PoblacionLocation(
    val poblacionName: String,
    val provincia: String,
    val latitude: Double,
    val longitude: Double
) {
    fun getDisplayName(): String {
        return "$poblacionName, $provincia"
    }
}