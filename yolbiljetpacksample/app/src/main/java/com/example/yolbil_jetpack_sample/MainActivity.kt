package com.example.yolbil_jetpack_sample

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.basarsoft.yolbil.ui.MapView
import com.example.yolbil_jetpack_sample.ui.theme.YolbiljetpacksampleTheme
import com.basarsoft.yolbil.utils.YolbilDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YolbiljetpacksampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    YolbilMapScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun YolbilMapScreen(
    modifier: Modifier = Modifier,
    viewModel: YolbilViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // İndirme durumları için state tanımları
    var downloadProgress by remember { mutableStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    // Dosya ve URL bilgileri
    val destinationFile = File(
        Environment.getExternalStorageDirectory().toString() + "/yolbilxdata/",
        "TR.vtiles"
    )
    val downloadUrl = "DOWNLOAD_URL"
    val downloadManager = YolbilDownloadManager("DOWNLOAD_HOST_URL(WITHOUT TR.VTILES)")

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MapView Display
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        viewModel.initializeMapView(this)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )

            // Navigation Button
            Button(
                onClick = {
                    viewModel.startNavigation()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Start Navigation")
            }

            // Download Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        message = "İndirme işlemi başlatılıyor..."
                        isDownloading = true
                        downloadManager.checkVersion(
                            Environment.getExternalStorageDirectory().toString() + "/yolbilxdata/",
                            object : YolbilDownloadManager.VersionListener {
                                override fun onVersionUpToDate() {
                                    coroutineScope.launch {
                                        message = "Sürüm güncel"
                                        isDownloading = false
                                    }
                                }

                                override fun onVersionOutdated() {
                                    coroutineScope.launch {
                                        message = "Sürüm eski, indirme başlatılıyor..."
                                        isDownloading = true
                                        startDownload(downloadManager, destinationFile, downloadUrl) { progress ->
                                            downloadProgress = progress
                                            isDownloading = progress < 100
                                        }
                                    }
                                }

                                override fun onError(error: String) {
                                    coroutineScope.launch {
                                        message = "Hata: $error"
                                        isDownloading = true
                                        startDownload(downloadManager, destinationFile, downloadUrl) { progress ->
                                            downloadProgress = progress
                                            isDownloading = progress < 100
                                        }
                                    }

                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Download Data")
            }

            // İndirme ilerleme çubuğu ve durum
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = downloadProgress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

suspend fun startDownload(
    downloadManager: YolbilDownloadManager,
    destinationFile: File,
    downloadUrl: String,
    onProgress: (Int) -> Unit
) {
    downloadManager.downloadFile(downloadUrl, destinationFile, object : YolbilDownloadManager.DownloadListener {
        override fun onProgress(progress: Int) {
            onProgress(progress)
        }

        override fun onSuccess(downloadedFile: File) {
            onProgress(100) // İndirme tamamlandı
        }

        override fun onError(errorMessage: String, errorType: YolbilDownloadManager.DownloadError) {
            onProgress(0) // İlerleme sıfırlanır
        }
    })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    YolbiljetpacksampleTheme {
        YolbilMapScreen()
    }
}