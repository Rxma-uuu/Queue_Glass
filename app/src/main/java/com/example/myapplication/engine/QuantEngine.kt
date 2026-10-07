package com.example.myapplication.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * JNI bridge to the QUEUEGLASS C++20 quant engine.
 *
 * All quantitative logic (event generation, order-book maintenance, candle
 * aggregation, execution experiments, multi-policy comparison, strategy backtesting)
 * runs natively. Kotlin owns presentation only. Native calls return JSON payloads,
 * parsed here with kotlinx.serialization.
 */
object QuantEngine {

    init {
        System.loadLibrary("queglass")
    }

    /** Resets the engine with a fresh deterministic seed. */
    external fun nativeReset(seed: Long)

    /** Steps [count] synthetic events and returns the full state payload. */
    external fun nativeStep(count: Int): String

    /** Current state payload without stepping. */
    external fun nativeGetState(): String

    /** Candles re-aggregated from the trade log at [intervalMs]. */
    external fun nativeGetCandles(intervalMs: Int, maxCount: Int): String

    /** Current book depth levels. */
    external fun nativeGetDepth(depth: Int): String

    /** Runs a deterministic execution experiment; returns JSON result. */
    external fun nativeRunExperiment(
        side: String,
        limitPriceTicks: Long,
        qtyUnits: Long,
        startSeq: Long,
        endSeq: Long,
    ): String

    /** Compares Immediate, TWAP, and Passive Limit execution policies. */
    external fun nativeComparePolicies(
        side: String,
        qty: Long,
        limitPrice: Long,
        arrivalDelayMs: Long,
        cancellationDelayMs: Long,
        deadlineMs: Long,
        slices: Int,
        sliceIntervalMs: Long,
        queueModel: String,
        startSeq: Long,
        endSeq: Long
    ): String

    /** Runs microstructure strategy backtest. */
    external fun nativeRunStrategyBacktest(
        initialCapital: Double,
        maxPosition: Long,
        imbalanceThreshold: Double,
        tradeSize: Long,
        holdDurationMs: Long,
        startSeq: Long,
        endSeq: Long
    ): String

    external fun nativeCreateCheckpoint(): Long
    external fun nativeRestoreCheckpoint(id: Long): Boolean

    // --- Section 14-15: Integration Hub / Provider Registry ---
    external fun nativeGetIntegrationProviders(): String
    external fun nativeTestIntegration(providerId: String): String
    external fun nativeRevokeIntegration(providerId: String): String
    external fun nativeExportResearchArtifacts(): String

    // --- Section 16: MCP server ---
    external fun nativeMcpListTools(): String
    external fun nativeMcpCallTool(toolName: String, argumentsJson: String): String
    external fun nativeMcpSubmitJob(toolName: String, argumentsJson: String): String
    external fun nativeMcpGetJob(jobId: String): String
    external fun nativeMcpCancelJob(jobId: String): String
    external fun nativeMcpAuditLogs(): String

    // --- Section 17: REST surface ---
    external fun nativeRestRequest(method: String, path: String, bodyJson: String): String
    external fun nativeRestOpenApiSpec(): String

    // --- Section 18: Internal plugins ---
    external fun nativeListPlugins(): String
    external fun nativeRunPlugin(pluginId: String, inputJson: String): String

    fun createCheckpoint(): Long = nativeCreateCheckpoint()
    fun restoreCheckpoint(id: Long): Boolean = nativeRestoreCheckpoint(id)

    fun reset(seed: Long) = nativeReset(seed)

    fun step(count: Int): StepState =
        json.decodeFromString<StepState>(nativeStep(count))

    fun state(): StepState =
        json.decodeFromString<StepState>(nativeGetState())

    fun candles(intervalMs: Int, maxCount: Int = 300): List<Candle> =
        json.decodeFromString<List<Candle>>(nativeGetCandles(intervalMs, maxCount))

    fun depth(levels: Int = 10): BookDepth =
        json.decodeFromString<BookDepth>(nativeGetDepth(levels))

    fun runExperiment(config: ExperimentRequest): ExperimentResult =
        json.decodeFromString<ExperimentResult>(
            nativeRunExperiment(
                config.side,
                config.limitPriceTicks,
                config.qtyUnits,
                config.startSeq,
                config.endSeq,
            )
        )

    fun comparePolicies(
        side: String,
        qty: Long,
        limitPrice: Long,
        arrivalDelayMs: Long = 0,
        cancellationDelayMs: Long = 0,
        deadlineMs: Long = 5000,
        slices: Int = 4,
        sliceIntervalMs: Long = 1000,
        queueModel: String = "TAIL",
        startSeq: Long = 1,
        endSeq: Long = 0
    ): PolicyComparisonResultDto =
        json.decodeFromString<PolicyComparisonResultDto>(
            nativeComparePolicies(
                side, qty, limitPrice, arrivalDelayMs, cancellationDelayMs,
                deadlineMs, slices, sliceIntervalMs, queueModel, startSeq, endSeq
            )
        )

    // --- Section 14-18 typed wrappers ---

    fun integrationProviders(): List<ProviderRegistryEntryDto> =
        json.decodeFromString<List<ProviderRegistryEntryDto>>(nativeGetIntegrationProviders())

    fun testIntegration(providerId: String): IntegrationTestResultDto =
        json.decodeFromString<IntegrationTestResultDto>(nativeTestIntegration(providerId))

    fun revokeIntegration(providerId: String): ProviderRegistryEntryDto =
        json.decodeFromString<ProviderRegistryEntryDto>(nativeRevokeIntegration(providerId))

    fun exportResearchArtifacts(): String = nativeExportResearchArtifacts()

    fun mcpListTools(): McpToolListDto =
        json.decodeFromString<McpToolListDto>(nativeMcpListTools())

    fun mcpCallTool(toolName: String, argumentsJson: String): String =
        nativeMcpCallTool(toolName, argumentsJson)

    fun mcpSubmitJob(toolName: String, argumentsJson: String): McpJobDto =
        json.decodeFromString<McpJobDto>(nativeMcpSubmitJob(toolName, argumentsJson))

    fun mcpGetJob(jobId: String): McpJobDto =
        json.decodeFromString<McpJobDto>(nativeMcpGetJob(jobId))

    fun mcpCancelJob(jobId: String): Boolean =
        json.decodeFromString<McpCancelResultDto>(nativeMcpCancelJob(jobId)).cancelled

    fun mcpAuditLogs(): List<McpAuditLogDto> =
        json.decodeFromString<List<McpAuditLogDto>>(nativeMcpAuditLogs())

    fun restRequest(method: String, path: String, bodyJson: String = "{}"): RestResponseDto =
        json.decodeFromString<RestResponseDto>(nativeRestRequest(method, path, bodyJson))

    fun restOpenApiSpec(): String = nativeRestOpenApiSpec()

    fun listPlugins(): List<PluginDescriptorDto> =
        json.decodeFromString<List<PluginDescriptorDto>>(nativeListPlugins())

    fun runPlugin(pluginId: String, inputJson: String): PluginResultDto =
        json.decodeFromString<PluginResultDto>(nativeRunPlugin(pluginId, inputJson))

    fun runStrategyBacktest(
        initialCapital: Double = 100000.0,
        maxPosition: Long = 500,
        imbalanceThreshold: Double = 0.30,
        tradeSize: Long = 100,
        holdDurationMs: Long = 2000,
        startSeq: Long = 1,
        endSeq: Long = 0
    ): StrategyPerformanceResultDto =
        json.decodeFromString<StrategyPerformanceResultDto>(
            nativeRunStrategyBacktest(
                initialCapital, maxPosition, imbalanceThreshold,
                tradeSize, holdDurationMs, startSeq, endSeq
            )
        )

    private val json = Json { ignoreUnknownKeys = true }
}

// ---------------------------------------------------------------------------
// Serializable models mirroring the C++ engine output.
// ---------------------------------------------------------------------------

@Serializable
data class BookDepthLevel(
    val price: Long,
    val qty: Long,
    val orders: Int,
)

@Serializable
data class BookDepth(
    val bids: List<BookDepthLevel> = emptyList(),
    val asks: List<BookDepthLevel> = emptyList(),
)

@Serializable
data class BookSummary(
    val best_bid: Long = 0,
    val best_ask: Long = 0,
    val spread: Long = 0,
    val midpoint: Double = 0.0,
    @SerialName("bid_depth_5") val bidDepth: Long = 0,
    @SerialName("ask_depth_5") val askDepth: Long = 0,
    @SerialName("depth_imbalance") val depthImbalance: Double = 0.0,
    @SerialName("active_bid_orders") val activeBidOrders: Int = 0,
    @SerialName("active_ask_orders") val activeAskOrders: Int = 0,
)

@Serializable
data class Candle(
    @SerialName("open_time") val openTime: Long,
    @SerialName("close_time") val closeTime: Long,
    val open: Long,
    val high: Long,
    val low: Long,
    val close: Long,
    val volume: Long,
    @SerialName("trade_count") val tradeCount: Int,
)

@Serializable
data class TraceEntry(
    val event: MarketEventDto,
    val valid: Boolean,
    val code: String,
    val message: String = "",
    @SerialName("best_bid") val bestBid: Long = 0,
    @SerialName("best_ask") val bestAsk: Long = 0,
    val spread: Long = 0,
    val midpoint: Double = 0.0,
)

@Serializable
data class MarketEventDto(
    val timestamp: Long,
    @SerialName("seq_num") val seqNum: Long,
    @SerialName("order_id") val orderId: Long,
    @SerialName("event_type") val eventType: String,
    val side: String,
    @SerialName("price_ticks") val priceTicks: Long,
    @SerialName("qty_units") val qtyUnits: Long,
)

@Serializable
data class PerfStats(
    @SerialName("events_processed") val eventsProcessed: Long = 0,
    @SerialName("replay_ns") val replayNs: Long = 0,
    @SerialName("book_update_ns") val bookUpdateNs: Long = 0,
    @SerialName("candles_ns") val candlesNs: Long = 0,
    @SerialName("experiment_ns") val experimentNs: Long = 0,
    @SerialName("peak_events_per_sec") val peakEventsPerSec: Long = 0,
    @SerialName("rss_bytes") val rssBytes: Long = 0,
)

@Serializable
data class StepState(
    val book: BookSummary = BookSummary(),
    val candles: List<Candle> = emptyList(),
    val trace: List<TraceEntry> = emptyList(),
    val perf: PerfStats = PerfStats(),
    @SerialName("total_events") val totalEvents: Long = 0,
    @SerialName("total_trades") val totalTrades: Long = 0,
    @SerialName("total_volume") val totalVolume: Long = 0,
    val gaps: Long = 0,
    val rejected: Long = 0,
    val volatility: Double = 0.0,
)

@Serializable
data class FillDto(
    val timestamp: Long,
    @SerialName("seq_num") val seqNum: Long,
    @SerialName("price_ticks") val priceTicks: Long,
    @SerialName("qty_units") val qtyUnits: Long,
    val notional: Double,
)

@Serializable
data class ExperimentResult(
    val side: String = "BUY",
    @SerialName("limit_price_ticks") val limitPriceTicks: Long = 0,
    @SerialName("qty_units") val qtyUnits: Long = 0,
    @SerialName("filled_qty") val filledQty: Long = 0,
    @SerialName("unfilled_qty") val unfilledQty: Long = 0,
    @SerialName("avg_fill_price") val avgFillPrice: Double = 0.0,
    @SerialName("total_notional") val totalNotional: Double = 0.0,
    val fees: Double = 0.0,
    @SerialName("slippage_vs_arrival_mid") val slippageVsArrivalMid: Double = 0.0,
    @SerialName("arrival_mid_ticks") val arrivalMidTicks: Long = 0,
    @SerialName("levels_consumed") val levelsConsumed: Int = 0,
    @SerialName("events_evaluated") val eventsEvaluated: Long = 0,
    @SerialName("gaps_detected") val gapsDetected: Long = 0,
    @SerialName("start_seq") val startSeq: Long = 0,
    @SerialName("end_seq") val endSeq: Long = 0,
    val fills: List<FillDto> = emptyList(),
)

@Serializable
data class ExecutionPolicyResultDto(
    @SerialName("policy_type") val policyType: String = "",
    val side: String = "BUY",
    @SerialName("total_qty_units") val totalQtyUnits: Long = 0,
    @SerialName("filled_qty") val filledQty: Long = 0,
    @SerialName("remaining_qty") val remainingQty: Long = 0,
    @SerialName("fill_ratio") val fillRatio: Double = 0.0,
    @SerialName("avg_fill_price") val avgFillPrice: Double = 0.0,
    @SerialName("total_notional") val totalNotional: Double = 0.0,
    @SerialName("net_fees") val netFees: Double = 0.0,
    @SerialName("arrival_timestamp") val arrivalTimestamp: Long = 0,
    @SerialName("arrival_mid_ticks") val arrivalMidTicks: Long = 0,
    @SerialName("completion_timestamp") val completionTimestamp: Long = 0,
    @SerialName("time_to_completion_ms") val timeToCompletionMs: Long = -1,
    @SerialName("execution_cost_bps") val executionCostBps: Double = 0.0,
    @SerialName("total_fills_count") val totalFillsCount: Int = 0,
    @SerialName("slices_executed") val slicesExecuted: Int = 0,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completion_reason") val completionReason: String = "",
    @SerialName("markout_100ms_bps") val markout100msBps: Double = 0.0,
    @SerialName("markout_500ms_bps") val markout500msBps: Double = 0.0,
    val fills: List<FillDto> = emptyList()
)

@Serializable
data class PolicyComparisonResultDto(
    val immediate: ExecutionPolicyResultDto = ExecutionPolicyResultDto(),
    val twap: ExecutionPolicyResultDto = ExecutionPolicyResultDto(),
    val passive: ExecutionPolicyResultDto = ExecutionPolicyResultDto(),
    @SerialName("cost_diff_twap_vs_immediate_bps") val costDiffTwapVsImmediateBps: Double = 0.0,
    @SerialName("cost_diff_passive_vs_immediate_bps") val costDiffPassiveVsImmediateBps: Double = 0.0,
    @SerialName("is_shadow_execution") val isShadowExecution: Boolean = true,
    @SerialName("shadow_disclaimer") val shadowDisclaimer: String = "",
    @SerialName("exceeds_depth_limits") val exceedsDepthLimits: Boolean = false
)

@Serializable
data class EquityPointDto(
    val timestamp: Long = 0,
    val equity: Double = 0.0,
    @SerialName("drawdown_pct") val drawdownPct: Double = 0.0
)

@Serializable
data class StrategyTradeRecordDto(
    @SerialName("trade_id") val tradeId: Long = 0,
    val side: String = "BUY",
    @SerialName("entry_time") val entryTime: Long = 0,
    @SerialName("exit_time") val exitTime: Long = 0,
    @SerialName("entry_price_ticks") val entryPriceTicks: Long = 0,
    @SerialName("exit_price_ticks") val exitPriceTicks: Long = 0,
    @SerialName("qty_units") val qtyUnits: Long = 0,
    @SerialName("realized_pnl") val realizedPnl: Double = 0.0,
    @SerialName("total_fees") val totalFees: Double = 0.0,
    @SerialName("return_pct") val returnPct: Double = 0.0,
    @SerialName("is_closed") val isClosed: Boolean = false
)

@Serializable
data class StrategyPerformanceResultDto(
    @SerialName("initial_capital") val initialCapital: Double = 100000.0,
    @SerialName("ending_cash") val endingCash: Double = 100000.0,
    @SerialName("ending_position_units") val endingPositionUnits: Long = 0,
    @SerialName("ending_unrealized_pnl") val endingUnrealizedPnl: Double = 0.0,
    @SerialName("total_realized_pnl") val totalRealizedPnl: Double = 0.0,
    @SerialName("total_fees_paid") val totalFeesPaid: Double = 0.0,
    @SerialName("mark_to_market_equity") val markToMarketEquity: Double = 100000.0,
    @SerialName("total_trades_count") val totalTradesCount: Int = 0,
    @SerialName("winning_trades_count") val winningTradesCount: Int = 0,
    @SerialName("losing_trades_count") val losingTradesCount: Int = 0,
    @SerialName("win_rate_pct") val winRatePct: Double = 0.0,
    @SerialName("sharpe_available") val sharpeAvailable: Boolean = false,
    @SerialName("sharpe_ratio") val sharpeRatio: Double = 0.0,
    @SerialName("sharpe_sampling_frequency") val sharpeSamplingFrequency: String = "",
    @SerialName("sharpe_annualization_assumption") val sharpeAnnualizationAssumption: String = "",
    @SerialName("strategy_disclaimer") val strategyDisclaimer: String = "",
    @SerialName("equity_curve") val equityCurve: List<EquityPointDto> = emptyList(),
    val trades: List<StrategyTradeRecordDto> = emptyList()
)

/** Parameters for an execution experiment, as entered by the user. */
data class ExperimentRequest(
    val side: String,             // "BUY" or "SELL"
    val limitPriceTicks: Long,    // 0 = market
    val qtyUnits: Long,
    val startSeq: Long,
    val endSeq: Long,             // 0 = current replay position
)

/** Price ticks are hundredths; format for display. */
fun Long.ticksToPriceString(): String = String.format("%.2f", this / 100.0)

// ---------------------------------------------------------------------------
// Section 14-15: Integration Hub DTOs (provider registry + verification)
// ---------------------------------------------------------------------------

/**
 * Provider registry entry. Mirrors the C++ IntegrationHub registry record.
 * A provider only reports state "Connected" after a successful authenticated
 * capability check; unverified providers stay disabled with an explanation.
 */
@Serializable
data class ProviderRegistryEntryDto(
    val id: String = "",
    val name: String = "",
    @SerialName("integration_type") val integrationType: String = "",
    @SerialName("exact_product_identity") val exactProductIdentity: String = "",
    @SerialName("official_documentation_url") val officialDocumentationUrl: String = "",
    @SerialName("date_verified") val dateVerified: String = "",
    @SerialName("connection_method") val connectionMethod: String = "",
    @SerialName("authentication_method") val authenticationMethod: String = "",
    val capabilities: List<String> = emptyList(),
    @SerialName("compatibility_test_results") val compatibilityTestResults: String = "",
    @SerialName("connection_state") val connectionState: String = "Unverified",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("unverified_explanation") val unverifiedExplanation: String = "",
    @SerialName("last_checked_timestamp_ms") val lastCheckedTimestampMs: Long = 0,
    @SerialName("latency_ms") val latencyMs: Double = 0.0,
)

@Serializable
data class IntegrationTestResultDto(
    val success: Boolean = false,
    val provider: ProviderRegistryEntryDto = ProviderRegistryEntryDto(),
)

// ---------------------------------------------------------------------------
// Section 16: MCP server DTOs
// ---------------------------------------------------------------------------

@Serializable
data class McpToolDto(
    val name: String = "",
    val description: String = "",
)

@Serializable
data class McpToolListDto(
    @SerialName("protocolVersion") val protocolVersion: String = "",
    val tools: List<McpToolDto> = emptyList(),
)

@Serializable
data class McpJobDto(
    @SerialName("job_id") val jobId: String = "",
    @SerialName("tool_name") val toolName: String = "",
    val status: String = "PENDING",
    @SerialName("progress_pct") val progressPct: Int = 0,
    @SerialName("error_message") val errorMessage: String = "",
)

@Serializable
data class McpCancelResultDto(val cancelled: Boolean = false)

@Serializable
data class McpAuditLogDto(
    @SerialName("timestamp_ms") val timestampMs: Long = 0,
    @SerialName("client_id") val clientId: String = "",
    @SerialName("tool_name") val toolName: String = "",
    @SerialName("request_id") val requestId: String = "",
    val status: String = "",
    @SerialName("duration_ms") val durationMs: Double = 0.0,
)

// ---------------------------------------------------------------------------
// Section 17: REST response DTO
// ---------------------------------------------------------------------------

@Serializable
data class RestResponseDto(
    @SerialName("status_code") val statusCode: Int = 200,
    @SerialName("request_id") val requestId: String = "",
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
    val body: JsonElement? = null,
)

// ---------------------------------------------------------------------------
// Section 18: Internal plugin DTOs
// ---------------------------------------------------------------------------

@Serializable
data class PluginResourceBudgetDto(
    @SerialName("max_events_scanned") val maxEventsScanned: Long = 0,
    @SerialName("max_output_bytes") val maxOutputBytes: Long = 0,
    @SerialName("max_runtime_hint_ms") val maxRuntimeHintMs: Long = 0,
)

@Serializable
data class PluginDescriptorDto(
    val id: String = "",
    val version: String = "",
    val description: String = "",
    @SerialName("required_data_capabilities") val requiredDataCapabilities: List<String> = emptyList(),
    val budget: PluginResourceBudgetDto = PluginResourceBudgetDto(),
)

@Serializable
data class PluginEvidenceRefDto(
    @SerialName("evidence_id") val evidenceId: String = "",
    val kind: String = "",
    val reference: String = "",
    val checksum: String = "",
)

/** Evidence output contract every internal plugin must satisfy. */
@Serializable
data class PluginResultDto(
    @SerialName("plugin_id") val pluginId: String = "",
    @SerialName("plugin_version") val pluginVersion: String = "",
    val success: Boolean = false,
    val error: String? = null,
    @SerialName("events_scanned") val eventsScanned: Long = 0,
    @SerialName("runtime_ms") val runtimeMs: Long = 0,
    val evidence: List<PluginEvidenceRefDto> = emptyList(),
    val output: JsonElement? = null,
)
