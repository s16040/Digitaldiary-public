package com.example.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.app.viewmodel.EntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: EntryViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val entriesWithLocation = remember(entries) {
        entries.filter { it.lat != null && it.lon != null }
    }

    val cameraPositionState = rememberCameraPositionState {
        val firstEntry = entriesWithLocation.firstOrNull()
        if (firstEntry != null) {
            position = CameraPosition.fromLatLngZoom(
                LatLng(firstEntry.lat!!, firstEntry.lon!!),
                10f
            )
        } else {
            // Domyślna pozycja - Warszawa
            position = CameraPosition.fromLatLngZoom(
                LatLng(52.2297, 21.0122),
                10f
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa wspomnień") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = true
            )
        ) {
            entriesWithLocation.forEach { entry ->
                Marker(
                    state = MarkerState(
                        position = LatLng(entry.lat!!, entry.lon!!)
                    ),
                    title = entry.title,
                    snippet = entry.place
                )
            }
        }
    }
}
