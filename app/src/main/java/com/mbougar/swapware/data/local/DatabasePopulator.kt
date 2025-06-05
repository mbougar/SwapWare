package com.mbougar.swapware.data.local

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider

class DatabasePopulator @Inject constructor(
    private val poblacionDaoProvider: Provider<PoblacionDao>,
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    fun populateOnCreate() {
        coroutineScope.launch {
            val poblacionDao = poblacionDaoProvider.get()
            if (poblacionDao.getCount() == 0) {
                Log.d("DB_POPULATE", "Poblaciones table is empty. Attempting to populate from CSV.")
                try {
                    val poblacionesToInsert = mutableListOf<PoblacionLocation>()
                    context.assets.open("poblaciones.csv").bufferedReader().useLines { lines ->
                        lines.drop(1)
                            .forEach { line ->
                                val tokens = line.split(',')
                                if (tokens.size >= 4) {
                                    try {
                                        val provincia = tokens[0].trim()
                                        val nombre = tokens[1].trim()
                                        val latitud = tokens[2].trim().replace(',', '.').toDouble()
                                        val longitud = tokens[3].trim().replace(',', '.').toDouble()

                                        if (nombre.isNotBlank() && provincia.isNotBlank()) {
                                            poblacionesToInsert.add(
                                                PoblacionLocation(
                                                    poblacionName = nombre,
                                                    provincia = provincia,
                                                    latitude = latitud,
                                                    longitude = longitud
                                                )
                                            )
                                        }
                                    } catch (e: NumberFormatException) {
                                        Log.e("DB_POPULATE", "Skipping line due to number parsing error: $line. Error: ${e.localizedMessage}")
                                    } catch (e: Exception) {
                                        Log.e("DB_POPULATE", "General error parsing line: $line. Error: ${e.localizedMessage}")
                                    }
                                } else {
                                    Log.w("DB_POPULATE", "Skipping malformed line (not enough tokens): $line")
                                }
                            }
                    }
                    if (poblacionesToInsert.isNotEmpty()) {
                        poblacionDao.insertAll(poblacionesToInsert)
                        Log.d("DB_POPULATE", "Successfully populated ${poblacionesToInsert.size} poblaciones from CSV.")
                    } else {
                        Log.w("DB_POPULATE", "No valid poblaciones found in CSV to insert.")
                    }
                } catch (e: IOException) {
                    Log.e("DB_POPULATE", "Error reading or populating poblaciones from CSV", e)
                }
            } else {
                Log.d("DB_POPULATE", "Poblaciones table already populated.")
            }
        }
    }
}