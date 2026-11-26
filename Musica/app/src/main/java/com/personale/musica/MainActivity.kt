package com.personale.musica

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personale.musica.ui.MusicaApp
import com.personale.musica.ui.MusicaTheme

class MainActivity : ComponentActivity() {

    private val audioPermission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MusicaTheme {
                val vm: PlayerViewModel = viewModel()
                var granted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, audioPermission) ==
                            PackageManager.PERMISSION_GRANTED
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result -> granted = result[audioPermission] == true }

                LaunchedEffect(Unit) {
                    val perms = buildList {
                        add(audioPermission)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    launcher.launch(perms.toTypedArray())
                }
                LaunchedEffect(granted) { if (granted) vm.loadSongs() }

                MusicaApp(vm = vm, permissionGranted = granted, onRequestPermission = {
                    launcher.launch(arrayOf(audioPermission))
                })
            }
        }
    }
}
