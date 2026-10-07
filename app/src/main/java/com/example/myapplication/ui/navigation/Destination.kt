package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Five primary bottom-navigation destinations for the QUEUEGLASS terminal:
 * - Market
 * - Replay
 * - Experiments
 * - Research (Research Assistant + RAG & Evidence)
 * - Settings (includes Integrations)
 */
enum class Destination(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    Market(
        title = "Market",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        description = "Live L3 candlesticks, order book depth heatmap & statistics"
    ),
    Replay(
        title = "Replay",
        icon = Icons.Default.PlayCircle,
        description = "Deterministic event replay, step controls & checkpoints"
    ),
    Experiments(
        title = "Experiments",
        icon = Icons.Default.Science,
        description = "Execution policy comparison, backtests & saved investigations"
    ),
    Research(
        title = "Research",
        icon = Icons.Default.AutoAwesome,
        description = "AI Research Assistant & RAG Evidence Retrieval"
    ),
    Settings(
        title = "Settings",
        icon = Icons.Default.Settings,
        description = "Backend config, integration hub, diagnostics & data management"
    )
}
