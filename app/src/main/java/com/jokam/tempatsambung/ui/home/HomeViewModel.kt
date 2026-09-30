package com.jokam.tempatsambung.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jokam.tempatsambung.data.model.Place
import com.jokam.tempatsambung.data.repository.DataRepository
import com.jokam.tempatsambung.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LocationStatus {
    object Checking : LocationStatus
    data class HasLocation(val lat: Double, val lng: Double) : LocationStatus
    object PermissionNeeded : LocationStatus
    object GpsDisabled : LocationStatus
    object LocationUnavailable : LocationStatus
    data class CitySelected(val cityName: String, val centroidLat: Double, val centroidLng: Double) : LocationStatus
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val locationStatus: LocationStatus = LocationStatus.Checking,
    val favoritePlaces: List<PlaceWithDistance> = emptyList(),
    val mainPlaces: List<PlaceWithDistance> = emptyList(),
    val citySuggestions: List<String> = emptyList(),
    val searchQuery: String = ""
)

data class PlaceWithDistance(
    val place: Place,
    val distanceMeters: Double?,
    val displayLabel: String?
)

class HomeViewModel(
    private val dataRepository: DataRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _allPlaces = MutableStateFlow<List<Place>>(emptyList())
    private val _locationStatus = MutableStateFlow<LocationStatus>(LocationStatus.Checking)
    private val _searchQuery = MutableStateFlow("")

    val favoritesFlow: StateFlow<Set<String>> = preferencesRepository.favoritesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    private data class PlacesState(
        val isLoading: Boolean,
        val errorMessage: String?,
        val places: List<Place>
    )

    private val _placesState = combine(_isLoading, _errorMessage, _allPlaces) { loading, error, places ->
        PlacesState(loading, error, places)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _placesState,
        _locationStatus,
        favoritesFlow,
        _searchQuery
    ) { (loading, error, places), locStatus, favIds, query ->

        val nonFavPlaces = places.filterNot { favIds.contains(it.id) }

        // Favorite places: deterministic sorting by name, distance calculated if GPS available
        val favPlaces = favIds.mapNotNull { id ->
            places.find { it.id == id }
        }.sortedBy { it.name.lowercase() }.map { place ->
            when (locStatus) {
                is LocationStatus.HasLocation -> {
                    val dist = LocationUtils.calculateDistanceMeters(
                        locStatus.lat, locStatus.lng, place.lat, place.lng
                    )
                    PlaceWithDistance(place, dist, LocationUtils.formatDistance(dist))
                }
                else -> {
                    PlaceWithDistance(place, null, place.city.ifBlank { null })
                }
            }
        }

        val computedMainList = when (locStatus) {
            is LocationStatus.HasLocation -> {
                nonFavPlaces.map { place ->
                    val dist = LocationUtils.calculateDistanceMeters(
                        locStatus.lat, locStatus.lng, place.lat, place.lng
                    )
                    PlaceWithDistance(place, dist, LocationUtils.formatDistance(dist))
                }.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
            }
            is LocationStatus.CitySelected -> {
                // In city mode: sort ALL non-fav places by distance from centroid, display city name (do NOT show km)
                nonFavPlaces
                    .map { place ->
                        val dist = LocationUtils.calculateDistanceMeters(
                            locStatus.centroidLat, locStatus.centroidLng, place.lat, place.lng
                        )
                        PlaceWithDistance(place, dist, place.city.ifBlank { null })
                    }.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
            }
            else -> {
                // When location is unavailable and no city selected yet, show alphabetical by name
                nonFavPlaces
                    .sortedBy { it.name.lowercase() }
                    .map { PlaceWithDistance(it, null, it.city.ifBlank { null }) }
            }
        }

        // Suggestions for city autocomplete: distinct "City, Province"
        val suggestions = if (query.isNotBlank()) {
            places
                .filter { it.city.isNotBlank() }
                .map {
                    if (it.province.isNotBlank()) "${it.city}, ${it.province}" else it.city
                }
                .distinct()
                .filter { it.contains(query, ignoreCase = true) }
                .take(10)
        } else {
            emptyList()
        }

        HomeUiState(
            isLoading = loading,
            errorMessage = error,
            locationStatus = locStatus,
            favoritePlaces = favPlaces,
            mainPlaces = computedMainList,
            citySuggestions = suggestions,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        loadPlaces()
    }

    fun loadPlaces(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = dataRepository.getPlaces(forceRefresh)
            result.onSuccess { places ->
                _allPlaces.value = places
                _isLoading.value = false
            }.onFailure { error ->
                _isLoading.value = false
                _errorMessage.value = error.localizedMessage
            }
        }
    }

    fun onLocationReceived(location: Location) {
        _locationStatus.value = LocationStatus.HasLocation(location.latitude, location.longitude)
    }

    fun onLocationPermissionDenied() {
        _locationStatus.value = LocationStatus.PermissionNeeded
    }

    fun onGpsDisabled() {
        _locationStatus.value = LocationStatus.GpsDisabled
    }

    fun onLocationUnavailable() {
        _locationStatus.value = LocationStatus.LocationUnavailable
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectCitySuggestion(suggestion: String) {
        val cityName = suggestion.split(",").firstOrNull()?.trim() ?: suggestion
        val cityPlaces = _allPlaces.value.filter { it.city.equals(cityName, ignoreCase = true) }
        val centroidLat = if (cityPlaces.isNotEmpty()) cityPlaces.map { it.lat }.average() else 0.0
        val centroidLng = if (cityPlaces.isNotEmpty()) cityPlaces.map { it.lng }.average() else 0.0

        _locationStatus.value = LocationStatus.CitySelected(cityName, centroidLat, centroidLng)
        _searchQuery.value = ""
    }

    fun clearSelectedCity() {
        _locationStatus.value = LocationStatus.LocationUnavailable
        _searchQuery.value = ""
    }

    fun toggleFavorite(placeId: String) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(placeId)
        }
    }
}
