#pragma once

#include "Types.hpp"
#include <cstdint>
#include <string>
#include <vector>

namespace queueglass {

// A single execution experiment: how a hypothetical order interacts with the
// replayed order book. All numbers are actual computed results — never mocked.
struct ExperimentConfig {
    // Literal text entered by the user, e.g. "BUY" / "SELL".
    std::string side{"BUY"};
    int64_t limit_price_ticks{0};   // 0 = market
    uint64_t qty_units{0};
    uint64_t start_seq{1};          // evaluate against book state after this seq
    uint64_t end_seq{0};            // 0 = until current replay position
};

struct FillRecord {
    int64_t timestamp{0};
    uint64_t seq_num{0};
    int64_t price_ticks{0};
    uint64_t qty_units{0};
    double notional{0.0};
};

struct ExperimentResult {
    // Echo of inputs for evidence display.
    ExperimentConfig config{};

    // Actual fill accounting.
    uint64_t filled_qty{0};
    uint64_t unfilled_qty{0};
    double avg_fill_price{0.0};        // in price units (ticks / 100.0)
    double total_notional{0.0};
    double fees{0.0};
    double slippage_vs_arrival_mid{0.0};
    int64_t arrival_mid_ticks{0};

    // Depth consumption evidence.
    std::vector<FillRecord> fills;
    uint32_t levels_consumed{0};

    // Gap/quality info for the evaluated window.
    uint64_t events_evaluated{0};
    uint64_t gaps_detected{0};

    std::string toJson() const;
};

// Simple performance measurement collected in-engine so the UI never invents
// throughput numbers. Durations are wall-clock nanoseconds measured around the
// actual work.
struct PerfStats {
    uint64_t events_processed{0};
    uint64_t replay_ns{0};          // total replay duration
    uint64_t book_update_ns{0};     // time spent applying book events
    uint64_t candles_ns{0};         // chart aggregation time
    uint64_t experiment_ns{0};      // last experiment evaluation time
    uint64_t peak_events_per_sec{0};
    uint64_t rss_bytes{0};          // best-effort resident memory, 0 if unavailable

    std::string toJson() const;
};

} // namespace queueglass
