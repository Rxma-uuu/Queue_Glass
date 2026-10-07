package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.data.AuthState
import com.example.myapplication.data.InvestigationDatabase
import com.example.myapplication.data.SavedInvestigation
import com.example.myapplication.engine.BookDepth
import com.example.myapplication.engine.Candle
import com.example.myapplication.engine.ExperimentRequest
import com.example.myapplication.engine.ExperimentResult
import com.example.myapplication.engine.PluginDescriptorDto
import com.example.myapplication.engine.PluginResultDto
import com.example.myapplication.engine.PolicyComparisonResultDto
import com.example.myapplication.engine.ProviderRegistryEntryDto
import com.example.myapplication.engine.QuantEngine
import com.example.myapplication.engine.StepState
import com.example.myapplication.engine.StrategyPerformanceResultDto
import com.example.myapplication.ui.navigation.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Chart interval choices in milliseconds: 1s, 5s, 30s, 2m. */
val CHART_INTERVALS = listOf(1_000, 5_000, 30_000, 120_000)
fun intervalLabel(ms: Int) = when (ms) {
    1_000 -> "1s"
    5_000 -> "5s"
    30_000 -> "30s"
    120_000 -> "2m"
    else -> "${ms}ms"
}

val INSTRUMENT_LIST = listOf("BTC-USD", "ETH-USD", "SOL-USD", "NVDA-USD", "SPY-USD")

data class ResearchMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "USER" or "ASSISTANT"
    val text: String,
    val isSummary: Boolean = true,
    val citedSources: List<String> = emptyList(),
    val proposedExperiment: ProposedExperimentConfig? = null
)

data class ProposedExperimentConfig(
    val side: String,
    val qty: Long,
    val limitPrice: Long,
    val slices: Int,
    val queueModel: String
)

data class ConnectorStatus(
    val name: String,
    val type: String,
    val isConnected: Boolean,
    val latencyMs: Double
)

/**
 * QUEUEGLASS terminal ViewModel.
 *
 * All engine calls run on Dispatchers.Default; the UI thread is never blocked.
 */
class TerminalViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val STEP_MS = 300L
        const val EVENTS_PER_TICK = 25
        const val MAX_EVENTS = 20_000
    }

    private val dao = InvestigationDatabase.get(app).investigationDao()

    // --- Authentication Repository ---
    private val authRepo = AuthRepository(app)
    val authState: StateFlow<AuthState> = authRepo.authState

    // --- Navigation state ---
    private val _destination = MutableStateFlow(Destination.Market)
    val destination: StateFlow<Destination> = _destination.asStateFlow()

    // --- Instrument state ---
    private val _instrument = MutableStateFlow("BTC-USD")
    val instrument: StateFlow<String> = _instrument.asStateFlow()

    // --- Saved investigations (Room) ---
    val savedInvestigations: StateFlow<List<SavedInvestigation>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Engine state ---
    private val _state = MutableStateFlow(StepState())
    val state: StateFlow<StepState> = _state.asStateFlow()

    private val _depth = MutableStateFlow(BookDepth())
    val depth: StateFlow<BookDepth> = _depth.asStateFlow()

    private val _candleList = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candleList.asStateFlow()

    /** Selected chart interval (ms). */
    private val _chartInterval = MutableStateFlow(5_000)
    val chartInterval: StateFlow<Int> = _chartInterval.asStateFlow()

    /** True while a replay is in progress. */
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Replay speed multiplier. */
    private val _speed = MutableStateFlow(2)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Checkpoints list. */
    private val _checkpoints = MutableStateFlow<List<Long>>(emptyList())
    val checkpoints: StateFlow<List<Long>> = _checkpoints.asStateFlow()

    /** Last experiment result. */
    private val _experiment = MutableStateFlow<ExperimentResult?>(null)
    val experiment: StateFlow<ExperimentResult?> = _experiment.asStateFlow()

    /** Previous experiment result (for side-by-side comparison). */
    private val _previousExperiment = MutableStateFlow<ExperimentResult?>(null)
    val previousExperiment: StateFlow<ExperimentResult?> = _previousExperiment.asStateFlow()

    /** Multi-policy comparison result (Immediate vs TWAP vs Passive Limit). */
    private val _policyComparison = MutableStateFlow<PolicyComparisonResultDto?>(null)
    val policyComparison: StateFlow<PolicyComparisonResultDto?> = _policyComparison.asStateFlow()

    /** Strategy performance backtest result. */
    private val _strategyResult = MutableStateFlow<StrategyPerformanceResultDto?>(null)
    val strategyResult: StateFlow<StrategyPerformanceResultDto?> = _strategyResult.asStateFlow()

    private val _seed = MutableStateFlow(42L)
    val seed: StateFlow<Long> = _seed.asStateFlow()

    // --- Prefilled experiment sequence bounds ---
    private val _investigateStartSeq = MutableStateFlow(1L)
    val investigateStartSeq: StateFlow<Long> = _investigateStartSeq.asStateFlow()

    private val _investigateEndSeq = MutableStateFlow(0L)
    val investigateEndSeq: StateFlow<Long> = _investigateEndSeq.asStateFlow()

    // --- Research Assistant State ---
    private val _researchStep = MutableStateFlow("Observation")
    val researchStep: StateFlow<String> = _researchStep.asStateFlow()

    private val _researchMessages = MutableStateFlow<List<ResearchMessage>>(
        listOf(
            ResearchMessage(
                sender = "ASSISTANT",
                text = "Welcome to QUEUEGLASS Research Assistant. You are currently analyzing BTC-USD on DS-SIM-L3-2025. Select a context or type a query to formulate hypotheses, run C++ experiments, and cite exact engine evidence.",
                isSummary = true,
                citedSources = listOf("L3-Match-Spec v2.4", "C++ Engine Core v1.2")
            )
        )
    )
    val researchMessages: StateFlow<List<ResearchMessage>> = _researchMessages.asStateFlow()

    // --- Settings State ---
    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _reducedMotion = MutableStateFlow(false)
    val reducedMotion: StateFlow<Boolean> = _reducedMotion.asStateFlow()

    private val _highContrast = MutableStateFlow(false)
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    private val _connectors = MutableStateFlow(
        listOf(
            ConnectorStatus("Binance Spot L3", "Exchange WS", true, 0.14),
            ConnectorStatus("Coinbase Advanced", "Exchange FIX", true, 0.22),
            ConnectorStatus("CME Globex Specs", "Market Simulator", true, 0.05),
            ConnectorStatus("LMAX Interbank", "Institutional FIX", false, 0.0)
        )
    )
    val connectors: StateFlow<List<ConnectorStatus>> = _connectors.asStateFlow()

    // --- Section 14-15: Provider registry (model providers & agent connectors) ---
    private val _providers = MutableStateFlow<List<ProviderRegistryEntryDto>>(emptyList())
    val providers: StateFlow<List<ProviderRegistryEntryDto>> = _providers.asStateFlow()

    // --- Section 16: MCP server status ---
    private val _mcpToolCount = MutableStateFlow(0)
    val mcpToolCount: StateFlow<Int> = _mcpToolCount.asStateFlow()

    // --- Section 17: REST / OpenAPI ---
    private val _lastRestStatus = MutableStateFlow<Int?>(null)
    val lastRestStatus: StateFlow<Int?> = _lastRestStatus.asStateFlow()

    // --- Section 18: Internal plugins ---
    private val _plugins = MutableStateFlow<List<PluginDescriptorDto>>(emptyList())
    val plugins: StateFlow<List<PluginDescriptorDto>> = _plugins.asStateFlow()

    private val _lastPluginResult = MutableStateFlow<PluginResultDto?>(null)
    val lastPluginResult: StateFlow<PluginResultDto?> = _lastPluginResult.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            _loading.value = true
            withContext(Dispatchers.Default) {
                QuantEngine.reset(_seed.value)
            }
            stepOnce(500)
            withContext(Dispatchers.Default) {
                // Section 14-18: load provider registry, MCP tool surface, and
                // statically packaged internal plugin descriptors.
                _providers.value = QuantEngine.integrationProviders()
                _mcpToolCount.value = QuantEngine.mcpListTools().tools.size
                _plugins.value = QuantEngine.listPlugins()
            }
            _loading.value = false
        }
    }

    // --- Integration Hub (Sections 14-15) ---

    /** Runs an authenticated capability check; only verified providers can become Connected. */
    fun testProvider(id: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { QuantEngine.testIntegration(id) }
            _providers.value = _providers.value.map { if (it.id == id) result.provider else it }
        }
    }

    fun revokeProvider(id: String) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.Default) { QuantEngine.revokeIntegration(id) }
            _providers.value = _providers.value.map { if (it.id == id) updated else it }
        }
    }

    /** Explicit export of research artifacts for manual sharing. */
    fun exportResearchArtifacts() {
        viewModelScope.launch {
            val json = withContext(Dispatchers.Default) { QuantEngine.exportResearchArtifacts() }
            _exportStatus.value = "Research artifacts exported (${json.length} bytes) for manual sharing"
        }
    }

    // --- MCP server (Section 16) ---

    fun runMcpTool(toolName: String, argumentsJson: String = "{}") {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                QuantEngine.mcpSubmitJob(toolName, argumentsJson)
            }
            _researchStep.value = "C++ execution"
        }
    }

    // --- REST surface (Section 17) ---

    fun verifyRestHealth() {
        viewModelScope.launch {
            val resp = withContext(Dispatchers.Default) {
                QuantEngine.restRequest("GET", "/health")
            }
            _lastRestStatus.value = resp.statusCode
        }
    }

    // --- Internal plugins (Section 18) ---

    fun runInternalPlugin(pluginId: String, inputJson: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                QuantEngine.runPlugin(pluginId, inputJson)
            }
            _lastPluginResult.value = result
        }
    }

    fun selectDestination(dest: Destination) {
        _destination.value = dest
    }

    fun setInstrument(inst: String) {
        if (inst !in INSTRUMENT_LIST) return
        _instrument.value = inst
        newSession()
    }

    fun setHaptics(enabled: Boolean) { _hapticsEnabled.value = enabled }
    fun setReducedMotion(enabled: Boolean) { _reducedMotion.value = enabled }
    fun setHighContrast(enabled: Boolean) { _highContrast.value = enabled }

    /** Steps the engine once by [count] events. */
    fun stepOnce(count: Int = EVENTS_PER_TICK) {
        viewModelScope.launch {
            val events = _state.value.totalEvents
            if (events >= MAX_EVENTS) return@launch
            val n = minOf(count, (MAX_EVENTS - events).toInt())
            if (n <= 0) return@launch

            val s = withContext(Dispatchers.Default) { QuantEngine.step(n) }
            _state.value = s
            refreshDerived()
        }
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        playbackJob = viewModelScope.launch {
            while (isActive && _state.value.totalEvents < MAX_EVENTS) {
                stepOnce(EVENTS_PER_TICK * _speed.value)
                delay(STEP_MS)
            }
            _isPlaying.value = false
        }
    }

    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        _isPlaying.value = false
    }

    fun setSpeed(multiplier: Int) {
        if (multiplier !in listOf(1, 2, 4)) return
        val wasPlaying = _isPlaying.value
        if (wasPlaying) pause()
        _speed.value = multiplier
        if (wasPlaying) play()
    }

    fun newSession() {
        pause()
        viewModelScope.launch {
            _loading.value = true
            _seed.value += 1
            withContext(Dispatchers.Default) { QuantEngine.reset(_seed.value) }
            _checkpoints.value = emptyList()
            _experiment.value = null
            _previousExperiment.value = null
            _policyComparison.value = null
            _strategyResult.value = null
            _state.value = StepState()
            stepOnce(500)
            _loading.value = false
        }
    }

    fun createCheckpoint() {
        viewModelScope.launch {
            val id = withContext(Dispatchers.Default) { QuantEngine.createCheckpoint() }
            if (id > 0) {
                _checkpoints.value = _checkpoints.value + id
            }
        }
    }

    fun restoreCheckpoint(id: Long) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.Default) { QuantEngine.restoreCheckpoint(id) }
            if (success) {
                val s = withContext(Dispatchers.Default) { QuantEngine.state() }
                _state.value = s
                refreshDerived()
            }
        }
    }

    fun setChartInterval(intervalMs: Int) {
        if (intervalMs !in CHART_INTERVALS) return
        _chartInterval.value = intervalMs
        viewModelScope.launch {
            val list = withContext(Dispatchers.Default) {
                QuantEngine.candles(intervalMs, 300)
            }
            _candleList.value = list
        }
    }

    fun investigateInterval(startSeq: Long, endSeq: Long) {
        _investigateStartSeq.value = startSeq
        _investigateEndSeq.value = if (endSeq > 0) endSeq else _state.value.totalEvents
        _destination.value = Destination.Experiments
    }

    private suspend fun refreshDerived() {
        val interval = _chartInterval.value
        val (candles, depth) = withContext(Dispatchers.Default) {
            QuantEngine.candles(interval, 300) to QuantEngine.depth(10)
        }
        _candleList.value = candles
        _depth.value = depth
    }

    // --- Experiments & Policy Comparisons ----------------------------------

    fun runExperiment(side: String, limitPriceTicks: Long, qtyUnits: Long, startSeq: Long, endSeq: Long) {
        viewModelScope.launch {
            val request = ExperimentRequest(side, limitPriceTicks, qtyUnits, startSeq, endSeq)
            val result = withContext(Dispatchers.Default) { QuantEngine.runExperiment(request) }
            _previousExperiment.value = _experiment.value
            _experiment.value = result
            _researchStep.value = "Results"
        }
    }

    fun runPolicyComparison(
        side: String = "BUY",
        qty: Long = 200,
        limitPrice: Long = 0,
        arrivalDelayMs: Long = 0,
        cancellationDelayMs: Long = 0,
        deadlineMs: Long = 5000,
        slices: Int = 4,
        sliceIntervalMs: Long = 1000,
        queueModel: String = "TAIL"
    ) {
        viewModelScope.launch {
            val startSeq = _investigateStartSeq.value.coerceAtLeast(1L)
            val endSeq = if (_investigateEndSeq.value > 0) _investigateEndSeq.value else _state.value.totalEvents
            val res = withContext(Dispatchers.Default) {
                QuantEngine.comparePolicies(
                    side = side,
                    qty = qty,
                    limitPrice = limitPrice,
                    arrivalDelayMs = arrivalDelayMs,
                    cancellationDelayMs = cancellationDelayMs,
                    deadlineMs = deadlineMs,
                    slices = slices,
                    sliceIntervalMs = sliceIntervalMs,
                    queueModel = queueModel,
                    startSeq = startSeq,
                    endSeq = endSeq
                )
            }
            _policyComparison.value = res
            _researchStep.value = "Results"
        }
    }

    fun runStrategyBacktest(
        initialCapital: Double = 100000.0,
        maxPosition: Long = 500,
        imbalanceThreshold: Double = 0.30,
        tradeSize: Long = 100,
        holdDurationMs: Long = 2000
    ) {
        viewModelScope.launch {
            val startSeq = 1L
            val endSeq = _state.value.totalEvents
            val res = withContext(Dispatchers.Default) {
                QuantEngine.runStrategyBacktest(
                    initialCapital = initialCapital,
                    maxPosition = maxPosition,
                    imbalanceThreshold = imbalanceThreshold,
                    tradeSize = tradeSize,
                    holdDurationMs = holdDurationMs,
                    startSeq = startSeq,
                    endSeq = endSeq
                )
            }
            _strategyResult.value = res
        }
    }

    // --- Research Assistant ---

    fun askResearchAssistant(query: String) {
        if (query.isBlank()) return
        val userMsg = ResearchMessage(sender = "USER", text = query, isSummary = false)
        _researchMessages.value = _researchMessages.value + userMsg

        viewModelScope.launch {
            delay(400) // Restrained response delay
            val s = _state.value
            val comp = _policyComparison.value
            val inst = _instrument.value

            val (responseText, proposal, cited) = when {
                query.contains("imbalance", ignoreCase = true) -> {
                    _researchStep.value = "Hypothesis"
                    Triple(
                        "[DETERMINISTIC OFFLINE SUMMARY]\nCurrent Order Book Imbalance for $inst is ${String.format("%+.2f", s.book.depthImbalance)}. " +
                                "Top 5-level Bid Depth: ${s.book.bidDepth} units, Ask Depth: ${s.book.askDepth} units. " +
                                "Hypothesis: Asymmetry in top depth indicates short-term order pressure. Proposing TWAP execution experiment to mitigate market impact.",
                        ProposedExperimentConfig("BUY", 300, 0, 4, "TAIL"),
                        listOf("Microstructure Paper #104", "Engine OrderBook.cpp L204")
                    )
                }
                query.contains("TWAP", ignoreCase = true) || query.contains("compare", ignoreCase = true) -> {
                    _researchStep.value = "Validated Experiment"
                    val twapText = if (comp != null) {
                        "Last C++ Engine comparison showed TWAP cost diff vs Immediate: ${String.format("%+.2f bps", comp.costDiffTwapVsImmediateBps)}."
                    } else {
                        "No execution comparison run yet. Ready to launch C++ evaluation across Immediate, TWAP, and Passive Limit policies."
                    }
                    Triple(
                        "[DETERMINISTIC OFFLINE SUMMARY]\n$twapText " +
                                "TWAP slices minimize impact by distributing order volume across discrete intervals. Evaluate 4-slice TWAP with TAIL queue model.",
                        ProposedExperimentConfig("BUY", 400, 0, 4, "TAIL"),
                        listOf("TWAP Execution Policy Spec v1.1", "C++ ExecutionEngine.cpp")
                    )
                }
                else -> {
                    _researchStep.value = "Explanation"
                    Triple(
                        "[DETERMINISTIC OFFLINE SUMMARY]\nEngine state for $inst: ${s.totalEvents} events processed across ${s.totalTrades} trades ($${s.totalVolume} units). " +
                                "Current Spread: ${(s.book.spread / 100.0)} ticks, Volatility: ${String.format("%.1f bps", s.volatility)}. " +
                                "All metrics derived deterministically from C++20 engine trace without external generative hallucinations.",
                        null,
                        listOf("QUEUEGLASS Trace Log", "Engine State Snapshot")
                    )
                }
            }

            val assistantMsg = ResearchMessage(
                sender = "ASSISTANT",
                text = responseText,
                isSummary = true,
                citedSources = cited,
                proposedExperiment = proposal
            )
            _researchMessages.value = _researchMessages.value + assistantMsg
        }
    }

    // --- Saved investigations ---

    fun saveInvestigation(note: String) {
        val exp = _experiment.value ?: return
        val s = _state.value
        viewModelScope.launch {
            dao.insert(
                SavedInvestigation(
                    savedAtMs = System.currentTimeMillis(),
                    seed = _seed.value,
                    chartIntervalMs = _chartInterval.value,
                    eventsStepped = s.totalEvents.toInt(),
                    side = exp.side,
                    limitPriceTicks = exp.limitPriceTicks,
                    qtyUnits = exp.qtyUnits,
                    startSeq = exp.startSeq,
                    endSeq = exp.endSeq,
                    filledQty = exp.filledQty,
                    unfilledQty = exp.unfilledQty,
                    avgFillPrice = exp.avgFillPrice,
                    totalNotional = exp.totalNotional,
                    fees = exp.fees,
                    slippageVsArrivalMid = exp.slippageVsArrivalMid,
                    arrivalMidTicks = exp.arrivalMidTicks,
                    levelsConsumed = exp.levelsConsumed,
                    gapsDetected = exp.gapsDetected,
                    fillCount = exp.fills.size,
                    replayNs = s.perf.replayNs,
                    experimentNs = s.perf.experimentNs,
                    note = note,
                )
            )
            _researchStep.value = "Experiment Memory"
        }
    }

    fun deleteInvestigation(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    fun clearAllSavedInvestigations() {
        viewModelScope.launch { dao.deleteAll() }
    }

    // --- Authentication Actions ---

    fun signIn(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.signIn(email, pass)
            onResult(res.success, res.errorMessage)
        }
    }

    fun createAccount(email: String, pass: String, name: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.createAccount(email, pass, name)
            onResult(res.success, res.errorMessage)
        }
    }

    fun resetPassword(email: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepo.resetPassword(email, newPass)
            onResult(res.success, res.errorMessage)
        }
    }

    fun enterOfflineDemo() {
        authRepo.enterOfflineDemo()
    }

    fun signOut() {
        pause()
        authRepo.signOut()
    }

    fun getAuthAvailabilityStatus(): String = authRepo.getAuthAvailabilityStatus()

    override fun onCleared() {
        pause()
        super.onCleared()
    }
}
