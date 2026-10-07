package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.TerminalViewModel
import com.example.myapplication.engine.BookDepth
import com.example.myapplication.engine.BookDepthLevel
import com.example.myapplication.engine.StepState
import com.example.myapplication.engine.TraceEntry
import com.example.myapplication.engine.ticksToPriceString
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
fun ReplayScreen(
    state: StepState,
    depth: BookDepth,
    isPlaying: Boolean,
    speed: Int,
    loading: Boolean,
    checkpoints: List<Long>,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSpeedSelect: (Int) -> Unit,
    onStep: (Int) -> Unit,
    onNewSession: () -> Unit,
    onCreateCheckpoint: () -> Unit,
    onRestoreCheckpoint: (Long) -> Unit
) {
    var selectedLevel by remember { mutableStateOf<BookDepthLevel?>(null) }
    var selectedLevelSide by remember { mutableStateOf<String?>(null) }

    val progress = (state.totalEvents.toFloat() / TerminalViewModel.MAX_EVENTS).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DETERMINISTIC REPLAY",
                    color = PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "L3 Match Event Sequence Engine",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
            StatusBadge("CHECKPOINTS: ${checkpoints.size}", CyanAccent, ElevatedSurface)
        }

        Spacer(Modifier.height(12.dp))

        // Progress bar & Sequence Timeline
        SectionCard(title = "EVENT SEQUENCE TIMELINE") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SEQ ${state.totalEvents} / ${TerminalViewModel.MAX_EVENTS}",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CyanAccent,
                trackColor = ElevatedSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        // Replay Playback Toolbar
        SectionCard(title = "PLAYBACK CONTROLS") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause
                Button(
                    onClick = if (isPlaying) onPause else onPlay,
                    enabled = !loading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = MainBackground
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPlaying) "Pause" else "Play", fontWeight = FontWeight.Bold)
                }

                // Step +25
                Button(
                    onClick = { onStep(25) },
                    enabled = !loading && !isPlaying,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElevatedSurface,
                        contentColor = CyanAccent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.FastForward, contentDescription = "Step +25")
                    Spacer(Modifier.width(2.dp))
                    Text("+25", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // Step +1
                Button(
                    onClick = { onStep(1) },
                    enabled = !loading && !isPlaying,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElevatedSurface,
                        contentColor = CyanAccent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Step +1")
                    Spacer(Modifier.width(2.dp))
                    Text("+1", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // New Session
                Button(
                    onClick = onNewSession,
                    enabled = !loading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElevatedSurface,
                        contentColor = SecondaryText
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                }
            }

            Spacer(Modifier.height(10.dp))

            // Speed Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "REPLAY SPEED",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                listOf(1, 2, 4).forEach { mult ->
                    val selected = mult == speed
                    Button(
                        onClick = { onSpeedSelect(mult) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) CyanAccent else ElevatedSurface,
                            contentColor = if (selected) MainBackground else SecondaryText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("${mult}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Checkpoint Seeking Manager
        SectionCard(title = "CHECKPOINT SEEKING MANAGER") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCreateCheckpoint,
                    enabled = !loading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElevatedSurface,
                        contentColor = WarningYellow
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Create Checkpoint")
                    Spacer(Modifier.width(6.dp))
                    Text("Create Checkpoint", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (checkpoints.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("SAVED CHECKPOINTS", color = SecondaryText, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    checkpoints.forEach { cpId ->
                        Button(
                            onClick = { onRestoreCheckpoint(cpId) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PanelBackground,
                                contentColor = CyanAccent
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        ) {
                            Text("CP #$cpId", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Order Book View with Selected Level Detail
        SectionCard(title = "SYNCHRONIZED ORDER BOOK (LADDER)") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bids
                Column(modifier = Modifier.weight(1f)) {
                    Text("BIDS (BUY)", color = PositiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    depth.bids.take(8).forEach { level ->
                        ReplayLadderRow(
                            level = level,
                            sideColor = PositiveGreen,
                            isSelected = selectedLevel == level,
                            onClick = {
                                selectedLevel = level
                                selectedLevelSide = "BID"
                            }
                        )
                    }
                }

                // Asks
                Column(modifier = Modifier.weight(1f)) {
                    Text("ASKS (SELL)", color = NegativeRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    depth.asks.take(8).forEach { level ->
                        ReplayLadderRow(
                            level = level,
                            sideColor = NegativeRed,
                            isSelected = selectedLevel == level,
                            onClick = {
                                selectedLevel = level
                                selectedLevelSide = "ASK"
                            }
                        )
                    }
                }
            }

            selectedLevel?.let { lvl ->
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, CyanAccent, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "SELECTED LEVEL DETAILS ($selectedLevelSide @ ${lvl.price.ticksToPriceString()})",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL QTY:", color = SecondaryText, fontSize = 11.sp)
                        Text("${lvl.qty} units", color = PrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ACTIVE ORDERS:", color = SecondaryText, fontSize = 11.sp)
                        Text("${lvl.orders} orders", color = PrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("AVG ORDER SIZE:", color = SecondaryText, fontSize = 11.sp)
                        val avgSize = if (lvl.orders > 0) lvl.qty / lvl.orders else 0
                        Text("$avgSize units/order", color = PrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Synchronized Trace Stream
        SectionCard(title = "EVENT TRACE LOG (LAST ${state.trace.size} EVENTS)") {
            TraceStreamView(traceList = state.trace)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReplayLadderRow(
    level: BookDepthLevel,
    sideColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) sideColor.copy(alpha = 0.3f) else ElevatedSurface)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) sideColor else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                level.price.ticksToPriceString(),
                color = PrimaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${level.qty}",
                color = sideColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TraceStreamView(traceList: List<TraceEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurface)
            .padding(6.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (traceList.isEmpty()) {
            Text("No trace entries yet.", color = SecondaryText, fontSize = 11.sp)
        }
        traceList.takeLast(30).reversed().forEach { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "#${entry.event.seqNum}",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(48.dp)
                )
                Text(
                    text = entry.event.eventType,
                    color = when (entry.event.eventType) {
                        "EXECUTE" -> PositiveGreen
                        "CANCEL" -> NegativeRed
                        "ADD" -> CyanAccent
                        else -> WarningYellow
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(64.dp)
                )
                Text(
                    text = "${entry.event.side} ${entry.event.qtyUnits} @ ${entry.event.priceTicks.ticksToPriceString()}",
                    color = PrimaryText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
