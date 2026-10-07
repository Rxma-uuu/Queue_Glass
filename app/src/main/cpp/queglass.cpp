// QUEUEGLASS JNI bridge.
//
// Exposes the C++20 queueglass core engine to Kotlin. All heavy computation
// (event generation, book maintenance, candle aggregation, experiments) runs
// natively; Kotlin owns presentation only. Calls return JSON payloads, parsed
// on the Kotlin side with kotlinx.serialization, to minimize JNI round trips.

#include <jni.h>
#include <string>
#include <memory>
#include <mutex>

#include "queueglass/Engine.hpp"
#include "queueglass/Experiment.hpp"
#include "queueglass/IntegrationHub.hpp"
#include "queueglass/McpServer.hpp"
#include "queueglass/RestApi.hpp"
#include "queueglass/InternalPlugin.hpp"

using queueglass::Engine;
using queueglass::ExperimentConfig;

namespace {

std::mutex g_mutex;  // one engine per session; guard against concurrent JNI calls
Engine* g_engine = nullptr;

// Integration surfaces (Section 14-18). Lazily constructed, engine-scoped.
queueglass::IntegrationHub* g_hub = nullptr;
queueglass::McpServer* g_mcp = nullptr;
queueglass::RestApiService* g_rest = nullptr;
bool g_plugins_registered = false;

queueglass::IntegrationHub& hub() {
    if (g_hub == nullptr) g_hub = new queueglass::IntegrationHub();
    return *g_hub;
}

queueglass::McpServer& mcp() {
    if (g_mcp == nullptr) g_mcp = new queueglass::McpServer();
    return *g_mcp;
}

queueglass::RestApiService& rest() {
    if (g_rest == nullptr) g_rest = new queueglass::RestApiService();
    return *g_rest;
}

void ensurePluginsRegistered() {
    if (!g_plugins_registered) {
        queueglass::registerBuiltinPlugins();
        g_plugins_registered = true;
    }
}

Engine& engine() {
    if (g_engine == nullptr) {
        g_engine = new Engine(42);
    }
    return *g_engine;
}

std::string toStdString(JNIEnv* env, jstring js) {
    if (js == nullptr) return {};
    const char* chars = env->GetStringUTFChars(js, nullptr);
    std::string s(chars);
    env->ReleaseStringUTFChars(js, chars);
    return s;
}

jstring toJString(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

} // namespace

extern "C" {

// Resets the engine with a fresh deterministic seed.
JNIEXPORT void JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeReset(JNIEnv*, jobject, jlong seed) {
    std::lock_guard<std::mutex> lock(g_mutex);
    delete g_engine;
    g_engine = new Engine(static_cast<uint64_t>(seed));
}

// Processes [count] synthetic events through the real engine. Returns a single
// JSON payload with book depth, candles, trace, perf stats and totals.
JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeStep(JNIEnv* env, jobject, jint count) {
    std::lock_guard<std::mutex> lock(g_mutex);
    Engine& e = engine();
    e.stepEvents(static_cast<size_t>(count));

    std::string json = "{";
    json += "\"book\":" + e.getBookSummaryJson(10) + ",";
    json += "\"candles\":" + e.getCandlesJsonForInterval(500, 300) + ",";
    json += "\"trace\":" + e.getEventTraceJson(200) + ",";
    json += "\"perf\":" + e.getPerfStats().toJson() + ",";
    json += "\"total_events\":" + std::to_string(e.getTotalEventsProcessed()) + ",";
    json += "\"total_trades\":" + std::to_string(e.getTotalTrades()) + ",";
    json += "\"total_volume\":" + std::to_string(e.getTotalVolume()) + ",";
    json += "\"gaps\":" + std::to_string(e.getGapsDetected()) + ",";
    json += "\"rejected\":" + std::to_string(e.getRejectedCount()) + ",";
    json += "\"volatility\":" + std::to_string(e.currentVolatility());
    json += '}';
    return toJString(env, json);
}

// Returns the same payload without stepping (current state only).
JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeGetState(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    Engine& e = engine();

    std::string json = "{";
    json += "\"book\":" + e.getBookSummaryJson(10) + ",";
    json += "\"candles\":" + e.getCandlesJsonForInterval(500, 300) + ",";
    json += "\"trace\":" + e.getEventTraceJson(200) + ",";
    json += "\"perf\":" + e.getPerfStats().toJson() + ",";
    json += "\"total_events\":" + std::to_string(e.getTotalEventsProcessed()) + ",";
    json += "\"total_trades\":" + std::to_string(e.getTotalTrades()) + ",";
    json += "\"total_volume\":" + std::to_string(e.getTotalVolume()) + ",";
    json += "\"gaps\":" + std::to_string(e.getGapsDetected()) + ",";
    json += "\"rejected\":" + std::to_string(e.getRejectedCount()) + ",";
    json += "\"volatility\":" + std::to_string(e.currentVolatility());
    json += '}';
    return toJString(env, json);
}

// Candles at an arbitrary chart interval (ms), re-aggregated deterministically
// from the recorded trade log.
JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeGetCandles(JNIEnv* env, jobject,
                                                                    jint interval_ms, jint max_count) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return toJString(env, engine().getCandlesJsonForInterval(interval_ms, static_cast<size_t>(max_count)));
}

// Order-book depth levels as JSON: {"bids":[...],"asks":[...]}.
JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeGetDepth(JNIEnv* env, jobject, jint depth) {
    std::lock_guard<std::mutex> lock(g_mutex);
    Engine& e = engine();
    auto bids = e.getOrderBook().getBidLevels(static_cast<size_t>(depth));
    auto asks = e.getOrderBook().getAskLevels(static_cast<size_t>(depth));

    std::string json = "{\"bids\":[";
    for (size_t i = 0; i < bids.size(); ++i) {
        if (i > 0) json += ',';
        json += "{\"price\":" + std::to_string(bids[i].price_ticks) +
                ",\"qty\":" + std::to_string(bids[i].total_qty) +
                ",\"orders\":" + std::to_string(bids[i].order_count) + '}';
    }
    json += "],\"asks\":[";
    for (size_t i = 0; i < asks.size(); ++i) {
        if (i > 0) json += ',';
        json += "{\"price\":" + std::to_string(asks[i].price_ticks) +
                ",\"qty\":" + std::to_string(asks[i].total_qty) +
                ",\"orders\":" + std::to_string(asks[i].order_count) + '}';
    }
    json += "]}";
    return toJString(env, json);
}

// Runs an execution experiment against the replayed book. Deterministic:
// replays the recorded event window through a fresh book; the live book is
// never mutated. Returns the full ExperimentResult as JSON.
JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeRunExperiment(JNIEnv* env, jobject,
                                                                       jstring side,
                                                                       jlong limit_price_ticks,
                                                                       jlong qty_units,
                                                                       jlong start_seq,
                                                                       jlong end_seq) {
    std::lock_guard<std::mutex> lock(g_mutex);
    ExperimentConfig cfg;
    cfg.side = toStdString(env, side);
    cfg.limit_price_ticks = limit_price_ticks;
    cfg.qty_units = static_cast<uint64_t>(qty_units);
    cfg.start_seq = static_cast<uint64_t>(start_seq);
    cfg.end_seq = static_cast<uint64_t>(end_seq);
    const auto result = engine().runExperiment(cfg);
    return toJString(env, result.toJson());
}

JNIEXPORT jlong JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeCreateCheckpoint(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return static_cast<jlong>(engine().createCheckpoint());
}

JNIEXPORT jboolean JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeRestoreCheckpoint(JNIEnv*, jobject, jlong id) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return engine().restoreCheckpoint(static_cast<uint64_t>(id)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeComparePolicies(
    JNIEnv* env, jobject,
    jstring side,
    jlong qty,
    jlong limit_price,
    jlong arrival_delay_ms,
    jlong cancellation_delay_ms,
    jlong deadline_ms,
    jint slices,
    jlong slice_interval_ms,
    jstring queue_model,
    jlong start_seq,
    jlong end_seq) {
    std::lock_guard<std::mutex> lock(g_mutex);
    queueglass::ExecutionPolicyConfig cfg;
    cfg.side = toStdString(env, side);
    cfg.total_qty_units = static_cast<uint64_t>(qty);
    cfg.limit_price_ticks = limit_price;
    cfg.order_arrival_delay_ms = static_cast<uint64_t>(arrival_delay_ms);
    cfg.cancellation_delay_ms = static_cast<uint64_t>(cancellation_delay_ms);
    cfg.completion_deadline_ms = static_cast<uint64_t>(deadline_ms);
    cfg.twap_num_slices = static_cast<uint32_t>(slices);
    cfg.twap_slice_interval_ms = static_cast<uint64_t>(slice_interval_ms);
    cfg.queue_model = toStdString(env, queue_model);
    cfg.start_seq = static_cast<uint64_t>(start_seq);
    cfg.end_seq = static_cast<uint64_t>(end_seq);

    const auto res = engine().compareExecutionPolicies(cfg);
    return toJString(env, res.toJson());
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeRunStrategyBacktest(
    JNIEnv* env, jobject,
    jdouble initial_capital,
    jlong max_position,
    jdouble imbalance_threshold,
    jlong trade_size,
    jlong hold_duration_ms,
    jlong start_seq,
    jlong end_seq) {
    std::lock_guard<std::mutex> lock(g_mutex);
    queueglass::MicrostructureStrategyConfig cfg;
    cfg.initial_capital = initial_capital;
    cfg.max_position_units = static_cast<uint64_t>(max_position);
    cfg.imbalance_entry_threshold = imbalance_threshold;
    cfg.trade_size_units = static_cast<uint64_t>(trade_size);
    cfg.hold_duration_ms = hold_duration_ms;
    cfg.start_seq = static_cast<uint64_t>(start_seq);
    cfg.end_seq = static_cast<uint64_t>(end_seq);

    const auto res = engine().runStrategyBacktest(cfg);
    return toJString(env, res.toJson());
}

// ---------------------------------------------------------------------------
// Section 14-18: Integration Hub / Provider Registry
// ---------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeGetIntegrationProviders(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return toJString(env, hub().toJson());
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeTestIntegration(JNIEnv* env, jobject, jstring provider_id) {
    std::lock_guard<std::mutex> lock(g_mutex);
    std::string id = toStdString(env, provider_id);
    bool ok = hub().testProviderConnection(id);
    const auto entry = hub().getProvider(id);
    std::string json = "{\"success\":";
    json += ok ? "true" : "false";
    json += ",\"provider\":" + entry.toJson() + "}";
    return toJString(env, json);
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeRevokeIntegration(JNIEnv* env, jobject, jstring provider_id) {
    std::lock_guard<std::mutex> lock(g_mutex);
    std::string id = toStdString(env, provider_id);
    hub().revokeProvider(id);
    return toJString(env, hub().getProvider(id).toJson());
}

// Explicit export of research artifacts for manual sharing (used when a
// provider is unverified and generic access must remain available).
JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeExportResearchArtifacts(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return toJString(env, hub().exportResearchArtifactsJson());
}

// ---------------------------------------------------------------------------
// Section 16: MCP server (protocol-compliant tool surface)
// ---------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeMcpListTools(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return toJString(env, mcp().listToolsJson());
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeMcpCallTool(JNIEnv* env, jobject,
                                                                     jstring tool_name,
                                                                     jstring arguments_json) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return toJString(env, mcp().callTool(toStdString(env, tool_name),
                                         toStdString(env, arguments_json), engine()));
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeMcpSubmitJob(JNIEnv* env, jobject,
                                                                      jstring tool_name,
                                                                      jstring arguments_json) {
    std::lock_guard<std::mutex> lock(g_mutex);
    std::string job_id = mcp().submitJob(toStdString(env, tool_name),
                                         toStdString(env, arguments_json), engine());
    return toJString(env, mcp().getJobStatusJson(job_id));
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeMcpGetJob(JNIEnv* env, jobject, jstring job_id) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return toJString(env, mcp().getJobStatusJson(toStdString(env, job_id)));
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeMcpCancelJob(JNIEnv* env, jobject, jstring job_id) {
    std::lock_guard<std::mutex> lock(g_mutex);
    bool ok = mcp().cancelJob(toStdString(env, job_id));
    std::string json = std::string("{\"cancelled\":") + (ok ? "true" : "false") + "}";
    return toJString(env, json);
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeMcpAuditLogs(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return toJString(env, mcp().getAuditLogsJson());
}

// ---------------------------------------------------------------------------
// Section 17: REST surface (same application services as MCP)
// ---------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeRestRequest(JNIEnv* env, jobject,
                                                                     jstring method,
                                                                     jstring path,
                                                                     jstring body_json) {
    std::lock_guard<std::mutex> lock(g_mutex);
    queueglass::RestRequest req;
    req.method = toStdString(env, method);
    req.path = toStdString(env, path);
    req.body_json = toStdString(env, body_json);
    const auto resp = rest().handleHttpRequest(req, engine(), hub(), mcp());
    return toJString(env, resp.toJson());
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeRestOpenApiSpec(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return toJString(env, rest().getOpenApiSpecJson());
}

// ---------------------------------------------------------------------------
// Section 18: Internal plugins (statically packaged backend modules)
// ---------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeListPlugins(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    ensurePluginsRegistered();
    return toJString(env, queueglass::PluginRegistry::instance().listDescriptorsJson());
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_engine_QuantEngine_nativeRunPlugin(JNIEnv* env, jobject,
                                                                   jstring plugin_id,
                                                                   jstring input_json) {
    std::lock_guard<std::mutex> lock(g_mutex);
    ensurePluginsRegistered();
    const auto result = queueglass::PluginRegistry::instance().runPlugin(
        toStdString(env, plugin_id), engine(), toStdString(env, input_json));
    return toJString(env, result.toJson());
}

} // extern "C"
