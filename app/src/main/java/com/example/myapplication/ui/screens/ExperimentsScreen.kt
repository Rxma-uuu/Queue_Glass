package com.example.myapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.SavedInvestigation
import com.example.myapplication.engine.EquityPointDto
import com.example.myapplication.engine.ExecutionPolicyResultDto
import com.example.myapplication.engine.ExperimentResult
import com.example.myapplication.engine.PolicyComparisonResultDto
import com.example.myapplication.engine.StrategyPerformanceResultDto
import com.example.myapplication.engine.ticksToPriceString
import com.example.myapplication.ui.components.LabeledField
import com.example.myapplication.ui.components.SectionCard
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
fun ExperimentsScreen(
    currentSeq: Long,
    investigateStartSeq: Long,
    investigateEndSeq: Long,
    experiment: ExperimentResult?,
    previousExperiment: ExperimentResult?,
    policyComparison: PolicyComparisonResultDto?,
    strategyResult: StrategyPerformanceResultDto?,
    savedInvestigations: List<SavedInvestigation>,
    onRunExperiment: (side: String, limitTicks: Long, qty: Long, startSeq: Long, endSeq: Long) -> Unit,
    onRunPolicyComparison: (side: String, qty: Long, limitPrice: Long, arrivalDelayMs: Long, cancellationDelayMs: Long, deadlineMs: Long, slices: Int, sliceIntervalMs: Long, queueModel: String) -> Unit,
    onRunStrategyBacktest: (initialCapital: Double, maxPosition: Long, imbalanceThreshold: Double, tradeSize: Long, holdDurationMs: Long) -> Unit,
    onSaveInvestigation: (note: String) -> Unit,
    onDeleteInvestigation: (Long) -> Unit
) {
    var side by remember { mutableStateOf("BUY") }
    var qtyText by remember { mutableStateOf("300") }
    var limitText by remember { mutableStateOf("0") }
    var slicesText by remember { mutableStateOf("4") }
    var deadlineText by remember { mutableStateOf("5000") }
    var queueModel by remember { mutableStateOf("TAIL") }
    var noteText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "EXECUTION & POLICY EXPERIMENTS",
            color = PrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "C++ Engine Replay & Microstructure Simulation",
            color = SecondaryText,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(12.dp))

        // Policy & Experiment Config Card
        SectionCard(title = "1. POLICY CONFIGURATION & INPUT VALIDATION") {
            // Side Selection
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("BUY", "SELL").forEach { s ->
                    val selected = s == side
                    Button(
                        onClick = { side = s },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                selected && s == "BUY" -> PositiveGreen
                                selected && s == "SELL" -> NegativeRed
                                else -> ElevatedSurface
                            },
                            contentColor = if (selected) MainBackground else SecondaryText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(s, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("ORDER QTY (units)", qtyText, { qtyText = it }, Modifier.weight(1f))
                LabeledField("LIMIT PRICE (ticks, 0=mkt)", limitText, { limitText = it }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("TWAP SLICES", slicesText, { slicesText = it }, Modifier.weight(1f))
                LabeledField("DEADLINE (ms)", deadlineText, { deadlineText = it }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            // Queue model
            Column {
                Text("PASSIVE QUEUE MODEL", color = SecondaryText, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HEAD", "TAIL").forEach { qm ->
                        val selected = qm == queueModel
                        Button(
                            onClick = { queueModel = qm },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) CyanAccent else ElevatedSurface,
                                contentColor = if (selected) MainBackground else SecondaryText
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(qm, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            validationError?.let { err ->
                Spacer(Modifier.height(6.dp))
                Text(err, color = NegativeRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(12.dp))

            // Run Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val qty = qtyText.toLongOrNull() ?: 0L
                        if (qty <= 0) {
                            validationError = "Validation Error: Order quantity must be > 0"
                            return@Button
                        }
                        validationError = null
                        val limit = limitText.toLongOrNull() ?: 0L
                        val slices = slicesText.toIntOrNull() ?: 4
                        val deadline = deadlineText.toLongOrNull() ?: 5000L

                        onRunPolicyComparison(side, qty, limit, 0L, 0L, deadline, slices, 1000L, queueModel)
                        onRunExperiment(side, limit, qty, investigateStartSeq.coerceAtLeast(1L), 0L)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = MainBackground),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run")
                    Spacer(Modifier.width(4.dp))
                    Text("Compare 3 Policies", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Multi-Policy Execution Comparison Cards
        policyComparison?.let { comp ->
            SectionCard(title = "2. POLICY COMPARISON RESULTS vs IMMEDIATE") {
                Text(
                    text = "SHADOW DISCLAIMER: " + comp.shadowDisclaimer,
                    color = WarningYellow,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PolicySubCard("IMMEDIATE AGGRESSIVE", comp.immediate, PositiveGreen)
                    PolicySubCard("TWAP (${comp.twap.slicesExecuted} Slices)", comp.twap, CyanAccent)
                    PolicySubCard("PASSIVE LIMIT ($queueModel Queue)", comp.passive, WarningYellow)
                }

                Spacer(Modifier.height(10.dp))

                // Difference summary box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSurface)
                        .padding(10.dp)
                ) {
                    Text("COST DIFFERENTIAL vs IMMEDIATE", color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    ResultRow(
                        "TWAP COST DIFF",
                        String.format("%+.2f bps", comp.costDiffTwapVsImmediateBps),
                        valueColor = if (comp.costDiffTwapVsImmediateBps <= 0) PositiveGreen else NegativeRed
                    )
                    ResultRow(
                        "PASSIVE COST DIFF",
                        String.format("%+.2f bps", comp.costDiffPassiveVsImmediateBps),
                        valueColor = if (comp.costDiffPassiveVsImmediateBps <= 0) PositiveGreen else NegativeRed
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Single Experiment Fills & Evidence Card
        experiment?.let { exp ->
            SectionCard(title = "3. EXPERIMENTAL FILL EVIDENCE & SAVING") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                        .padding(10.dp)
                ) {
                    Text(
                        "${exp.side} ${exp.qtyUnits} units @ " + (if (exp.limitPriceTicks > 0) "LIMIT ${exp.limitPriceTicks.ticksToPriceString()}" else "MARKET"),
                        color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    ResultRow("FILLED / UNFILLED", "${exp.filledQty} / ${exp.unfilledQty}")
                    ResultRow("AVG FILL PRICE", if (exp.avgFillPrice > 0) String.format("%.2f", exp.avgFillPrice) else "-")
                    ResultRow("NOTIONAL TOTAL", String.format("%.2f", exp.totalNotional))
                    ResultRow("ESTIMATED FEES", String.format("%.4f", exp.fees))
                    ResultRow(
                        "SLIPPAGE vs MID",
                        String.format("%+.4f", exp.slippageVsArrivalMid),
                        valueColor = if (exp.slippageVsArrivalMid > 0) NegativeRed else PositiveGreen
                    )
                    ResultRow("LEVELS CONSUMED", exp.levelsConsumed.toString())
                }

                Spacer(Modifier.height(8.dp))

                // Note & Save Button
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Note (e.g. testing passive queue fill probability)", color = SecondaryText, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSaveInvestigation(noteText)
                        noteText = ""
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = CyanAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .border(1.dp, CyanAccent, RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                    Spacer(Modifier.width(6.dp))
                    Text("Save Investigation Record to Local DB", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Microstructure Strategy Backtest Extension
        StrategyBacktestSection(strategyResult = strategyResult, onRunBacktest = onRunStrategyBacktest)

        Spacer(Modifier.height(12.dp))

        // Saved Investigations List Card
        SectionCard(title = "4. SAVED EXPERIMENT HISTORY (ROOM DATABASE)") {
            if (savedInvestigations.isEmpty()) {
                Text("No saved investigations in local database.", color = SecondaryText, fontSize = 11.sp)
            } else {
                savedInvestigations.forEach { saved ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElevatedSurface)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${saved.side} ${saved.qtyUnits} units (Seed #${saved.seed})",
                                color = CyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteInvestigation(saved.id) },
                                modifier = Modifier.defaultMinSize(48.dp, 48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = NegativeRed)
                            }
                        }
                        Text(
                            "Filled: ${saved.filledQty}/${saved.qtyUnits} @ ${String.format("%.2f", saved.avgFillPrice)} | Slippage: ${String.format("%+.4f", saved.slippageVsArrivalMid)}",
                            color = PrimaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                        if (saved.note.isNotBlank()) {
                            Text("Note: ${saved.note}", color = SecondaryText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PolicySubCard(title: String, res: ExecutionPolicyResultDto, accentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurface)
            .padding(8.dp)
    ) {
        Text(title, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(2.dp))
        ResultRow("FILLED / TOTAL", "${res.filledQty} / ${res.totalQtyUnits} (${String.format("%.1f%%", res.fillRatio * 100)})")
        ResultRow("AVG FILL PRICE", if (res.avgFillPrice > 0) String.format("%.2f", res.avgFillPrice) else "N/A")
        ResultRow("EXEC COST vs MID", String.format("%+.2f bps", res.executionCostBps))
        ResultRow("MARKOUT (+100ms)", String.format("%+.2f bps", res.markout100msBps))
    }
}

@Composable
private fun StrategyBacktestSection(
    strategyResult: StrategyPerformanceResultDto?,
    onRunBacktest: (initialCapital: Double, maxPosition: Long, imbalanceThreshold: Double, tradeSize: Long, holdDurationMs: Long) -> Unit
) {
    var capitalText by remember { mutableStateOf("100000") }
    var maxPosText by remember { mutableStateOf("500") }
    var imbThreshText by remember { mutableStateOf("0.30") }
    var tradeSizeText by remember { mutableStateOf("100") }

    SectionCard(title = "MICROSTRUCTURE STRATEGY BACKTEST") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("CAPITAL ($)", capitalText, { capitalText = it }, Modifier.weight(1f))
            LabeledField("MAX POS (units)", maxPosText, { maxPosText = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("IMBALANCE THRESH", imbThreshText, { imbThreshText = it }, Modifier.weight(1f))
            LabeledField("TRADE SIZE", tradeSizeText, { tradeSizeText = it }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                val cap = capitalText.toDoubleOrNull() ?: 100000.0
                val pos = maxPosText.toLongOrNull() ?: 500L
                val imb = imbThreshText.toDoubleOrNull() ?: 0.30
                val size = tradeSizeText.toLongOrNull() ?: 100L
                onRunBacktest(cap, pos, imb, size, 2000L)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = MainBackground),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
        ) {
            Text("Run Strategy Backtest", fontWeight = FontWeight.Bold)
        }

        strategyResult?.let { res ->
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedSurface)
                    .padding(10.dp)
            ) {
                Text("ACCOUNTING SUMMARY", color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                ResultRow("REALIZED PnL", String.format("$%.2f", res.totalRealizedPnl), valueColor = if (res.totalRealizedPnl >= 0) PositiveGreen else NegativeRed)
                ResultRow("MARK-TO-MARKET EQUITY", String.format("$%.2f", res.markToMarketEquity), valueColor = CyanAccent)
                ResultRow("CLOSED TRADES", "${res.totalTradesCount} (Win Rate: ${String.format("%.1f%%", res.winRatePct)})")
                ResultRow(
                    "SHARPE RATIO",
                    if (res.sharpeAvailable) String.format("%.2f", res.sharpeRatio) else "N/A",
                    valueColor = CyanAccent
                )
            }

            if (res.equityCurve.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("EQUITY CURVE", color = SecondaryText, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                EquityCurveCanvas(res.equityCurve)
            }
        }
    }
}

@Composable
private fun EquityCurveCanvas(points: List<EquityPointDto>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurface)
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.size < 2) return@Canvas
            val w = size.width
            val h = size.height

            val minE = points.minOf { it.equity }
            val maxE = points.maxOf { it.equity }
            val range = (maxE - minE).coerceAtLeast(0.01)

            val path = Path()
            points.forEachIndexed { i, p ->
                val x = (i.toFloat() / (points.size - 1)) * w
                val normY = ((p.equity - minE) / range).toFloat()
                val y = h * (1f - normY) * 0.9f + h * 0.05f

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = CyanAccent,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, valueColor: Color = PrimaryText) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, color = SecondaryText, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
