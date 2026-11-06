package com.foodconnect.feature.donations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.foodconnect.core.security.SessionManager
import com.foodconnect.data.model.*
import com.foodconnect.data.repository.ApiResult
import com.foodconnect.data.repository.DonationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DonationDetailUiState(
    val isLoading: Boolean = false,
    val donation: DonationResponse? = null,
    val error: String? = null,
    val acceptSuccess: Boolean = false,
    val userRole: String? = null
)

@HiltViewModel
class DonationDetailViewModel @Inject constructor(
    private val donationRepository: DonationRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val donationId: Long = savedStateHandle["donationId"] ?: 0L

    private val _uiState = MutableStateFlow(DonationDetailUiState())
    val uiState: StateFlow<DonationDetailUiState> = _uiState.asStateFlow()

    init {
        loadDonation()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(userRole = sessionManager.getRole())
        }
    }

    private fun loadDonation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = donationRepository.getDonation(donationId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(donation = result.data, isLoading = false)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message, isLoading = false)
            }
        }
    }

    fun acceptDonation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = donationRepository.acceptDonation(donationId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(acceptSuccess = true, isLoading = false)
                    loadDonation()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message, isLoading = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationDetailScreen(
    onBackClick: () -> Unit,
    onNavigateToPickup: (Long) -> Unit,
    onAccepted: () -> Unit,
    viewModel: DonationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAcceptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.acceptSuccess) {
        if (uiState.acceptSuccess) onAccepted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.donation?.foodName ?: "Donation Details") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.donation == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Donation not found") }
            else -> {
                val d = uiState.donation!!
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                    // Image
                    d.imageUrl?.let {
                        AsyncImage(model = it, contentDescription = d.foodName, modifier = Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop)
                    }

                    Column(Modifier.padding(16.dp)) {
                        // Status chip
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(d.foodName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            AssistChip(onClick = {}, label = { Text(d.status?.name ?: "") })
                        }
                        Spacer(Modifier.height(16.dp))

                        // Info rows
                        DetailRow(Icons.Default.Restaurant, "Restaurant", d.restaurantName ?: "Unknown")
                        DetailRow(Icons.Default.Scale, "Quantity", "${d.quantity} ${d.quantityUnit}")
                        DetailRow(Icons.Default.Category, "Category", d.foodCategory?.name?.replace("_", " ") ?: "Other")
                        d.address?.let { DetailRow(Icons.Default.LocationOn, "Address", it) }
                        d.pickupDeadline?.let { DetailRow(Icons.Default.Schedule, "Pickup Deadline", it.replace("T", " ").take(16)) }
                        d.distance?.let { DetailRow(Icons.Default.NearMe, "Distance", String.format("%.1f km", it)) }
                        d.description?.let { DetailRow(Icons.Default.Description, "Description", it) }
                        d.storageInstructions?.let { DetailRow(Icons.Default.Thermostat, "Handling", it) }
                        d.claimedByNgoName?.let { DetailRow(Icons.Default.VolunteerActivism, "Claimed By", it) }

                        Spacer(Modifier.height(24.dp))

                        // Action buttons based on role and status
                        if (uiState.userRole == "NGO" && d.status == DonationStatus.AVAILABLE) {
                            Button(onClick = { showAcceptDialog = true }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("Accept Donation")
                            }
                        }

                        if (uiState.userRole == "NGO" && (d.status == DonationStatus.ACCEPTED || d.status == DonationStatus.COLLECTED)) {
                            Button(onClick = { onNavigateToPickup(d.id) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(8.dp)); Text("Navigate to Restaurant")
                            }
                        }
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Snackbar(Modifier.padding(16.dp)) { Text(error) }
        }
    }

    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptDialog = false },
            title = { Text("Accept Donation?") },
            text = { Text("You'll be assigned to pick up this food. Are you sure?") },
            confirmButton = { TextButton(onClick = { showAcceptDialog = false; viewModel.acceptDonation() }) { Text("Accept") } },
            dismissButton = { TextButton(onClick = { showAcceptDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
