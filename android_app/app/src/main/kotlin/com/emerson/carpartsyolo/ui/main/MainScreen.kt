package com.emerson.carpartsyolo.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerson.carpartsyolo.camera.CameraPreview
import com.emerson.carpartsyolo.ml.DetectionResult
import com.emerson.carpartsyolo.ml.YoloSegmenter
import com.emerson.carpartsyolo.ui.overlay.DetectionOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Segmenter YOLO Singleton associado ao ciclo de vida
    val segmenter = remember { YoloSegmenter(context) }

    DisposableEffect(Unit) {
        // Tenta carregar o modelo default dos assets na inicialização
        val loaded = segmenter.loadDefaultModel()
        viewModel.setModelStatus(loaded, if (loaded) segmenter.currentModelName else "Não carregado")
        onDispose {
            segmenter.close()
        }
    }

    // Permissão de câmera
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Seletor de imagem da galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                viewModel.setProcessingGallery(true)
                val bitmap = loadBitmapFromUri(context, it)
                if (bitmap != null) {
                    val result = withContext(Dispatchers.Default) {
                        segmenter.segment(
                            bitmap = bitmap,
                            confThreshold = uiState.confThreshold,
                            iouThreshold = uiState.iouThreshold,
                            maskOpacity = uiState.maskOpacity
                        )
                    }
                    viewModel.setGalleryImage(bitmap, result)
                } else {
                    viewModel.setProcessingGallery(false)
                    Toast.makeText(context, "Não foi possível carregar a imagem", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Seletor de arquivo .tflite do armazenamento (ex: baixado do Drive)
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null && bytes.isNotEmpty()) {
                        val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                            order(ByteOrder.nativeOrder())
                            put(bytes)
                            rewind()
                        }
                        val success = segmenter.loadFromByteBuffer(buffer, "modelo_externo.tflite")
                        viewModel.setModelStatus(success, if (success) "best.tflite (Drive/Local)" else "Falha ao carregar")
                        val msg = if (success) "Modelo TFLite carregado com sucesso!" else "Erro ao ler modelo .tflite"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CarParts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "YOLO26-seg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Chip de Status do Modelo
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isModelLoaded) Color(0xFF1B5E20) else Color(0xFFE65100),
                        modifier = Modifier
                            .clickable { viewModel.showModelPicker(true) }
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isModelLoaded) Color(0xFF00E676) else Color(0xFFFFD600))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isModelLoaded) "Modelo Ativo" else "Carregar Modelo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Botão Inverter Câmera (só no modo Live)
                    if (uiState.mode == AppMode.LIVE_CAMERA) {
                        IconButton(onClick = { viewModel.toggleCamera() }) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Alternar Câmera")
                        }
                    }

                    // Botão Configurações / Hiperparâmetros
                    IconButton(onClick = { viewModel.showSettings(true) }) {
                        Icon(Icons.Default.Tune, contentDescription = "Configurações")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.mode == AppMode.LIVE_CAMERA,
                    onClick = { viewModel.setMode(AppMode.LIVE_CAMERA) },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    label = { Text("Câmera ao Vivo") }
                )
                NavigationBarItem(
                    selected = uiState.mode == AppMode.GALLERY_IMAGE,
                    onClick = { viewModel.setMode(AppMode.GALLERY_IMAGE) },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    label = { Text("Foto / Galeria") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.mode) {
                AppMode.LIVE_CAMERA -> {
                    if (!hasCameraPermission) {
                        CameraPermissionPrompt(
                            onRequestPermission = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    } else {
                        LiveCameraContent(
                            segmenter = segmenter,
                            uiState = uiState,
                            onResult = { result -> viewModel.updateLiveResult(result) },
                            onSelectModel = { viewModel.showModelPicker(true) }
                        )
                    }
                }
                AppMode.GALLERY_IMAGE -> {
                    GalleryContent(
                        uiState = uiState,
                        onPickImage = { galleryLauncher.launch("image/*") },
                        onSelectModel = { viewModel.showModelPicker(true) }
                    )
                }
            }

            // BottomSheet de Configurações
            if (uiState.showSettingsSheet) {
                SettingsBottomSheet(
                    uiState = uiState,
                    onDismiss = { viewModel.showSettings(false) },
                    onConfChanged = { viewModel.updateConfThreshold(it) },
                    onIouChanged = { viewModel.updateIouThreshold(it) },
                    onOpacityChanged = { viewModel.updateMaskOpacity(it) },
                    onToggleBoxes = { viewModel.toggleBoxes(it) },
                    onToggleLabels = { viewModel.toggleLabels(it) },
                    onToggleMasks = { viewModel.toggleMasks(it) }
                )
            }

            // BottomSheet de Carregamento de Modelo
            if (uiState.showModelPickerSheet) {
                ModelPickerBottomSheet(
                    uiState = uiState,
                    onDismiss = { viewModel.showModelPicker(false) },
                    onPickFile = {
                        viewModel.showModelPicker(false)
                        modelPickerLauncher.launch("*/*")
                    },
                    onReloadAssets = {
                        val ok = segmenter.loadDefaultModel()
                        viewModel.setModelStatus(ok, if (ok) segmenter.currentModelName else "Não carregado")
                        Toast.makeText(
                            context,
                            if (ok) "Modelo recarregado dos assets!" else "Nenhum arquivo .tflite encontrado em assets/",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveCameraContent(
    segmenter: YoloSegmenter,
    uiState: AppUiState,
    onResult: (DetectionResult) -> Unit,
    onSelectModel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            segmenter = segmenter,
            confThreshold = uiState.confThreshold,
            iouThreshold = uiState.iouThreshold,
            maskOpacity = uiState.maskOpacity,
            cameraSelector = uiState.cameraSelector,
            onResult = onResult,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay com caixas e máscaras
        DetectionOverlay(
            detectionResult = uiState.liveResult,
            showBoxes = uiState.showBoxes,
            showLabels = uiState.showLabels,
            showMasks = uiState.showMasks,
            isMirrored = uiState.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA,
            modifier = Modifier.fillMaxSize()
        )

        // Aviso se modelo não estiver carregado
        if (!uiState.isModelLoaded) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modelo TFLite não carregado",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Toque para carregar o best.tflite baixado do Drive",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = onSelectModel,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Carregar")
                    }
                }
            }
        }

        // Card superior com telemetria (latência e peças)
        val result = uiState.liveResult
        if (result != null && uiState.isModelLoaded) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "⚡ ${result.inferenceTimeMs} ms",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "•  ${result.parts.size} peças detectadas",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Chips horizontais inferiores com as peças encontradas
        val parts = uiState.liveResult?.parts.orEmpty()
        if (parts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(parts) { part ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(part.partClass.color).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(part.partClass.color))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(part.partClass.color))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${part.partClass.labelPt} ${(part.confidence * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryContent(
    uiState: AppUiState,
    onPickImage: () -> Unit,
    onSelectModel: () -> Unit
) {
    if (uiState.galleryBitmap == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Segmentação de Imagem",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Selecione uma foto da galeria ou arquivo para detectar e segmentar todas as peças do veículo com o YOLO26n.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onPickImage,
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escolher Imagem")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Imagem com Overlay de Segmentação
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(
                                (uiState.galleryBitmap.width.toFloat() / uiState.galleryBitmap.height.toFloat())
                                    .coerceIn(0.6f, 1.8f)
                            )
                    ) {
                        Image(
                            bitmap = uiState.galleryBitmap.asImageBitmap(),
                            contentDescription = "Imagem selecionada",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        DetectionOverlay(
                            detectionResult = uiState.galleryResult,
                            showBoxes = uiState.showBoxes,
                            showLabels = uiState.showLabels,
                            showMasks = uiState.showMasks,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (uiState.isProcessingGallery) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Peças Identificadas (${uiState.galleryResult?.parts?.size ?: 0})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    OutlinedButton(onClick = onPickImage) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trocar Foto")
                    }
                }
            }

            val result = uiState.galleryResult
            if (result != null) {
                item {
                    // Resumo de Desempenho
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pré-processo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${result.preprocessTimeMs} ms", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Inferência", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${result.inferenceTimeMs} ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pós-processo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${result.postprocessTimeMs} ms", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                items(result.parts) { part ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(part.partClass.color))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = part.partClass.labelPt,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = part.partClass.key,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { part.confidence },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(part.partClass.color),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${(part.confidence * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(part.partClass.color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Permissão de Câmera Necessária",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Para segmentar e identificar peças automotivas em tempo real, o aplicativo precisa de acesso à câmera do seu dispositivo.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Autorizar Câmera")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    uiState: AppUiState,
    onDismiss: () -> Unit,
    onConfChanged: (Float) -> Unit,
    onIouChanged: (Float) -> Unit,
    onOpacityChanged: (Int) -> Unit,
    onToggleBoxes: (Boolean) -> Unit,
    onToggleLabels: (Boolean) -> Unit,
    onToggleMasks: (Boolean) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Configurações de Detecção",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Slider de Confiança
            Text(
                text = "Confiança Mínima: ${(uiState.confThreshold * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = uiState.confThreshold,
                onValueChange = onConfChanged,
                valueRange = 0.10f..0.90f
            )

            // Slider de IoU (NMS)
            Text(
                text = "Limiar de Supressão (IoU NMS): ${(uiState.iouThreshold * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = uiState.iouThreshold,
                onValueChange = onIouChanged,
                valueRange = 0.20f..0.80f
            )

            // Opacidade da máscara
            Text(
                text = "Opacidade das Máscaras: ${(uiState.maskOpacity / 255f * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = uiState.maskOpacity.toFloat(),
                onValueChange = { onOpacityChanged(it.toInt()) },
                valueRange = 40f..255f
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Toggles visuais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mostrar Máscaras de Segmentação")
                Switch(checked = uiState.showMasks, onCheckedChange = onToggleMasks)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mostrar Bounding Boxes")
                Switch(checked = uiState.showBoxes, onCheckedChange = onToggleBoxes)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mostrar Nomes e Confiança")
                Switch(checked = uiState.showLabels, onCheckedChange = onToggleLabels)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerBottomSheet(
    uiState: AppUiState,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onReloadAssets: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Gerenciador do Modelo YOLO",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Status: ${if (uiState.isModelLoaded) "Ativo ✅" else "Não Carregado ⚠️"}",
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isModelLoaded) Color(0xFF00C853) else Color(0xFFFF9100)
                    )
                    Text(
                        text = "Arquivo: ${if (uiState.modelName.isNotBlank()) uiState.modelName else "Nenhum"}",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Como usar o modelo treinado:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. Como seu modelo best.tflite já foi exportado no Colab e salvo no seu Google Drive, você pode baixá-lo no celular e carregá-lo pelo botão abaixo.\n" +
                       "2. Ou coloque o arquivo best.tflite na pasta 'app/src/main/assets/' do projeto para embutir direto no APK.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Selecionar best.tflite do Armazenamento")
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onReloadAssets,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tentar Recarregar dos Assets")
            }
        }
    }
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
