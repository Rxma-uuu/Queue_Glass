#pragma once

#include "Types.hpp"
#include "MarketEvent.hpp"
#include "OrderBook.hpp"
#include "Experiment.hpp"
#include <vector>
#include <string>
#include <cmath>

namespace queueglass {

enum class PolicyType {
    IMMEDIATE_AGGRESSIVE,
    TWAP,
    PASSIVE_LIMIT
};

inline std::string policyTypeToString(PolicyType p) {
    switch (p) {
        case PolicyType::IMMEDIATE_AGGRESSIVE: return "IMMEDIATE_AGGRESSIVE";
        case PolicyType::TWAP: return "TWAP";
        case PolicyType::PASSIVE_LIMIT: return "PASSIVE_LIMIT";
    }
    return "UNKNOWN";
}

inline PolicyType stringToPolicyType(const std::string& s) {
    if (s == "TWAP") return PolicyType::TWAP;
    if (s == "PASSIVE_LIMIT") return PolicyType::PASSIVE_LIMIT;
    return PolicyType::IMMEDIATE_AGGRESSIVE;
}

struct ExecutionPolicyConfig {
    PolicyType policy_type{PolicyType::IMMEDIATE_AGGRESSIVE};
    std::string side{"BUY"};
    uint64_t total_qty_units{100};
    int64_t limit_price_ticks{0};       // 0 = market
    uint64_t order_arrival_delay_ms{0}; // delay before first slice/order is active
    uint64_t cancellation_delay_ms{0};  // delay to process cancellation
    uint64_t completion_deadline_ms{0}; // absolute max duration from arrival before cancellation

    // TWAP specific
    uint32_t twap_num_slices{4};
    uint64_t twap_slice_interval_ms{1000};

    // Passive Limit specific
    // Queue priority assumption: "HEAD" (fills immediately when touched),
    // "TAIL" (must wait for resting depth ahead to fill), or "PROPORTIONAL"
    std::string queue_model{"TAIL"};

    // Fee rates (e.g. 0.0005 = 5 bps taker, -0.0001 = -1 bps maker rebate)
    double taker_fee_rate{0.0005};
    double maker_fee_rate{-0.0001};

    // Evaluation window bounds
    uint64_t start_seq{1};
    uint64_t end_seq{0}; // 0 = process all remaining
};

struct ExecutionPolicyResult {
    PolicyType policy_type{PolicyType::IMMEDIATE_AGGRESSIVE};
    std::string side{"BUY"};
    uint64_t total_qty_units{0};
    uint64_t filled_qty{0};
    uint64_t remaining_qty{0};
    double fill_ratio{0.0};              // filled_qty / total_qty_units

    double avg_fill_price{0.0};          // in price units (ticks / 100.0)
    double total_notional{0.0};
    double net_fees{0.0};                // positive = fee paid, negative = rebate received

    int64_t arrival_timestamp{0};
    int64_t arrival_mid_ticks{0};
    int64_t completion_timestamp{0};
    int64_t time_to_completion_ms{-1};   // -1 if incomplete or unavailable

    double execution_cost_bps{0.0};      // slippage vs arrival mid in bps
    uint32_t total_fills_count{0};
    uint32_t slices_executed{0};

    // Residual quantity tracking
    bool is_completed{false};
    std::string completion_reason;       // "FILLED", "DEADLINE_EXPIRED", "PARTIAL_DEADLINE", "NO_LIQUIDITY"

    // Markout at specified horizons (e.g. +100ms, +500ms, +1000ms mid change from fill)
    double markout_100ms_bps{0.0};
    double markout_500ms_bps{0.0};

    std::vector<FillRecord> fills;

    std::string toJson() const;
};

} // namespace queueglass
