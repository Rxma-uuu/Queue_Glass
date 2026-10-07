package com.example.myapplication

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.AuthState
import com.example.myapplication.data.SavedInvestigation
import com.example.myapplication.engine.BookDepth
import com.example.myapplication.engine.Candle
import com.example.myapplication.engine.ExperimentResult
import com.example.myapplication.engine.PluginDescriptorDto
import com.example.myapplication.engine.PolicyComparisonResultDto
import com.example.myapplication.engine.ProviderRegistryEntryDto
import com.example.myapplication.engine.StepState
import com.example.myapplication.engine.StrategyPerformanceResultDto
import com.example.myapplication.ui.navigation.Destination
import com.example.myapplication.ui.screens.ExperimentsScreen
import com.example.myapplication.ui.screens.LoginScreen
import com.example.myapplication.ui.screens.MarketScreen
import com.example.myapplication.ui.screens.ReplayScreen
import com.example.myapplication.ui.screens.ResearchScreen
import com.example.myapplication.ui.screens.SettingsScreen
import com.example.myapplication.ui.theme.CyanAccent
import com.example.myapplication.ui.theme.MainBackground
import com.example.myapplication.ui.theme.PanelBackground
import com.example.myapplication.ui.theme.PrimaryText
import com.example.myapplication.ui.theme.SecondaryText

@Composable
fun TerminalScreen(viewModel: TerminalViewModel = viewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    if (authState is AuthState.Unauthenticated) {
        LoginScreen(
            authAvailabilityStatus = viewModel.getAuthAvailabilityStatus(),
            onSignIn = viewModel::signIn,
            onCreateAccount = viewModel::createAccount,
            onResetPassword = viewModel::resetPassword,
            onExploreOfflineDemo = viewModel::enterOfflineDemo
        )
        return
    }

    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val instrument by viewModel.instrument.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val candles by viewModel.candles.collectAsStateWithLifecycle()
    val depth by viewModel.depth.collectAsStateWithLifecycle()
    val chartInterval by viewModel.chartInterval.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val speed by viewModel.speed.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val checkpoints by viewModel.checkpoints.collectAsStateWithLifecycle()
    val experiment by viewModel.experiment.collectAsStateWithLifecycle()
    val previousExperiment by viewModel.previousExperiment.collectAsStateWithLifecycle()
    val policyComparison by viewModel.policyComparison.collectAsStateWithLifecycle()
    val strategyResult by viewModel.strategyResult.collectAsStateWithLifecycle()
    val seed by viewModel.seed.collectAsStateWithLifecycle()
    val saved by viewModel.savedInvestigations.collectAsStateWithLifecycle()
    val startSeq by viewModel.investigateStartSeq.collectAsStateWithLifecycle()
    val endSeq by viewModel.investigateEndSeq.collectAsStateWithLifecycle()
    val researchStep by viewModel.researchStep.collectAsStateWithLifecycle()
    val researchMessages by viewModel.researchMessages.collectAsStateWithLifecycle()
    val haptics by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val reducedMotion by viewModel.reducedMotion.collectAsStateWithLifecycle()
    val highContrast by viewModel.highContrast.collectAsStateWithLifecycle()
    val connectors by viewModel.connectors.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val mcpToolCount by viewModel.mcpToolCount.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isWideScreen) {
        // Adaptive Tablet / Landscape NavigationRail Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MainBackground)
        ) {
            NavigationRail(
                containerColor = PanelBackground,
                contentColor = PrimaryText,
                header = {
                    Text(
                        text = "QG",
                        color = CyanAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            ) {
                Destination.entries.forEach { dest ->
                    val selected = dest == destination
                    NavigationRailItem(
                        selected = selected,
                        onClick = { viewModel.selectDestination(dest) },
                        icon = { Icon(imageVector = dest.icon, contentDescription = dest.title) },
                        label = { Text(dest.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MainBackground,
                            selectedTextColor = CyanAccent,
                            indicatorColor = CyanAccent,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        )
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                ScreenHostContent(
                    destination = destination,
                    state = state,
                    candles = candles,
                    depth = depth,
                    chartInterval = chartInterval,
                    instrument = instrument,
                    isPlaying = isPlaying,
                    speed = speed,
                    loading = loading,
                    checkpoints = checkpoints,
                    experiment = experiment,
                    previousExperiment = previousExperiment,
                    policyComparison = policyComparison,
                    strategyResult = strategyResult,
                    saved = saved,
                    seed = seed,
                    startSeq = startSeq,
                    endSeq = endSeq,
                    researchStep = researchStep,
                    researchMessages = researchMessages,
                    haptics = haptics,
                    reducedMotion = reducedMotion,
                    highContrast = highContrast,
                    connectors = connectors,
                    providers = providers,
                    plugins = plugins,
                    mcpToolCount = mcpToolCount,
                    authState = authState,
                    viewModel = viewModel
                )
            }
        }
    } else {
        // Standard Portrait Phone Bottom Navigation Layout
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = PanelBackground,
                    contentColor = PrimaryText,
                    tonalElevation = 8.dp
                ) {
                    Destination.entries.forEach { dest ->
                        val selected = dest == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.selectDestination(dest) },
                            icon = { Icon(imageVector = dest.icon, contentDescription = dest.title) },
                            label = { Text(dest.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MainBackground,
                                selectedTextColor = CyanAccent,
                                indicatorColor = CyanAccent,
                                unselectedIconColor = SecondaryText,
                                unselectedTextColor = SecondaryText
                            )
                        )
                    }
                }
            },
            containerColor = MainBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ScreenHostContent(
                    destination = destination,
                    state = state,
                    candles = candles,
                    depth = depth,
                    chartInterval = chartInterval,
                    instrument = instrument,
                    isPlaying = isPlaying,
                    speed = speed,
                    loading = loading,
                    checkpoints = checkpoints,
                    experiment = experiment,
                    previousExperiment = previousExperiment,
                    policyComparison = policyComparison,
                    strategyResult = strategyResult,
                    saved = saved,
                    seed = seed,
                    startSeq = startSeq,
                    endSeq = endSeq,
                    researchStep = researchStep,
                    researchMessages = researchMessages,
                    haptics = haptics,
                    reducedMotion = reducedMotion,
                    highContrast = highContrast,
                    connectors = connectors,
                    providers = providers,
                    plugins = plugins,
                    mcpToolCount = mcpToolCount,
                    authState = authState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun ScreenHostContent(
    destination: Destination,
    state: StepState,
    candles: List<Candle>,
    depth: BookDepth,
    chartInterval: Int,
    instrument: String,
    isPlaying: Boolean,
    speed: Int,
    loading: Boolean,
    checkpoints: List<Long>,
    experiment: ExperimentResult?,
    previousExperiment: ExperimentResult?,
    policyComparison: PolicyComparisonResultDto?,
    strategyResult: StrategyPerformanceResultDto?,
    saved: List<SavedInvestigation>,
    seed: Long,
    startSeq: Long,
    endSeq: Long,
    researchStep: String,
    researchMessages: List<ResearchMessage>,
    haptics: Boolean,
    reducedMotion: Boolean,
    highContrast: Boolean,
    connectors: List<ConnectorStatus>,
    providers: List<ProviderRegistryEntryDto>,
    plugins: List<PluginDescriptorDto>,
    mcpToolCount: Int,
    authState: AuthState,
    viewModel: TerminalViewModel
) {
    when (destination) {
        Destination.Market -> MarketScreen(
            state = state,
            candles = candles,
            depth = depth,
            chartInterval = chartInterval,
            instrument = instrument,
            seed = seed,
            authState = authState,
            savedInvestigations = saved,
            onInstrumentSelect = viewModel::setInstrument,
            onIntervalSelect = viewModel::setChartInterval,
            onInvestigateInterval = viewModel::investigateInterval,
            onNavigateToDestination = viewModel::selectDestination,
            onSignOut = viewModel::signOut,
            onNavigateToAuth = viewModel::signOut
        )

        Destination.Replay -> ReplayScreen(
            state = state,
            depth = depth,
            isPlaying = isPlaying,
            speed = speed,
            loading = loading,
            checkpoints = checkpoints,
            onPlay = viewModel::play,
            onPause = viewModel::pause,
            onSpeedSelect = viewModel::setSpeed,
            onStep = { viewModel.stepOnce(it) },
            onNewSession = viewModel::newSession,
            onCreateCheckpoint = viewModel::createCheckpoint,
            onRestoreCheckpoint = viewModel::restoreCheckpoint
        )

        Destination.Experiments -> ExperimentsScreen(
            currentSeq = state.totalEvents,
            investigateStartSeq = startSeq,
            investigateEndSeq = endSeq,
            experiment = experiment,
            previousExperiment = previousExperiment,
            policyComparison = policyComparison,
            strategyResult = strategyResult,
            savedInvestigations = saved,
            onRunExperiment = viewModel::runExperiment,
            onRunPolicyComparison = viewModel::runPolicyComparison,
            onRunStrategyBacktest = viewModel::runStrategyBacktest,
            onSaveInvestigation = viewModel::saveInvestigation,
            onDeleteInvestigation = viewModel::deleteInvestigation
        )

        Destination.Research -> ResearchScreen(
            state = state,
            instrument = instrument,
            chartInterval = chartInterval,
            currentStep = researchStep,
            messages = researchMessages,
            onAskAssistant = viewModel::askResearchAssistant,
            onLaunchProposed = { prop ->
                viewModel.selectDestination(Destination.Experiments)
                viewModel.runPolicyComparison(
                    side = prop.side,
                    qty = prop.qty,
                    limitPrice = prop.limitPrice,
                    slices = prop.slices,
                    queueModel = prop.queueModel
                )
            }
        )

        Destination.Settings -> SettingsScreen(
            seed = seed,
            perf = state.perf,
            hapticsEnabled = haptics,
            reducedMotion = reducedMotion,
            highContrast = highContrast,
            connectors = connectors,
            providers = providers,
            plugins = plugins,
            mcpToolCount = mcpToolCount,
            authState = authState,
            onSignOut = viewModel::signOut,
            onNavigateToAuth = viewModel::signOut,
            onTestProvider = viewModel::testProvider,
            onRevokeProvider = viewModel::revokeProvider,
            onExportArtifacts = viewModel::exportResearchArtifacts,
            onToggleHaptics = viewModel::setHaptics,
            onToggleReducedMotion = viewModel::setReducedMotion,
            onToggleHighContrast = viewModel::setHighContrast,
            onClearAllData = viewModel::clearAllSavedInvestigations,
            onResetEngine = viewModel::newSession
        )
    }
}
