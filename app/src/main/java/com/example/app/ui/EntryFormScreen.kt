package com.example.app.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.launch
import com.example.app.util.AudioRecorder
import com.example.app.util.CameraUtil
import com.example.app.util.LocationUtil
import com.example.app.viewmodel.EntryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EntryFormScreen(
    navController: NavController,
    uid: String,
    viewModel: EntryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var location by remember { mutableStateOf<LocationUtil.LocationData?>(null) }

    var showCamera by remember { mutableStateOf(false) }
    val audioRecorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { photoUri = it }
    }

    LaunchedEffect(uid) {
        viewModel.setUserId(uid)
    }

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    if (showCamera) {
        CameraScreen(
            onPhotoTaken = { uri ->
                photoUri = uri
                showCamera = false
            },
            onBack = { showCamera = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Nowy wpis") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (title.isBlank() || content.isBlank()) {
                                    return@TextButton
                                }

                                viewModel.saveEntry(
                                    title = title,
                                    content = content,
                                    location = location,
                                    photoUri = photoUri,
                                    audioUri = audioUri
                                )

                                navController.navigateUp()
                            },
                            enabled = !isLoading && title.isNotBlank() && content.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text("Zapisz")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Treść wspomnienia") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sekcja multimediów
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Multimedia",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Zdjęcie
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { showCamera = true },
                                enabled = permissionsState.permissions[0].status.isGranted
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Zrób zdjęcie")
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") }
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Z galerii")
                            }
                        }

                        photoUri?.let { uri ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Wybrane zdjęcie",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { photoUri = null },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Usuń zdjęcie",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Audio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isRecording) {
                                        audioUri = audioRecorder.stop()
                                        isRecording = false
                                    } else {
                                        if (audioRecorder.start()) {
                                            isRecording = true
                                        }
                                    }
                                },
                                enabled = permissionsState.permissions[1].status.isGranted,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isRecording) "Zatrzymaj" else "Nagraj audio")
                            }

                            if (audioUri != null) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    Icons.Default.AudioFile,
                                    contentDescription = "Audio nagrane",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Audio nagrane")
                                IconButton(onClick = { audioUri = null }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Usuń audio")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lokalizacja
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lokalizacja",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (location != null) {
                                Text(
                                    text = location!!.place ?: "Lokalizacja pobrana",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    location = LocationUtil.getCurrentLocation(context)
                                }
                            },
                            enabled = permissionsState.permissions[2].status.isGranted
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (location == null) "Pobierz" else "Zaktualizuj")
                        }
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraScreen(
    onPhotoTaken: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    CameraUtil.startPreview(context, this) { capture ->
                        imageCapture = capture
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Wstecz",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = {
                    imageCapture?.let { capture ->
                        CameraUtil.takePicture(
                            context = context,
                            imageCapture = capture,
                            onSuccess = onPhotoTaken,
                            onError = { it.printStackTrace() }
                        )
                    }
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Zrób zdjęcie",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
