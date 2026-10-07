#pragma once

#include "Types.hpp"
#include "OrderBook.hpp"
#include "Candle.hpp"
#include "Volatility.hpp"
#include "Trace.hpp"
#include "EventGenerator.hpp"
#include <cstdint>
#include <vector>
#include <string>

namespace queueglass {

struct Checkpoint {
    uint64_t checkpoint_id{0};
    int64_t timestamp{0};
    uint64_t next_seq_num{0};
    uint64_t total_events_processed{0};
    uint64_t total_trades{0};
    uint64_t total_volume{0};

    // Engine states
    std::vector<Order> active_orders;
    std::vector<Candle> completed_candles;
    bool has_active_candle{false};
    Candle active_candle{};
    std::vector<int64_t> volatility_prices;
    std::vector<TraceRecord> trace_records;
    EventGenerator::GeneratorState generator_state;

    std::string toJson() const;
};

} // namespace queueglass
