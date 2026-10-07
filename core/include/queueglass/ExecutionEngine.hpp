#pragma once

#include "ExecutionPolicies.hpp"
#include "MarketEvent.hpp"
#include "OrderBook.hpp"
#include <vector>

namespace queueglass {

struct PolicyComparisonResult {
    ExecutionPolicyResult immediate;
    ExecutionPolicyResult twap;
    ExecutionPolicyResult passive;

    double cost_diff_twap_vs_immediate_bps{0.0};
    double cost_diff_passive_vs_immediate_bps{0.0};

    // Historical shadow execution disclaimer flag
    bool is_shadow_execution{true};
    std::string shadow_disclaimer{"Historical shadow execution assumes market path does not react to hypothetical order."};
    bool exceeds_depth_limits{false};

    std::string toJson() const;
};

class ExecutionEngine {
public:
    ExecutionEngine() = default;

    // Evaluates a policy configuration over a recorded event log deterministically.
    // Does not mutate any external state or live book.
    static ExecutionPolicyResult evaluatePolicy(
        const std::vector<MarketEvent>& event_log,
        const ExecutionPolicyConfig& config
    );

    // Evaluates and compares all 3 policies on the same underlying window.
    static PolicyComparisonResult comparePolicies(
        const std::vector<MarketEvent>& event_log,
        const ExecutionPolicyConfig& base_config
    );

private:
    static int64_t findArrivalMidpoint(
        const std::vector<MarketEvent>& event_log,
        size_t start_seq
    );
};

} // namespace queueglass
