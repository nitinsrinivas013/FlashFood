package com.foodconnect.feature.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodconnect.data.model.DonationResponse
import com.foodconnect.data.model.DonationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDashboardScreen(
    onCreateDonation: () -> Unit,
    onDonationClick: (Long) -> Unit,
    onNotificationsClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: RestaurantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FoodConnect", style = MaterialTheme.typography.titleLarge)
                        uiState.profile?.restaurantName?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary),
                actions = {
                    IconButton(onClick = onNotificationsClick) { Icon(Icons.Default.Notifications, "Notifications", tint = MaterialTheme.colorScheme.onPrimary) }
                    IconButton(onClick = { viewModel.logout(); onLogout() }) { Icon(Icons.Default.Logout, "Logout", tint = MaterialTheme.colorScheme.onPrimary) }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateDonation,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Donate Food") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.donations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FoodBank, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No donations yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                    Text("Tap 'Donate Food' to create your first listing", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Stats row
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val active = uiState.donations.count { it.status == DonationStatus.AVAILABLE || it.status == DonationStatus.ACCEPTED }
                        val completed = uiState.donations.count { it.status == DonationStatus.COMPLETED }
                        StatCard("Active", active.toString(), Icons.Default.LocalShipping, Modifier.weight(1f))
                        StatCard("Completed", completed.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
                        StatCard("Total", uiState.donations.size.toString(), Icons.Default.Inventory, Modifier.weight(1f))
                    }
                }

                items(uiState.donations, key = { it.id }) { donation ->
                    DonationCard(donation = donation, onClick = { onDonationClick(donation.id) })
                }
            }
        }

        uiState.error?.let {
            Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DonationCard(donation: DonationResponse, onClick: () -> Unit) {
    val statusColor = when (donation.status) {
        DonationStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
        DonationStatus.ACCEPTED -> MaterialTheme.colorScheme.tertiary
        DonationStatus.COLLECTED -> MaterialTheme.colorScheme.secondary
        DonationStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DonationStatus.EXPIRED -> MaterialTheme.colorScheme.error
        DonationStatus.CANCELLED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(donation.foodName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                AssistChip(onClick = {}, label = { Text(donation.status?.name ?: "UNKNOWN", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = statusColor.copy(alpha = 0.15f), labelColor = statusColor))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Scale, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("${donation.quantity} ${donation.quantityUnit}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(16.dp))
                if (donation.distance != null) {
                    Icon(Icons.Default.NearMe, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${donation.distance} km", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (donation.claimedByNgoName != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolunteerActivism, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(4.dp))
                    Text("Claimed by: ${donation.claimedByNgoName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            donation.pickupDeadline?.let { deadline ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("Pickup by: ${deadline.replace("T", " ").take(16)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
