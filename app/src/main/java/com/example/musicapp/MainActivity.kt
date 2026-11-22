package com.example.musicapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicapp.data.AuthRepository
import com.example.musicapp.data.FavoritesRepository
import com.example.musicapp.player.PlayerManager
import com.example.musicapp.ui.AuthScreen
import com.example.musicapp.ui.MusicApp
import com.example.musicapp.ui.MusicTheme
import com.example.musicapp.ui.ThemeSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSettings.init(applicationContext)    // tema claro/oscuro guardado
        PlayerManager.init(applicationContext)
        FavoritesRepository.init(applicationContext)
        AuthRepository.init(applicationContext)   // restaura la sesión guardada
        setContent {
            MusicTheme {
                Root()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) PlayerManager.release()
    }
}

@Composable
private fun Root() {
    val user by AuthRepository.user.collectAsStateWithLifecycle()
    val current = user
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (current == null) AuthScreen() else MusicApp(current)
    }
}
