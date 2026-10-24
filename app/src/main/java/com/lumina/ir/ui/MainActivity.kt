package com.lumina.ir.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lumina.ir.ui.screens.DashboardScreen
import com.lumina.ir.ui.screens.HexExplorerScreen
import com.lumina.ir.ui.screens.MusicRemoteScreen
import com.lumina.ir.ui.screens.ScenesScreen
import com.lumina.ir.ui.theme.AccentPrimary
import com.lumina.ir.ui.theme.BackgroundDeep
import com.lumina.ir.ui.theme.LuminaIRTheme
import com.lumina.ir.ui.theme.SurfaceDarkElevated
import com.lumina.ir.ui.theme.TextMuted
import com.lumina.ir.ui.theme.TextPrimary
import com.lumina.ir.ui.viewmodel.LuminaViewModel

enum class LuminaTab(val title: String, val icon: ImageVector) {
    REMOTE("Mando", Icons.Default.SettingsRemote),
    DASHBOARD("Panel Pro", Icons.Default.Lightbulb),
    SCENES("Escenas", Icons.Default.AutoAwesome),
    EXPLORER("Escáner", Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {

    private val viewModel: LuminaViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permiso concedido o denegado para notificaciones de Foreground Service */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            LuminaIRTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: LuminaViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = LuminaTab.entries

    Scaffold(
        containerColor = BackgroundDeep,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDarkElevated,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(text = tab.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = AccentPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDeep)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> MusicRemoteScreen(viewModel = viewModel)
                1 -> DashboardScreen(viewModel = viewModel)
                2 -> ScenesScreen(viewModel = viewModel)
                3 -> HexExplorerScreen(viewModel = viewModel)
            }
        }
    }
}
