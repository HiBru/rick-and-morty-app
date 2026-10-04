package de.shinz.rickandmortyshowcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Bootstraps [App] and nothing else — all UI lives in `:shared`.
 *
 * It carries no `@Preview`: a preview here would be UI in the Android host, and
 * previewing the composition root needs a DI graph anyway.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
