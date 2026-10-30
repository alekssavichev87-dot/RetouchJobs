package com.example.retouchjobs

import androidx.compose.runtime.remember
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.retouchjobs.ui.MainScreen
import com.example.retouchjobs.ui.MainViewModel
import com.example.retouchjobs.ui.theme.RetouchJobsTheme

fun main() = application {
    val viewModel = remember { MainViewModel() }
    val icon = remember { BitmapPainter(useResource("icon.png", ::loadImageBitmap)) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Ретушь для памятников — поиск вакансий",
        icon = icon,
        state = rememberWindowState(size = DpSize(520.dp, 900.dp)),
    ) {
        RetouchJobsTheme {
            MainScreen(viewModel)
        }
    }
}
