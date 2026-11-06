package com.foodconnect.feature.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodconnect.data.model.DonationResponse
import com.foodconnect.data.model.DonationStatus
import com.foodconnect.data.repository.ApiResult
import com.foodconnect.data.repository.DonationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class PickupNavUiState(
    val isLoading: Boolean = true,
    val donation: DonationResponse? = null,
    val ngoLat: Double? = null,
    val ngoLng: Double? = null,
    val error: String? = null,
    val completed: Boolean = false
)

@HiltViewModel
class PickupNavigationViewModel @Inject constructor(
    private val donationRepository: DonationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val donationId: Long = savedStateHandle["donationId"] ?: 0L
    private val _uiState = MutableStateFlow(PickupNavUiState())
    val uiState: StateFlow<PickupNavUiState> = _uiState.asStateFlow()

    init { loadDonation() }

    private fun loadDonation() {
        viewModelScope.launch {
            when (val result = donationRepository.getDonation(donationId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(donation = result.data, isLoading = false)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message, isLoading = false)
            }
        }
    }

    fun updateNgoLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(ngoLat = lat, ngoLng = lng)
    }

    fun markCollected() {
        viewModelScope.launch {
            when (donationRepository.markCollected(donationId)) {
                is ApiResult.Success -> loadDonation()
                is ApiResult.Error -> {}
            }
        }
    }

    fun markCompleted() {
        viewModelScope.launch {
            when (donationRepository.markCompleted(donationId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(completed = true)
                is ApiResult.Error -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupNavigationScreen(
    onBackClick: () -> Unit,
    onCompleted: () -> Unit,
    viewModel: PickupNavigationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Get current location
    LaunchedEffect(Unit) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedClient.lastLocation.await()
            location?.let { viewModel.updateNgoLocation(it.latitude, it.longitude) }
        } catch (_: SecurityException) {}
    }

    LaunchedEffect(uiState.completed) { if (uiState.completed) onCompleted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pickup Navigation") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.donation == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Donation not found") }
            else -> {
                val donation = uiState.donation!!
                val restaurantLat = donation.latitude ?: 0.0
                val restaurantLng = donation.longitude ?: 0.0
                val restaurantPos = LatLng(restaurantLat, restaurantLng)
                val ngoPos = if (uiState.ngoLat != null && uiState.ngoLng != null) LatLng(uiState.ngoLat!!, uiState.ngoLng!!) else null

                Column(Modifier.fillMaxSize().padding(padding)) {
                    // Map
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(restaurantPos, 14f)
                        }

                        // Fit both markers when we have NGO position
                        LaunchedEffect(ngoPos) {
                            if (ngoPos != null) {
                                val bounds = LatLngBounds.builder()
                                    .include(restaurantPos)
                                    .include(ngoPos)
                                    .build()
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                            }
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = true),
                            uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
                        ) {
                            // Restaurant marker
                            Marker(
                                state = MarkerState(position = restaurantPos),
                                title = donation.restaurantName ?: "Restaurant",
                                snippet = donation.address
                            )
                            // NGO marker
                            ngoPos?.let {
                                Marker(
                                    state = MarkerState(position = it),
                                    title = "Your Location"
                                )
                            }
                        }
                    }

                    // Bottom info panel
                    Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(donation.restaurantName ?: "Restaurant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(donation.address ?: "Address not available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            donation.distance?.let {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NearMe, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(String.format("%.1f km away", it), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fastfood, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text("${donation.foodName} — ${donation.quantity} ${donation.quantityUnit}", style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(Modifier.height(16.dp))

                            // Action buttons
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Start Navigation button — launches Google Maps
                                Button(
                                    onClick = { launchGoogleMapsNavigation(context, restaurantLat, restaurantLng) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Navigation, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Start Navigation")
                                }

                                when (donation.status) {
                                    DonationStatus.ACCEPTED -> {
                                        OutlinedButton(onClick = { viewModel.markCollected() }, modifier = Modifier.weight(1f)) {
                                            Text("Mark Collected")
                                        }
                                    }
                                    DonationStatus.COLLECTED -> {
                                        OutlinedButton(onClick = { viewModel.markCompleted() }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.CheckCircle, null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Complete")
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Launch Google Maps turn-by-turn navigation to the restaurant.
 * Falls back to a Maps URL if the navigation intent is unavailable.
 */
private fun launchGoogleMapsNavigation(context: Context, lat: Double, lng: Double) {
    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        // Fallback to web maps
        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}
