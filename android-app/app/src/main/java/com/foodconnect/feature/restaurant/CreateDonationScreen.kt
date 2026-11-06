package com.foodconnect.feature.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodconnect.data.model.FoodCategory
import com.foodconnect.data.model.QuantityUnit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDonationScreen(
    onDonationCreated: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: RestaurantViewModel = hiltViewModel()
) {
    var foodName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var storageInstructions by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FoodCategory.COOKED_MEALS) }
    var selectedUnit by remember { mutableStateOf(QuantityUnit.KG) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    val createState by viewModel.createState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Use restaurant's saved location or default
    val lat = uiState.profile?.latitude ?: 28.6139
    val lng = uiState.profile?.longitude ?: 77.2090
    val restaurantAddress = uiState.profile?.address ?: ""

    LaunchedEffect(Unit) { if (address.isBlank() && restaurantAddress.isNotBlank()) address = restaurantAddress }
    LaunchedEffect(createState.success) { if (createState.success) onDonationCreated() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donate Surplus Food") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(value = foodName, onValueChange = { foodName = it }, label = { Text("Food Name *") },
                leadingIcon = { Icon(Icons.Default.Fastfood, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") },
                leadingIcon = { Icon(Icons.Default.Description, null) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            // Category dropdown
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(value = selectedCategory.name.replace("_", " "), onValueChange = {},
                    label = { Text("Food Category") }, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    FoodCategory.entries.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat.name.replace("_", " ")) },
                            onClick = { selectedCategory = cat; categoryExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Quantity + Unit row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))

                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = selectedUnit.name, onValueChange = {}, label = { Text("Unit") },
                        readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        QuantityUnit.entries.forEach { u ->
                            DropdownMenuItem(text = { Text(u.name) }, onClick = { selectedUnit = u; unitExpanded = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Pickup Address *") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(value = storageInstructions, onValueChange = { storageInstructions = it },
                label = { Text("Storage/Handling Instructions") }, leadingIcon = { Icon(Icons.Default.Thermostat, null) },
                minLines = 2, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))

            if (createState.error != null) {
                Text(createState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val now = LocalDateTime.now()
                    val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    viewModel.createDonation(
                        foodName = foodName, description = description, category = selectedCategory,
                        quantity = quantity.toDoubleOrNull() ?: 0.0, unit = selectedUnit,
                        pickupDeadline = now.plusHours(6).format(fmt),
                        availableUntil = now.plusHours(4).format(fmt),
                        address = address, lat = lat, lng = lng,
                        storageInstructions = storageInstructions, imageUrl = null
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !createState.isLoading
            ) {
                if (createState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else { Icon(Icons.Default.Publish, null); Spacer(Modifier.width(8.dp)); Text("Publish Donation") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
