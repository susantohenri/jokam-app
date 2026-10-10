package com.jokam.tempatsambung.ui.pengurus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jokam.tempatsambung.data.model.Pengurus
import com.jokam.tempatsambung.data.model.Place
import com.jokam.tempatsambung.data.repository.DataRepository
import com.jokam.tempatsambung.ui.home.LocationStatus
import com.jokam.tempatsambung.ui.home.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PengurusUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pengurusList: List<Pengurus> = emptyList(),
    val searchQuery: String = ""
)

class PengurusViewModel(
    private val dataRepository: DataRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _rawPengurus = MutableStateFlow<List<Pengurus>>(emptyList())
    private val _places = MutableStateFlow<List<Place>>(emptyList())
    private val _locationStatus = MutableStateFlow<LocationStatus>(LocationStatus.Checking)
    private val _searchQuery = MutableStateFlow("")

    private data class DataState(
        val isLoading: Boolean,
        val errorMessage: String?,
        val pengurus: List<Pengurus>,
        val places: List<Place>
    )

    private val _dataState = combine(_isLoading, _errorMessage, _rawPengurus, _places) { loading, error, pengurus, places ->
        DataState(loading, error, pengurus, places)
    }

    val uiState: StateFlow<PengurusUiState> = combine(
        _dataState,
        _locationStatus,
        _searchQuery
    ) { (loading, error, pengurus, places), locStatus, query ->

        // Calculate centroids per city (ignoring empty city names)
        val cityCentroids = places.filter { it.city.isNotBlank() }.groupBy { it.city.lowercase() }.mapValues { entry ->
            val lats = entry.value.map { it.lat }
            val lngs = entry.value.map { it.lng }
            Pair(lats.average(), lngs.average())
        }

        val sortedList = when (locStatus) {
            is LocationStatus.HasLocation -> {
                pengurus.sortedWith(
                    compareBy<Pengurus> { p ->
                        val centroid = cityCentroids[p.city.lowercase()]
                        if (centroid != null) {
                            LocationUtils.calculateDistanceMeters(locStatus.lat, locStatus.lng, centroid.first, centroid.second)
                        } else {
                            Double.MAX_VALUE
                        }
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.province }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.city }
                )
            }
            is LocationStatus.CitySelected -> {
                pengurus.sortedWith(
                    compareBy<Pengurus> { p ->
                        val centroid = cityCentroids[p.city.lowercase()]
                        if (centroid != null) {
                            LocationUtils.calculateDistanceMeters(locStatus.centroidLat, locStatus.centroidLng, centroid.first, centroid.second)
                        } else {
                            Double.MAX_VALUE
                        }
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.province }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.city }
                )
            }
            else -> {
                // Alphabetical by province then city (case-insensitive)
                pengurus.sortedWith(
                    compareBy<Pengurus, String>(String.CASE_INSENSITIVE_ORDER) { it.province }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.city }
                )
            }
        }

        val filteredList = if (query.isBlank()) {
            sortedList
        } else {
            val q = query.trim().lowercase()
            sortedList.filter { p ->
                p.city.lowercase().contains(q) ||
                p.province.lowercase().contains(q) ||
                p.phone.contains(q)
            }
        }

        PengurusUiState(
            isLoading = loading,
            errorMessage = error,
            pengurusList = filteredList,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PengurusUiState(isLoading = true)
    )

    fun updateLocationStatus(status: LocationStatus) {
        _locationStatus.value = status
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val pRes = dataRepository.getPengurus(forceRefresh)
            val plRes = dataRepository.getPlaces(forceRefresh)

            plRes.onSuccess { _places.value = it }
            pRes.onSuccess {
                _rawPengurus.value = it
                _isLoading.value = false
            }.onFailure {
                _errorMessage.value = it.localizedMessage
                _isLoading.value = false
            }
        }
    }
}
