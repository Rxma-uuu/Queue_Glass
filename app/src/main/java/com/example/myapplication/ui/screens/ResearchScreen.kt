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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ProposedExperimentConfig
import com.example.myapplication.ResearchMessage
import com.example.myapplication.engine.StepState
import com.example.myapplication.intervalLabel
import com.example.myapplication.ui.components.SectionCard
import com.example.myapplication.ui.components.StatusBadge
import com.example.myapplication.ui.theme.BorderColor
import com.example.myapplication.ui.theme.CyanAccent
import com.example.myapplication.ui.theme.ElevatedSurface
import com.example.myapplication.ui.theme.MainBackground
import com.example.myapplication.ui.theme.PanelBackground
import com.example.myapplication.ui.theme.PositiveGreen
import com.example.myapplication.ui.theme.PrimaryText
import com.example.myapplication.ui.theme.SecondaryText
import com.example.myapplication.ui.theme.WarningYellow

val WORKFLOW_STEPS = listOf(
    "Observation",
    "Hypothesis",
    "Validated experiment",
    "C++ execution",
    "Results",
    "Visualization",
    "Explanation",
    "Experiment memory"
)

data class KnowledgeDoc(
    val title: String,
    val category: String, // "Exchange Spec", "Methodology", "Research Paper", "Internal Doc"
    val source: String,
    val version: String,
    val section: String,
    val timestamp: String,
    val snippet: String
)

val KNOWLEDGE_BASE_DOCS = listOf(
    KnowledgeDoc(
        title = "CME L3 Matching Engine Specification",
        category = "Exchange Spec",
        source = "Exchange-Rulebook-2025.pdf",
        version = "v2.4.1",
        section = "Section 4.2 - Price-Time Queue Priority",
        timestamp = "2025-01-15 08:30:00 UTC",
        snippet = "Limit orders at the same price level are executed in strict FIFO order according to arrival sequence number. Cancellations remove queue position permanently."
    ),
    KnowledgeDoc(
        title = "TWAP & Optimal Execution Methodology",
        category = "Methodology",
        source = "Quant-Execution-Handbook.pdf",
        version = "v1.0.8",
        section = "Chapter 3 - Discrete Slicing & Market Impact",
        timestamp = "2025-02-01 10:15:00 UTC",
        snippet = "Slicing parent orders into uniform time-spaced child orders minimizes variance and square-root market impact, outperforming immediate aggressive sweep."
    ),
    KnowledgeDoc(
        title = "Order Book Imbalance & Short-Term Alpha",
        category = "Research Paper",
        source = "Journal-of-Microstructure-2024.pdf",
        version = "v3.1.0",
        section = "Section 2 - Depth Ratio & Volatility",
        timestamp = "2024-11-20 14:00:00 UTC",
        snippet = "Top-5 level depth imbalance exceeding +0.30 predicts a high probability of upward mid-price movement over 500ms horizon."
    )
)

@Composable
fun ResearchScreen(
    state: StepState,
    instrument: String,
    chartInterval: Int,
    currentStep: String,
    messages: List<ResearchMessage>,
    onAskAssistant: (String) -> Unit,
    onLaunchProposed: (ProposedExperimentConfig) -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("ASSISTANT") } // "ASSISTANT" or "RAG_EVIDENCE"
    var selectedCategory by remember { mutableStateOf("ALL") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QUANT RESEARCH ASSISTANT",
                    color = PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "RAG Evidence Retrieval & Validated C++ Pipeline",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
            StatusBadge("OFFLINE RAG", CyanAccent, ElevatedSurface)
        }

        Spacer(Modifier.height(12.dp))

        // Guided Research Workflow Stepper
        SectionCard(title = "RESEARCH WORKFLOW PIPELINE") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WORKFLOW_STEPS.take(4).forEach { step ->
                    val isActive = step.equals(currentStep, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) CyanAccent else ElevatedSurface)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step,
                            color = if (isActive) MainBackground else SecondaryText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WORKFLOW_STEPS.drop(4).forEach { step ->
                    val isActive = step.equals(currentStep, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) CyanAccent else ElevatedSurface)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step,
                            color = if (isActive) MainBackground else SecondaryText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Context Inspector Card
        SectionCard(title = "STRUCTURED CONTEXT INSPECTOR") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContextTag("DATASET ID", "DS-SIM-L3-2025")
                ContextTag("INSTRUMENT", instrument)
                ContextTag("TIMEFRAME", intervalLabel(chartInterval))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContextTag("MODE", "OFFLINE DETERMINISTIC")
                ContextTag("MID PRICE", String.format("%.2f", state.book.midpoint))
                ContextTag("EVENTS REPLAYED", "${state.totalEvents}")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Main Tab Selector (Research Assistant vs RAG Evidence)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { activeTab = "ASSISTANT" },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == "ASSISTANT") CyanAccent else ElevatedSurface,
                    contentColor = if (activeTab == "ASSISTANT") MainBackground else SecondaryText
                ),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(imageVector = Icons.Default.Psychology, contentDescription = "Assistant")
                Spacer(Modifier.width(6.dp))
                Text("AI Assistant", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { activeTab = "RAG_EVIDENCE" },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == "RAG_EVIDENCE") CyanAccent else ElevatedSurface,
                    contentColor = if (activeTab == "RAG_EVIDENCE") MainBackground else SecondaryText
                ),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = "RAG Evidence")
                Spacer(Modifier.width(6.dp))
                Text("RAG Knowledge Base", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (activeTab == "ASSISTANT") {
            // Research Assistant Chat View
            SectionCard(title = "RESEARCH ASSISTANT CHAT & PROPOSALS") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    messages.forEach { msg ->
                        AssistantBubble(msg = msg, onLaunchProposed = onLaunchProposed)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Prompt Shortcuts
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PromptChip("Analyze Order Book Imbalance") { onAskAssistant("Analyze order book imbalance for $instrument") }
                    PromptChip("Compare TWAP vs Immediate") { onAskAssistant("Compare TWAP vs Immediate execution cost") }
                }

                Spacer(Modifier.height(8.dp))

                // Query input bar
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        placeholder = { Text("Ask Research Assistant...", color = SecondaryText, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = PanelBackground,
                            unfocusedContainerColor = PanelBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (queryText.isNotBlank()) {
                                onAskAssistant(queryText)
                                queryText = ""
                            }
                        },
                        modifier = Modifier
                            .defaultMinSize(48.dp, 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanAccent)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MainBackground)
                    }
                }
            }
        } else {
            // RAG Evidence & Citation Document Retrieval View
            SectionCard(title = "VERIFIED RAG KNOWLEDGE BASE & DOCUMENT CITATIONS") {
                // Category Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL", "Exchange Spec", "Methodology", "Research Paper").forEach { cat ->
                        val selected = cat == selectedCategory
                        Button(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) CyanAccent else ElevatedSurface,
                                contentColor = if (selected) MainBackground else SecondaryText
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                val filteredDocs = if (selectedCategory == "ALL") KNOWLEDGE_BASE_DOCS else KNOWLEDGE_BASE_DOCS.filter { it.category == selectedCategory }

                filteredDocs.forEach { doc ->
                    DocCitationCard(doc)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ContextTag(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PanelBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            Text(label, color = SecondaryText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun AssistantBubble(msg: ResearchMessage, onLaunchProposed: (ProposedExperimentConfig) -> Unit) {
    val isUser = msg.sender == "USER"
    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (msg.isSummary && !isUser) {
            Text(
                text = "[DETERMINISTIC OFFLINE SUMMARY]",
                color = WarningYellow,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(2.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isUser) CyanAccent.copy(alpha = 0.2f) else PanelBackground)
                .border(1.dp, if (isUser) CyanAccent else BorderColor, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Text(
                text = msg.text,
                color = PrimaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp
            )
        }

        if (msg.citedSources.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                msg.citedSources.forEach { src ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElevatedSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("cite: $src", color = SecondaryText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        msg.proposedExperiment?.let { prop ->
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { onLaunchProposed(prop) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen, contentColor = MainBackground),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run Proposed")
                Spacer(Modifier.width(4.dp))
                Text("Launch Proposed Experiment (${prop.side} ${prop.qty} TWAP)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PromptChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text, color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DocCitationCard(doc: KnowledgeDoc) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = Icons.Default.Description, contentDescription = "Doc", tint = CyanAccent)
            Spacer(Modifier.width(6.dp))
            Text(
                doc.title,
                color = PrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(doc.category, PositiveGreen, PanelBackground)
        }
        Spacer(Modifier.height(4.dp))
        Text("Source: ${doc.source} | Version: ${doc.version} | Section: ${doc.section}", color = SecondaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text("Retrieved: ${doc.timestamp}", color = SecondaryText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(PanelBackground)
                .padding(8.dp)
        ) {
            Text(doc.snippet, color = PrimaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
        }
    }
}
