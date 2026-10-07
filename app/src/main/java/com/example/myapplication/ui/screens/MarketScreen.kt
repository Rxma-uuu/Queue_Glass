package com.example.myapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.CHART_INTERVALS
import com.example.myapplication.INSTRUMENT_LIST
import com.example.myapplication.data.AuthState
import com.example.myapplication.data.SavedInvestigation
import com.example.myapplication.engine.BookDepth
import com.example.myapplication.engine.BookDepthLevel
import com.example.myapplication.engine.Candle
import com.example.myapplication.engine.StepState
import com.example.myapplication.engine.ticksToPriceString
import com.example.myapplication.intervalLabel
import com.example.myapplication.ui.components.SectionCard
import com.example.myapplication.ui.components.StatMini
import com.example.myapplication.ui.components.StatusBadge
import com.example.myapplication.ui.navigation.Destination
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
import kotlin.math.roundToInt

@Composable
fun MarketScreen(
    state: StepState,
    candles: List<Candle>,
    depth: BookDepth,
    chartInterval: Int,
    instrument: String,
    seed: Long,
    authState: AuthState,
    savedInvestigations: List<SavedInvestigation>,
    onInstrumentSelect: (String) -> Unit,
    onIntervalSelect: (Int) -> Unit,
    onInvestigateInterval: (startSeq: Long, endSeq: Long) -> Unit,
    onNavigateToDestination: (Destination) -> Unit,
    onSignOut: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    var instrumentMenuExpanded by remember { mutableStateOf(false) }
    var heatmapExpanded by remember { mutableStateOf(false) }

    val lastPrice = state.book.midpoint
    val prevClose = candles.firstOrNull()?.open?.toDouble()?.div(100.0) ?: lastPrice
    val priceDiff = lastPrice - prevClose
    val pctDiff = if (prevClose > 0) (priceDiff / prevClose) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- POLISHED HEADER WITH DATA-MODE INDICATOR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "QUEUEGLASS",
                            color = PrimaryText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusBadge("SIMULATED DATA", WarningYellow, Color(0xFF3A2E10))
                            StatusBadge("SEED $seed", CyanAccent.copy(alpha = 0.8f), ElevatedSurface)
                        }
                    }

                    // Instrument Selector Dropdown
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable { instrumentMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = instrument,
                                color = CyanAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Instrument",
                                tint = SecondaryText
                            )
                        }

                        DropdownMenu(
                            expanded = instrumentMenuExpanded,
                            onDismissRequest = { instrumentMenuExpanded = false },
                            modifier = Modifier
                                .background(PanelBackground)
                                .border(1.dp, BorderColor)
                        ) {
                            INSTRUMENT_LIST.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            item,
                                            color = if (item == instrument) CyanAccent else PrimaryText,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (item == instrument) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onInstrumentSelect(item)
                                        instrumentMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Data Mode Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSurface)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (authState) {
                            is AuthState.Authenticated -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = PositiveGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "AUTHENTICATED ACCOUNT",
                                            color = PositiveGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = authState.email,
                                            color = PrimaryText,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onSignOut,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NegativeRed),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(NegativeRed.copy(alpha = 0.6f))),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sign Out", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            is AuthState.OfflineDemo -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge("OFFLINE DEMO MODE", CyanAccent, ElevatedSurface)
                                }

                                OutlinedButton(
                                    onClick = onNavigateToAuth,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(CyanAccent.copy(alpha = 0.6f))),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = "Sign In", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sign In / Register", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            else -> {
                                Text("Offline Mode", color = SecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Ticker Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MID PRICE", color = SecondaryText, fontSize = 9.sp, letterSpacing = 1.sp)
                Text(
                    text = String.format("%.2f", lastPrice),
                    color = PrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("CHANGE", color = SecondaryText, fontSize = 9.sp, letterSpacing = 1.sp)
                Text(
                    text = String.format("%+.2f (%+.2f%%)", priceDiff, pctDiff),
                    color = if (priceDiff >= 0) PositiveGreen else NegativeRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Chart Interval Selector
        SectionCard(title = "TIMEFRAME INTERVAL") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CHART_INTERVALS.forEach { interval ->
                    val selected = interval == chartInterval
                    Button(
                        onClick = { onIntervalSelect(interval) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) CyanAccent else ElevatedSurface,
                            contentColor = if (selected) MainBackground else SecondaryText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 40.dp)
                    ) {
                        Text(
                            intervalLabel(interval),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Main Candlestick & Volume Chart
        SectionCard(title = "$instrument (${intervalLabel(chartInterval)} CANDLES)") {
            MarketCandleChart(
                candles = candles,
                bestBidTicks = state.book.best_bid,
                bestAskTicks = state.book.best_ask
            )
        }

        Spacer(Modifier.height(12.dp))

        // Visual Market Statistics
        SectionCard(title = "L3 MARKET STATISTICS") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMini("BEST BID", state.book.best_bid.ticksToPriceString(), PositiveGreen, Modifier.weight(1f), "Top bid price")
                StatMini("BEST ASK", state.book.best_ask.ticksToPriceString(), NegativeRed, Modifier.weight(1f), "Top ask price")
                StatMini("SPREAD", String.format("%.2f", state.book.spread / 100.0), PrimaryText, Modifier.weight(1f), "Ask minus Bid")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMini("BID DEPTH (5L)", state.book.bidDepth.toString(), PositiveGreen, Modifier.weight(1f), "5-level sum")
                StatMini("ASK DEPTH (5L)", state.book.askDepth.toString(), NegativeRed, Modifier.weight(1f), "5-level sum")
                StatMini(
                    "IMBALANCE",
                    String.format("%+.2f", state.book.depthImbalance),
                    if (state.book.depthImbalance >= 0) PositiveGreen else NegativeRed,
                    Modifier.weight(1f),
                    "(Bid-Ask)/(Bid+Ask)"
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMini("TRADES", state.totalTrades.toString(), PrimaryText, Modifier.weight(1f), "Total trades")
                StatMini("VOLUME", state.totalVolume.toString(), PrimaryText, Modifier.weight(1f), "Units executed")
                StatMini(
                    "VOLATILITY",
                    if (state.volatility > 0.0) String.format("%.1f bps", state.volatility) else "N/A",
                    WarningYellow,
                    Modifier.weight(1f),
                    "Rolling std dev"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- QUICK ACCESS MODULE CARDS ---
        SectionCard(title = "QUICK ACCESS MODULES") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Replay Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToDestination(Destination.Replay) }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Replay Engine", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Deterministic event stepping & checkpoint manager", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Experiments Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToDestination(Destination.Experiments) }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Experiments", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Immediate vs TWAP execution policy comparison", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Research Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToDestination(Destination.Research) }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Research", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("RAG evidence retrieval & C++ hypothesis pipeline", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- RECENT SAVED INVESTIGATIONS ---
        SectionCard(title = "RECENT SAVED INVESTIGATIONS") {
            if (savedInvestigations.isEmpty()) {
                Text(
                    text = "No saved investigations yet. Run experiments to store C++ execution evidence.",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedInvestigations.take(3).forEach { inv ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    onInvestigateInterval(inv.startSeq, inv.endSeq)
                                }
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${inv.side} ${inv.qtyUnits}u (SEQ ${inv.startSeq}-${inv.endSeq})",
                                            color = PrimaryText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (inv.note.isNotBlank()) {
                                        Text(
                                            text = inv.note,
                                            color = SecondaryText,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = "${inv.filledQty}/${inv.qtyUnits} filled",
                                    color = if (inv.unfilledQty == 0L) PositiveGreen else WarningYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Expandable Depth Heatmap / Ladder
        SectionCard(
            title = "ORDER BOOK DEPTH HEATMAP",
            action = {
                Icon(
                    imageVector = if (heatmapExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Heatmap",
                    tint = CyanAccent,
                    modifier = Modifier.clickable { heatmapExpanded = !heatmapExpanded }
                )
            }
        ) {
            DepthHeatmapView(depth = depth, expanded = heatmapExpanded)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MarketCandleChart(
    candles: List<Candle>,
    bestBidTicks: Long,
    bestAskTicks: Long
) {
    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ElevatedSurface)
                .padding(8.dp)
                .pointerInput(candles) {
                    detectTapGestures { offset ->
                        if (candles.isNotEmpty()) {
                            val idx = ((offset.x / size.width) * candles.size).roundToInt().coerceIn(0, candles.size - 1)
                            selectedCandleIndex = idx
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (candles.isEmpty()) return@Canvas

                val w = size.width
                val h = size.height

                val minPrice = candles.minOf { it.low }.toDouble()
                val maxPrice = candles.maxOf { it.high }.toDouble()
                val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)

                // Gridlines
                val gridLines = 4
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                for (i in 0..gridLines) {
                    val y = h * (i.toFloat() / gridLines)
                    drawLine(
                        color = BorderColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        pathEffect = dashEffect,
                        strokeWidth = 1f
                    )
                }

                // Best Bid/Ask horizontal lines
                if (bestBidTicks > 0) {
                    val bidY = h * (1f - ((bestBidTicks - minPrice) / priceRange).toFloat())
                    drawLine(
                        color = PositiveGreen.copy(alpha = 0.6f),
                        start = Offset(0f, bidY),
                        end = Offset(w, bidY),
                        strokeWidth = 1.5f
                    )
                }
                if (bestAskTicks > 0) {
                    val askY = h * (1f - ((bestAskTicks - minPrice) / priceRange).toFloat())
                    drawLine(
                        color = NegativeRed.copy(alpha = 0.6f),
                        start = Offset(0f, askY),
                        end = Offset(w, askY),
                        strokeWidth = 1.5f
                    )
                }

                // Candlesticks
                val candleWidth = (w / candles.size).coerceAtMost(24f)
                val bodyWidth = candleWidth * 0.7f

                candles.forEachIndexed { idx, candle ->
                    val cx = (idx + 0.5f) * candleWidth

                    val openY = h * (1f - ((candle.open - minPrice) / priceRange).toFloat())
                    val closeY = h * (1f - ((candle.close - minPrice) / priceRange).toFloat())
                    val highY = h * (1f - ((candle.high - minPrice) / priceRange).toFloat())
                    val lowY = h * (1f - ((candle.low - minPrice) / priceRange).toFloat())

                    val isGreen = candle.close >= candle.open
                    val candleColor = if (isGreen) PositiveGreen else NegativeRed

                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(cx, highY),
                        end = Offset(cx, lowY),
                        strokeWidth = 1.5f
                    )

                    // Body
                    val topY = minOf(openY, closeY)
                    val bodyHeight = maxOf(Math.abs(openY - closeY), 2f)
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(cx - bodyWidth / 2f, topY),
                        size = Size(bodyWidth, bodyHeight)
                    )
                }

                // Crosshair indicator
                selectedCandleIndex?.let { sIdx ->
                    if (sIdx in candles.indices) {
                        val cx = (sIdx + 0.5f) * candleWidth
                        drawLine(
                            color = CyanAccent,
                            start = Offset(cx, 0f),
                            end = Offset(cx, h),
                            pathEffect = dashEffect,
                            strokeWidth = 1.5f
                        )
                    }
                }
            }
        }

        selectedCandleIndex?.let { sIdx ->
            if (sIdx in candles.indices) {
                val c = candles[sIdx]
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(PanelBackground)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("O: ${c.open.ticksToPriceString()}", color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("H: ${c.high.ticksToPriceString()}", color = PositiveGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("L: ${c.low.ticksToPriceString()}", color = NegativeRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("C: ${c.close.ticksToPriceString()}", color = PrimaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("V: ${c.volume}", color = CyanAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun DepthHeatmapView(depth: BookDepth, expanded: Boolean) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Bids Heatmap
            Column(modifier = Modifier.weight(1f)) {
                Text("BIDS L3", color = PositiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                val maxBidQty = (depth.bids.maxOfOrNull { it.qty } ?: 1L).toFloat()
                val visibleBids = if (expanded) depth.bids else depth.bids.take(5)
                visibleBids.forEach { level ->
                    HeatmapRow(level, maxBidQty, PositiveGreen)
                }
            }

            // Asks Heatmap
            Column(modifier = Modifier.weight(1f)) {
                Text("ASKS L3", color = NegativeRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                val maxAskQty = (depth.asks.maxOfOrNull { it.qty } ?: 1L).toFloat()
                val visibleAsks = if (expanded) depth.asks else depth.asks.take(5)
                visibleAsks.forEach { level ->
                    HeatmapRow(level, maxAskQty, NegativeRed)
                }
            }
        }
    }
}

@Composable
private fun HeatmapRow(level: BookDepthLevel, maxQty: Float, color: Color) {
    val ratio = (level.qty / maxQty).coerceIn(0.05f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ElevatedSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(fraction = ratio)
                .background(color.copy(alpha = 0.25f))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
        ) {
            Text(
                level.price.ticksToPriceString(),
                color = PrimaryText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${level.qty} (${level.orders} orders)",
                color = SecondaryText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
