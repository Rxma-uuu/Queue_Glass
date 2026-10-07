#pragma once

// Section 18: Internal Plugins
//
// Reusable approved research workflows packaged as backend modules. Plugins
// are statically compiled into the QUEUEGLASS core library. Arbitrary
// downloaded native code is NEVER dynamically loaded on Android.

#include "Types.hpp"
#include "Experiment.hpp"
#include <string>
#include <vector>
#include <memory>

namespace queueglass {

class Engine;

// Resource budget enforced by the host before a plugin handler runs.
struct PluginResourceBudget {
    uint64_t max_events_scanned{100000};
    uint64_t max_output_bytes{262144};   // 256 KiB bounded responses
    uint32_t max_runtime_hint_ms{2000};
};

struct PluginPermissionSet {
    bool read_event_log{true};
    bool read_order_book{true};
    bool run_experiments{false};
    bool write_evidence{false};
    bool network_access{false};          // always false for internal plugins
};

struct PluginDescriptor {
    std::string id;
    std::string version;
    std::string description;
    std::string input_schema_json;
    std::string output_schema_json;
    PluginPermissionSet permissions;
    std::vector<std::string> required_data_capabilities;
    PluginResourceBudget budget;
};

struct PluginEvidenceRef {
    std::string evidence_id;
    std::string kind;        // "TRACE_RANGE", "EXPERIMENT", "DOCUMENT"
    std::string reference;   // e.g. "seq_100_to_500" or "EV-MATCH-SPEC-V24"
    std::string checksum;    // deterministic content hash of the artifact
};

struct PluginResult {
    bool success{false};
    std::string plugin_id;
    std::string plugin_version;
    std::string error_message;
    std::string output_json;                 // bounded by budget.max_output_bytes
    std::vector<PluginEvidenceRef> evidence; // evidence output contract
    uint64_t events_scanned{0};
    uint64_t runtime_ms{0};

    std::string toJson() const;
};

// Abstract interface every internal plugin implements.
class InternalPlugin {
public:
    virtual ~InternalPlugin() = default;

    virtual const PluginDescriptor& descriptor() const = 0;

    // Handler: executes deterministically against the engine's recorded
    // event log. Never mutates the live book.
    virtual PluginResult execute(Engine& engine, const std::string& input_json) const = 0;
};

// Host registry for statically packaged backend plugin modules.
class PluginRegistry {
public:
    static PluginRegistry& instance();

    void registerPlugin(std::shared_ptr<InternalPlugin> plugin);
    std::vector<PluginDescriptor> listDescriptors() const;
    std::string listDescriptorsJson() const;

    // Runs permission + capability + budget checks before dispatching.
    PluginResult runPlugin(const std::string& plugin_id, Engine& engine, const std::string& input_json) const;

private:
    PluginRegistry() = default;
    std::vector<std::shared_ptr<InternalPlugin>> plugins_;
};

// --- Approved internal plugins -------------------------------------------

// 1. Execution Comparison: compare approved policies using the C++ engine.
class ExecutionComparisonPlugin : public InternalPlugin {
public:
    const PluginDescriptor& descriptor() const override;
    PluginResult execute(Engine& engine, const std::string& input_json) const override;
};

// 2. Chart Interval Investigation: summarize market state and execution
//    events in a selected interval.
class ChartIntervalInvestigationPlugin : public InternalPlugin {
public:
    const PluginDescriptor& descriptor() const override;
    PluginResult execute(Engine& engine, const std::string& input_json) const override;
};

// 3. Evidence Report: produce a reproducible report from verified artifacts.
class EvidenceReportPlugin : public InternalPlugin {
public:
    const PluginDescriptor& descriptor() const override;
    PluginResult execute(Engine& engine, const std::string& input_json) const override;
};

// Registers all built-in plugins with the global registry (idempotent).
void registerBuiltinPlugins();

} // namespace queueglass
