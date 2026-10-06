package com.keyserdsoze.cardreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.keyserdsoze.cardreader.ui.CardReaderApp
import com.keyserdsoze.cardreader.ui.theme.CardReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CardReaderTheme { CardReaderApp() } }
    }
}
