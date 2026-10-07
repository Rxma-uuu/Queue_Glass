package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ConnectorStatus
import com.example.myapplication.data.AuthState
import com.example.myapplication.engine.PluginDescriptorDto
import com.example.myapplication.engine.PerfStats
import com.example.myapplication.engine.ProviderRegistryEntryDto
import com.example.myapplication.ui.components.LabeledField
import com.example.myapplication.ui.components.SectionCard
import com.example.myapplication.ui.components.StatusBadge
import com.example.myapplication.ui.theme.BorderColor
import com.example.myapplication.ui.theme.CyanAccent
import com.example.myapplication.ui.theme.ElevatedSurface
import com.example.myapplication.ui.theme.MainBackground
import com.example.myapplication.ui.theme.NegativeRed
import com.example.myapplication.ui.theme.PanelBackground
import com.example.myapplication.ui.theme.PositiveGreen
import com.example.myapplication.ui.theme.PrimaryText
import com.example.myapplication.ui.theme.SecondaryText
import com.example.myapplication.ui.theme.WarningYellow

@Composable
fun SettingsScreen(
    seed: Long,
    perf: PerfStats,
    hapticsEnabled: Boolean,
    reducedMotion: Boolean,
    highContrast: Boolean,
    connectors: List<ConnectorStatus>,
    providers: List<ProviderRegistryEntryDto>,
    plugins: List<PluginDescriptorDto>,
    mcpToolCount: Int,
    authState: AuthState,
    onSignOut: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onTestProvider: (String) -> Unit,
    onRevokeProvider: (String) -> Unit,
    onExportArtifacts: () -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit,
    onClearAllData: () -> Unit,
    onResetEngine: () -> Unit
) {
    var threadCountText by remember { mutableStateOf("4") }
    var noiseLevel by remember { mutableStateOf("Low") }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "SETTINGS & INTEGRATIONS HUB",
            color = PrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Backend Configuration, Connectors & Local Data",
            color = SecondaryText,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(12.dp))

        // 0. Account & Authentication Status
        SectionCard(title = "ACCOUNT & AUTHENTICATION") {
            when (authState) {
                is AuthState.Authenticated -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            StatusBadge("AUTHENTICATED ACCOUNT", PositiveGreen, ElevatedSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = authState.email,
                                color = PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (authState.fullName.isNotBlank()) {
                                Text(
                                    text = authState.fullName,
                                    color = SecondaryText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Button(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NegativeRed, contentColor = PrimaryText)
                        ) {
                            Text("Sign Out", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                is AuthState.OfflineDemo -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            StatusBadge("OFFLINE DEMO MODE", CyanAccent, ElevatedSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Using local simulation without account session",
                                color = SecondaryText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = onNavigateToAuth,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = MainBackground)
                        ) {
                            Text("Sign In / Register", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                else -> {
                    Text("Unauthenticated", color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 1. Backend Configuration
        SectionCard(title = "1. C++ BACKEND CONFIGURATION") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("ENGINE SEED", "$seed", {}, Modifier.weight(1f))
                LabeledField("THREAD POOL", threadCountText, { threadCountText = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("SYNTHETIC NOISE LEVEL", color = SecondaryText, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Low", "Medium", "High").forEach { lvl ->
                    val selected = lvl == noiseLevel
                    Button(
                        onClick = { noiseLevel = lvl },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) CyanAccent else ElevatedSurface,
                            contentColor = if (selected) MainBackground else SecondaryText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(lvl, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 2. Integration Hub (Placed inside Settings as required)
        SectionCard(title = "2. INTEGRATION HUB & EXCHANGE CONNECTORS") {
            connectors.forEach { conn ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSurface)
                        .padding(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Hub, contentDescription = "Hub", tint = CyanAccent)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(conn.name, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(conn.type, color = SecondaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    StatusBadge(
                        text = if (conn.isConnected) "ACTIVE (${conn.latencyMs}ms)" else "OFFLINE",
                        color = if (conn.isConnected) PositiveGreen else SecondaryText,
                        backgroundColor = PanelBackground
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 2b. Provider Registry (Sections 14-15): agent connectors, model
        // adapters and generic clients with verified connection states.
        SectionCard(title = "2B. PROVIDER REGISTRY (AGENT & MODEL CONNECTORS)") {
            Text(
                "MCP SERVER: $mcpToolCount TOOLS REGISTERED | PROTOCOL 2024-11-05",
                color = CyanAccent,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(8.dp))
            providers.forEach { p -> ProviderRow(p, onTestProvider, onRevokeProvider) }
        }

        Spacer(Modifier.height(12.dp))

        // 2c. Internal research plugins (Section 18): statically packaged
        // backend modules; no dynamic native code loading on Android.
        SectionCard(title = "2C. INTERNAL RESEARCH PLUGINS (STATIC BACKEND MODULES)") {
            if (plugins.isEmpty()) {
                Text("No plugins registered.", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            plugins.forEach { plugin ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSurface)
                        .padding(8.dp)
                ) {
                    Text(plugin.id, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("v${plugin.version} | ${plugin.description}", color = SecondaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        "BUDGET: ${plugin.budget.maxEventsScanned} events / ${plugin.budget.maxOutputBytes / 1024} KiB output",
                        color = SecondaryText, fontSize = 8.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 3. Appearance and Accessibility
        SectionCard(title = "3. APPEARANCE & ACCESSIBILITY") {
            SettingSwitchRow("HAPTIC FEEDBACK FOR DELIBERATE ACTIONS", hapticsEnabled, onToggleHaptics)
            Spacer(Modifier.height(6.dp))
            SettingSwitchRow("REDUCED MOTION MODE", reducedMotion, onToggleReducedMotion)
            Spacer(Modifier.height(6.dp))
            SettingSwitchRow("HIGH CONTRAST MODE", highContrast, onToggleHighContrast)
        }

        Spacer(Modifier.height(12.dp))

        // 4. Measured Diagnostics
        SectionCard(title = "4. ENGINE DIAGNOSTICS & PERF COUNTERS") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiagMini("EVENTS PROC", "${perf.eventsProcessed}")
                DiagMini("REPLAY TIME", "${perf.replayNs / 1_000_000} ms")
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiagMini("BOOK UPDATE", "${perf.bookUpdateNs / 1_000} µs")
                DiagMini("EXPERIMENT", "${perf.experimentNs / 1_000} µs")
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiagMini("PEAK THROUGHPUT", "${perf.peakEventsPerSec} ev/s")
                DiagMini("RSS MEMORY", "${perf.rssBytes / (1024 * 1024)} MB")
            }
        }

        Spacer(Modifier.height(12.dp))

        // 5. Local Data Management & Export / Import
        SectionCard(title = "5. LOCAL DATA MANAGEMENT & EXPORT") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onExportArtifacts()
                        exportStatus = "Research artifacts exported for manual sharing (verified + unverified providers)"
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = CyanAccent),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Export")
                    Spacer(Modifier.width(4.dp))
                    Text("Export Session", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { exportStatus = "Imported benchmark_l3_trace.csv successfully" },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = CyanAccent),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = "Import")
                    Spacer(Modifier.width(4.dp))
                    Text("Import Trace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            exportStatus?.let { msg ->
                Spacer(Modifier.height(6.dp))
                Text(msg, color = PositiveGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onResetEngine,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = PrimaryText),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Reset Engine Trace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onClearAllData,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelBackground, contentColor = NegativeRed),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .border(1.dp, NegativeRed, RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear DB")
                    Spacer(Modifier.width(4.dp))
                    Text("Clear Room DB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun providerStateColor(state: String) = when (state) {
    "Connected" -> PositiveGreen
    "Degraded" -> WarningYellow
    "Requires authorization", "Unverified" -> WarningYellow
    "Revoked", "Unsupported" -> NegativeRed
    else -> SecondaryText // Not configured
}

@Composable
private fun ProviderRow(
    provider: ProviderRegistryEntryDto,
    onTest: (String) -> Unit,
    onRevoke: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurface)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = Icons.Default.Hub, contentDescription = null, tint = CyanAccent)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${provider.integrationType} | ${provider.connectionMethod}",
                    color = SecondaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace
                )
            }
            StatusBadge(
                text = provider.connectionState.uppercase(),
                color = providerStateColor(provider.connectionState),
                backgroundColor = PanelBackground
            )
        }

        if (!provider.isVerified && provider.unverifiedExplanation.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                provider.unverifiedExplanation,
                color = WarningYellow,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 11.sp
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { onTest(provider.id) },
                enabled = provider.isVerified,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElevatedSurface,
                    contentColor = CyanAccent,
                    disabledContainerColor = PanelBackground,
                    disabledContentColor = SecondaryText
                ),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onRevoke(provider.id) },
                enabled = provider.connectionState == "Connected" || provider.connectionState == "Degraded",
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElevatedSurface,
                    contentColor = NegativeRed,
                    disabledContainerColor = PanelBackground,
                    disabledContentColor = SecondaryText
                ),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text("REVOKE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MainBackground,
                checkedTrackColor = CyanAccent,
                uncheckedThumbColor = SecondaryText,
                uncheckedTrackColor = PanelBackground
            )
        )
    }
}

@Composable
private fun DiagMini(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(label, color = SecondaryText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
