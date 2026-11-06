package com.foodconnect.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodconnect.data.model.NotificationResponse
import com.foodconnect.data.model.NotificationType
import com.foodconnect.data.repository.ApiResult
import com.foodconnect.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationResponse>>(emptyList())
    val notifications: StateFlow<List<NotificationResponse>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = notificationRepository.getNotifications()) {
                is ApiResult.Success -> _notifications.value = result.data
                is ApiResult.Error -> {}
            }
            _isLoading.value = false
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
            _notifications.value = _notifications.value.map {
                if (it.id == id) it.copy(isRead = true) else it
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onDonationClick: (Long) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No notifications yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(8.dp)) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationItem(notif, onClick = {
                        viewModel.markAsRead(notif.id)
                        notif.donationId?.let { onDonationClick(it) }
                    })
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: NotificationResponse, onClick: () -> Unit) {
    val icon = when (notification.notificationType) {
        NotificationType.NEW_DONATION_NEARBY -> Icons.Default.Fastfood
        NotificationType.DONATION_ACCEPTED -> Icons.Default.CheckCircle
        NotificationType.DONATION_COLLECTED -> Icons.Default.LocalShipping
        NotificationType.DONATION_COMPLETED -> Icons.Default.Verified
        NotificationType.DONATION_CANCELLED -> Icons.Default.Cancel
        NotificationType.DONATION_EXPIRED -> Icons.Default.HourglassBottom
        else -> Icons.Default.Notifications
    }
    val bgAlpha = if (notification.isRead == true) 0f else 0.06f

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, Modifier.size(28.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(notification.title ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = if (notification.isRead != true) FontWeight.Bold else FontWeight.Normal)
                Spacer(Modifier.height(2.dp))
                Text(notification.message ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                notification.createdAt?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it.replace("T", " ").take(16), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            if (notification.isRead != true) {
                Box(Modifier.size(8.dp).padding(top = 4.dp)) {
                    Surface(Modifier.size(8.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {}
                }
            }
        }
    }
}
