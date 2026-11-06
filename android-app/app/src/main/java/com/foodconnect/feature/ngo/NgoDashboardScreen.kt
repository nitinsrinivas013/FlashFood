package com.foodconnect.feature.ngo

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodconnect.data.model.DonationClaimResponse
import com.foodconnect.data.model.PickupStatus
import com.foodconnect.feature.restaurant.DonationCard
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NgoDashboardScreen(
    onDonationClick: (Long) -> Unit,
    onNotificationsClick: () -> Unit,
    onNavigateToPickup: (Long) -> Unit,
    onLogout: () -> Unit,
    viewModel: NgoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            scope.launch {
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    val location = fusedClient.lastLocation.await()
                    location?.let { viewModel.updateLocation(it.latitude, it.longitude) }
                } catch (_: SecurityException) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FoodConnect", style = MaterialTheme.typography.titleLarge)
                        uiState.profile?.ngoName?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary),
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                val loc = fusedClient.lastLocation.await()
                                loc?.let { viewModel.updateLocation(it.latitude, it.longitude) }
                            } catch (_: SecurityException) {}
                        }
                    }) { Icon(Icons.Default.MyLocation, "Refresh Location", tint = MaterialTheme.colorScheme.onPrimary) }
                    IconButton(onClick = onNotificationsClick) { Icon(Icons.Default.Notifications, "Notifications", tint = MaterialTheme.colorScheme.onPrimary) }
                    IconButton(onClick = { viewModel.logout(); onLogout() }) { Icon(Icons.Default.Logout, "Logout", tint = MaterialTheme.colorScheme.onPrimary) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Tab bar
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(selected = pagerState.currentPage == 0, onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Nearby Food") }, icon = { Icon(Icons.Default.NearMe, null) })
                Tab(selected = pagerState.currentPage == 1, onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("My Pickups") }, icon = { Icon(Icons.Default.LocalShipping, null) })
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> NearbyDonationsTab(uiState, onDonationClick)
                    1 -> MyPickupsTab(uiState.myPickups, onNavigateToPickup, viewModel)
                }
            }
        }
    }
}

@Composable
private fun NearbyDonationsTab(uiState: NgoUiState, onDonationClick: (Long) -> Unit) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (uiState.nearbyDonations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SearchOff, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text("No nearby donations", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                if (uiState.currentLat == null) {
                    Text("Enable location to discover food nearby", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.nearbyDonations, key = { it.id }) { donation ->
                DonationCard(donation = donation, onClick = { onDonationClick(donation.id) })
            }
        }
    }
}

@Composable
private fun MyPickupsTab(pickups: List<DonationClaimResponse>, onNavigateToPickup: (Long) -> Unit, viewModel: NgoViewModel) {
    if (pickups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inbox, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text("No active pickups", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                Text("Accept a donation to start", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pickups, key = { it.id }) { pickup ->
                PickupCard(pickup, onNavigateToPickup, viewModel)
            }
        }
    }
}

@Composable
private fun PickupCard(pickup: DonationClaimResponse, onNavigateToPickup: (Long) -> Unit, viewModel: NgoViewModel) {
    val isActive = pickup.pickupStatus == PickupStatus.ASSIGNED || pickup.pickupStatus == PickupStatus.EN_ROUTE
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pickup #${pickup.donationId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(pickup.pickupStatus?.name ?: "UNKNOWN") })
            }
            pickup.ngoName?.let { Text("NGO: $it", style = MaterialTheme.typography.bodyMedium) }
            pickup.acceptedAt?.let { Text("Accepted: ${it.replace("T"," ").take(16)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isActive) {
                    Button(onClick = { pickup.donationId?.let { onNavigateToPickup(it) } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(4.dp)); Text("Navigate")
                    }
                    OutlinedButton(onClick = { pickup.donationId?.let { viewModel.markCollected(it) } }, modifier = Modifier.weight(1f)) {
                        Text("Collected")
                    }
                }
                if (pickup.pickupStatus == PickupStatus.COLLECTED) {
                    Button(onClick = { pickup.donationId?.let { viewModel.markCompleted(it) } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(4.dp)); Text("Complete")
                    }
                }
            }
        }
    }
}
