package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: ThemeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          Box(modifier = Modifier.fillMaxSize()) {
            // Animated blood fluid background that ticks behind apps
            BloodDripBg(viewModel = viewModel) {
              AnimatedContent(
                targetState = viewModel.activeScreen,
                transitionSpec = {
                  (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                      (slideOutVertically { height -> -height / 2 } + fadeOut())
                },
                label = "AppScreenTransition"
              ) { screen ->
                when (screen) {
                  "HOME" -> HomeScreenLayout(viewModel) { destination ->
                    viewModel.activeScreen = destination
                  }
                  "CRUOR" -> CruorCoreApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "NECRO" -> NecroDialerApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "KILN" -> KilnEyeApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "MESG" -> MesgVoidApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "TEMP" -> TempClockApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "VOIDEX" -> VoidexMallApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "SENS" -> SensBoardApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "RITUAL" -> RitualPaintApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "WHATSAPP" -> SanguineChatApp(viewModel) { viewModel.activeScreen = "HOME" }
                  "FACEBOOK" -> SoulBookApp(viewModel) { viewModel.activeScreen = "HOME" }
                }
              }
            }

            // CRT static scanline overlay reflecting retro-occult systems
            ScanlineOverlay()
          }
        }
      }
    }
  }
}

