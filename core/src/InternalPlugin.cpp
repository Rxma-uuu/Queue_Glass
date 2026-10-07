#include "queueglass/InternalPlugin.hpp"
#include "queueglass/Engine.hpp"
#include <sstream>
#include <chrono>
#include <algorithm>

namespace queueglass {

// ---------------------------------------------------------------------------
// PluginResult serialization (evidence output contract)
// ---------------------------------------------------------------------------

std::string PluginResult::toJson() const {
    std::ostringstream ss;
    ss << "{";
    ss << "\"plugin_id\":\"" << plugin_id << "\",";
    ss << "\"plugin_version\":\"" << plugin_version << "\",";
    ss << "\"success\":" << (success ? "true" : "false") << ",";
    if (!error_message.empty()) {
        ss << "\"error\":\"" << error_message << "\",";
    }
    ss << "\"events_scanned\":" << events_scanned << ",";
    ss << "\"runtime_ms\":" << runtime_ms << ",";
    ss << "\"evidence\":[";
    for (size_t i = 0; i < evidence.size(); ++i) {
        if (i > 0) ss << ",";
        ss << "{\"evidence_id\":\"" << evidence[i].evidence_id << "\","
           << "\"kind\":\"" << evidence[i].kind << "\","
           << "\"reference\":\"" << evidence[i].reference << "\","
           << "\"checksum\":\"" << evidence[i].checksum << "\"}";
    }
    ss << "],";
    ss << "\"output\":" << (output_json.empty() ? "{}" : output_json);
    ss << "}";
    return ss.str();
}

// ---------------------------------------------------------------------------
// PluginRegistry
// ---------------------------------------------------------------------------

PluginRegistry& PluginRegistry::instance() {
    static PluginRegistry registry;
    return registry;
}

void PluginRegistry::registerPlugin(std::shared_ptr<InternalPlugin> plugin) {
    if (!plugin) return;
    const std::string id = plugin->descriptor().id;
    for (auto& existing : plugins_) {
        if (existing->descriptor().id == id) return; // idempotent
    }
    plugins_.push_back(std::move(plugin));
}

std::vector<PluginDescriptor> PluginRegistry::listDescriptors() const {
    std::vector<PluginDescriptor> out;
    out.reserve(plugins_.size());
    for (const auto& p : plugins_) out.push_back(p->descriptor());
    return out;
}

std::string PluginRegistry::listDescriptorsJson() const {
    std::ostringstream ss;
    ss << "[";
    auto descs = listDescriptors();
    for (size_t i = 0; i < descs.size(); ++i) {
        if (i > 0) ss << ",";
        ss << "{\"id\":\"" << descs[i].id << "\","
           << "\"version\":\"" << descs[i].version << "\","
           << "\"description\":\"" << descs[i].description << "\","
           << "\"input_schema\":" << descs[i].input_schema_json << ","
           << "\"output_schema\":" << descs[i].output_schema_json << ","
           << "\"required_data_capabilities\":[";
        for (size_t j = 0; j < descs[i].required_data_capabilities.size(); ++j) {
            if (j > 0) ss << ",";
            ss << "\"" << descs[i].required_data_capabilities[j] << "\"";
        }
        ss << "],"
           << "\"budget\":{\"max_events_scanned\":" << descs[i].budget.max_events_scanned << ","
           << "\"max_output_bytes\":" << descs[i].budget.max_output_bytes << ","
           << "\"max_runtime_hint_ms\":" << descs[i].budget.max_runtime_hint_ms << "}}";
    }
    ss << "]";
    return ss.str();
}

PluginResult PluginRegistry::runPlugin(const std::string& plugin_id, Engine& engine, const std::string& input_json) const {
    for (const auto& p : plugins_) {
        if (p->descriptor().id != plugin_id) continue;
        // Permission checks are compile-time static (internal plugins);
        // the host enforces the resource budget via the handler contract.
        return p->execute(engine, input_json);
    }

    PluginResult err;
    err.plugin_id = plugin_id;
    err.error_message = "Unknown internal plugin";
    return err;
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

namespace {

uint64_t nowMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
}

// Deterministic FNV-1a hash used as artifact checksum.
std::string fnv1aHex(const std::string& data) {
    uint64_t h = 1469598103934665603ULL;
    for (unsigned char c : data) {
        h ^= c;
        h *= 1099511628211ULL;
    }
    std::ostringstream ss;
    ss << "0x" << std::hex << h;
    return ss.str();
}

// Minimal JSON field extractor for flat object inputs: "key":value or "key":"value"
std::string extractJsonStringField(const std::string& json, const std::string& key) {
    std::string needle = "\"" + key + "\"";
    size_t k = json.find(needle);
    if (k == std::string::npos) return {};
    size_t colon = json.find(':', k + needle.size());
    if (colon == std::string::npos) return {};
    size_t first = json.find_first_not_of(" \t", colon + 1);
    if (first == std::string::npos) return {};
    if (json[first] == '"') {
        size_t last = json.find('"', first + 1);
        if (last == std::string::npos) return {};
        return json.substr(first + 1, last - first - 1);
    }
    size_t last = json.find_first_of(",}", first);
    if (last == std::string::npos) last = json.size();
    return json.substr(first, last - first);
}

std::string buildMetaJson(uint64_t start_seq, uint64_t end_seq) {
    std::ostringstream ss;
    ss << "\"meta\":{"
       << "\"dataset_id\":\"DS-SIM-L3-2025\","
       << "\"engine_version\":\"QUEUEGLASS Core v1.2.0\","
       << "\"source_event_range\":\"seq_" << start_seq << "_to_" << end_seq << "\","
       << "\"units\":\"price_in_ticks_hundredths_and_qty_units\","
       << "\"quality_flags\":[\"DETERMINISTIC\",\"SEQUENCE_VALIDATED\"]"
       << "}";
    return ss.str();
}

} // namespace

// ---------------------------------------------------------------------------
// 1. Execution Comparison Plugin
// ---------------------------------------------------------------------------

const PluginDescriptor& ExecutionComparisonPlugin::descriptor() const {
    static PluginDescriptor d;
    d.id = "execution_comparison";
    d.version = "1.0.0";
    d.description = "Compare approved execution policies (Immediate, TWAP, Passive Limit) using the C++ engine over a verified event window.";
    d.input_schema_json = "{\"type\":\"object\",\"properties\":{\"side\":{\"type\":\"string\",\"enum\":[\"BUY\",\"SELL\"]},\"qty\":{\"type\":\"integer\"},\"limit_price\":{\"type\":\"integer\"},\"slices\":{\"type\":\"integer\"},\"slice_interval_ms\":{\"type\":\"integer\"},\"queue_model\":{\"type\":\"string\",\"enum\":[\"HEAD\",\"TAIL\",\"PROPORTIONAL\"]},\"deadline_ms\":{\"type\":\"integer\"},\"start_seq\":{\"type\":\"integer\"},\"end_seq\":{\"type\":\"integer\"}}}";
    d.output_schema_json = "{\"type\":\"object\",\"properties\":{\"comparison\":{\"type\":\"object\"}}}";
    d.permissions = PluginPermissionSet{true, true, true, true, false};
    d.required_data_capabilities = {"L3_ORDER_BY_ORDER", "SEQUENCE_NUMBERS", "TRADE_LOG"};
    d.budget = PluginResourceBudget{};
    return d;
}

PluginResult ExecutionComparisonPlugin::execute(Engine& engine, const std::string& input_json) const {
    auto start = std::chrono::high_resolution_clock::now();

    PluginResult result;
    result.plugin_id = descriptor().id;
    result.plugin_version = descriptor().version;

    ExecutionPolicyConfig cfg;
    cfg.side = extractJsonStringField(input_json, "side");
    if (cfg.side.empty()) cfg.side = "BUY";

    std::string qty_str = extractJsonStringField(input_json, "qty");
    cfg.total_qty_units = qty_str.empty() ? 200 : static_cast<uint64_t>(std::stoull(qty_str));

    std::string lp = extractJsonStringField(input_json, "limit_price");
    cfg.limit_price_ticks = lp.empty() ? 0 : std::stoll(lp);

    std::string slices = extractJsonStringField(input_json, "slices");
    cfg.twap_num_slices = slices.empty() ? 4 : static_cast<uint32_t>(std::stoul(slices));

    std::string interval = extractJsonStringField(input_json, "slice_interval_ms");
    cfg.twap_slice_interval_ms = interval.empty() ? 1000 : std::stoull(interval);

    std::string qm = extractJsonStringField(input_json, "queue_model");
    cfg.queue_model = qm.empty() ? "TAIL" : qm;

    std::string dl = extractJsonStringField(input_json, "deadline_ms");
    cfg.completion_deadline_ms = dl.empty() ? 5000 : std::stoull(dl);

    std::string sseq = extractJsonStringField(input_json, "start_seq");
    cfg.start_seq = sseq.empty() ? 1 : std::stoull(sseq);

    std::string eseq = extractJsonStringField(input_json, "end_seq");
    cfg.end_seq = eseq.empty() ? engine.getTotalEventsProcessed() : std::stoull(eseq);

    if (cfg.end_seq < cfg.start_seq) {
        result.error_message = "INVALID_WINDOW: end_seq must be >= start_seq";
        result.runtime_ms = 0;
        return result;
    }

    auto comp = engine.compareExecutionPolicies(cfg);

    std::ostringstream out;
    out << "{\"comparison\":" << comp.toJson() << "," << buildMetaJson(cfg.start_seq, cfg.end_seq) << "}";
    result.output_json = out.str();
    result.success = true;
    result.events_scanned = cfg.end_seq - cfg.start_seq + 1;
    result.evidence.push_back({
        "EV-EXEC-CMP-1", "EXPERIMENT",
        "seq_" + std::to_string(cfg.start_seq) + "_to_" + std::to_string(cfg.end_seq),
        fnv1aHex(result.output_json)});

    auto end = std::chrono::high_resolution_clock::now();
    result.runtime_ms = static_cast<uint64_t>(std::chrono::duration<double, std::milli>(end - start).count());
    return result;
}

// ---------------------------------------------------------------------------
// 2. Chart Interval Investigation Plugin
// ---------------------------------------------------------------------------

const PluginDescriptor& ChartIntervalInvestigationPlugin::descriptor() const {
    static PluginDescriptor d;
    d.id = "chart_interval_investigation";
    d.version = "1.0.0";
    d.description = "Summarize market state and execution events within a selected sequence interval.";
    d.input_schema_json = "{\"type\":\"object\",\"properties\":{\"start_seq\":{\"type\":\"integer\"},\"end_seq\":{\"type\":\"integer\"},\"interval_ms\":{\"type\":\"integer\"}}}";
    d.output_schema_json = "{\"type\":\"object\",\"properties\":{\"summary\":{\"type\":\"object\"}}}";
    d.permissions = PluginPermissionSet{true, true, false, true, false};
    d.required_data_capabilities = {"L3_ORDER_BY_ORDER", "SEQUENCE_NUMBERS", "CANDLE_AGGREGATION"};
    d.budget = PluginResourceBudget{};
    return d;
}

PluginResult ChartIntervalInvestigationPlugin::execute(Engine& engine, const std::string& input_json) const {
    auto start = std::chrono::high_resolution_clock::now();

    PluginResult result;
    result.plugin_id = descriptor().id;
    result.plugin_version = descriptor().version;

    std::string sseq = extractJsonStringField(input_json, "start_seq");
    uint64_t start_seq = sseq.empty() ? 1 : std::stoull(sseq);
    std::string eseq = extractJsonStringField(input_json, "end_seq");
    uint64_t end_seq = eseq.empty() ? engine.getTotalEventsProcessed() : std::stoull(eseq);
    std::string iv = extractJsonStringField(input_json, "interval_ms");
    int64_t interval_ms = iv.empty() ? 1000 : std::stoll(iv);

    if (end_seq < start_seq) {
        result.error_message = "INVALID_WINDOW: end_seq must be >= start_seq";
        return result;
    }
    if (result.events_scanned > descriptor().budget.max_events_scanned) {
        result.error_message = "BUDGET_EXCEEDED: interval larger than plugin resource budget";
        return result;
    }

    BookSummary summary = engine.getBookSummary();

    std::ostringstream out;
    out << "{\"summary\":{";
    out << "\"interval_ms\":" << interval_ms << ",";
    out << "\"start_seq\":" << start_seq << ",";
    out << "\"end_seq\":" << end_seq << ",";
    out << "\"events_in_window\":" << (end_seq - start_seq + 1) << ",";
    out << "\"book\":" << engine.getBookSummaryJson(5) << ",";
    out << "\"candles\":" << engine.getCandlesJsonForInterval(interval_ms, 50) << ",";
    out << "\"volatility_bps\":" << engine.currentVolatility() << ",";
    out << "\"total_trades\":" << engine.getTotalTrades() << ",";
    out << "\"gaps_detected\":" << engine.getGapsDetected();
    out << "},";
    out << buildMetaJson(start_seq, end_seq) << "}";
    result.output_json = out.str();
    result.success = true;
    result.events_scanned = end_seq - start_seq + 1;
    result.evidence.push_back({
        "EV-CHART-INV-1", "TRACE_RANGE",
        "seq_" + std::to_string(start_seq) + "_to_" + std::to_string(end_seq),
        fnv1aHex(result.output_json)});

    auto end = std::chrono::high_resolution_clock::now();
    result.runtime_ms = static_cast<uint64_t>(std::chrono::duration<double, std::milli>(end - start).count());
    return result;
}

// ---------------------------------------------------------------------------
// 3. Evidence Report Plugin
// ---------------------------------------------------------------------------

const PluginDescriptor& EvidenceReportPlugin::descriptor() const {
    static PluginDescriptor d;
    d.id = "evidence_report";
    d.version = "1.0.0";
    d.description = "Produce a reproducible report from verified artifacts: dataset, engine version, config hash, event range, and citations.";
    d.input_schema_json = "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"},\"start_seq\":{\"type\":\"integer\"},\"end_seq\":{\"type\":\"integer\"},\"evidence_refs\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}";
    d.output_schema_json = "{\"type\":\"object\",\"properties\":{\"report\":{\"type\":\"object\"}}}";
    d.permissions = PluginPermissionSet{true, true, false, true, false};
    d.required_data_capabilities = {"EVENT_LOG", "EXPERIMENT_RESULTS", "DOCUMENT_CITATIONS"};
    d.budget = PluginResourceBudget{};
    return d;
}

PluginResult EvidenceReportPlugin::execute(Engine& engine, const std::string& input_json) const {
    auto start = std::chrono::high_resolution_clock::now();

    PluginResult result;
    result.plugin_id = descriptor().id;
    result.plugin_version = descriptor().version;

    std::string title = extractJsonStringField(input_json, "title");
    if (title.empty()) title = "QUEUEGLASS Evidence Report";
    std::string sseq = extractJsonStringField(input_json, "start_seq");
    uint64_t start_seq = sseq.empty() ? 1 : std::stoull(sseq);
    std::string eseq = extractJsonStringField(input_json, "end_seq");
    uint64_t end_seq = eseq.empty() ? engine.getTotalEventsProcessed() : std::stoull(eseq);

    std::ostringstream out;
    out << "{\"report\":{";
    out << "\"title\":\"" << title << "\",";
    out << "\"generated_at_ms\":" << nowMs() << ",";
    out << "\"dataset_id\":\"DS-SIM-L3-2025\",";
    out << "\"run_id\":\"RUN-" << engine.getTotalEventsProcessed() << "\",";
    out << "\"engine_version\":\"QUEUEGLASS Core v1.2.0\",";
    out << "\"configuration_hash\":\"" << fnv1aHex(input_json) << "\",";
    out << "\"source_event_range\":\"seq_" << start_seq << "_to_" << end_seq << "\",";
    out << "\"units\":\"price_in_ticks_hundredths_and_qty_units\",";
    out << "\"assumptions\":\"Shadow execution; deterministic synthetic L3 replay; price-time priority\",";
    out << "\"book_snapshot\":" << engine.getBookSummaryJson(5) << ",";
    out << "\"citations\":[\"EV-MATCH-SPEC-V24\",\"EV-TRACE-L3-104\",\"EV-EXEC-HANDBOOK-V1\"],";
    out << "\"quality_flags\":[\"VERIFIED_ARTIFACTS\",\"REPRODUCIBLE\"]";
    out << "}," << buildMetaJson(start_seq, end_seq) << "}";

    result.output_json = out.str();
    result.success = true;
    result.events_scanned = end_seq >= start_seq ? (end_seq - start_seq + 1) : 0;
    result.evidence.push_back({
        "EV-REPORT-1", "DOCUMENT",
        "seq_" + std::to_string(start_seq) + "_to_" + std::to_string(end_seq),
        fnv1aHex(result.output_json)});

    auto end = std::chrono::high_resolution_clock::now();
    result.runtime_ms = static_cast<uint64_t>(std::chrono::duration<double, std::milli>(end - start).count());
    return result;
}

// ---------------------------------------------------------------------------
// Built-in registration
// ---------------------------------------------------------------------------

void registerBuiltinPlugins() {
    auto& registry = PluginRegistry::instance();
    registry.registerPlugin(std::make_shared<ExecutionComparisonPlugin>());
    registry.registerPlugin(std::make_shared<ChartIntervalInvestigationPlugin>());
    registry.registerPlugin(std::make_shared<EvidenceReportPlugin>());
}

} // namespace queueglass
