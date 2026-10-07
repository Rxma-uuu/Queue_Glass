#pragma once

#include "Types.hpp"
#include <cstdint>
#include <vector>
#include <string>

namespace queueglass {

// A single executed trade. Feeds candle aggregation, volatility and volume stats.
struct Trade {
    int64_t timestamp{0};
    int64_t price_ticks{0};
    uint64_t qty_units{0};
    Side aggressor{Side::BUY};
};

struct Candle {
    int64_t open_time{0};
    int64_t close_time{0};
    int64_t open{0};
    int64_t high{0};
    int64_t low{0};
    int64_t close{0};
    uint64_t volume{0};
    uint32_t trade_count{0};

    std::string toJson() const {
        std::string json = "{";
        json += "\"open_time\":" + std::to_string(open_time) + ",";
        json += "\"close_time\":" + std::to_string(close_time) + ",";
        json += "\"open\":" + std::to_string(open) + ",";
        json += "\"high\":" + std::to_string(high) + ",";
        json += "\"low\":" + std::to_string(low) + ",";
        json += "\"close\":" + std::to_string(close) + ",";
        json += "\"volume\":" + std::to_string(volume) + ",";
        json += "\"trade_count\":" + std::to_string(trade_count);
        json += "}";
        return json;
    }
};

class CandleAggregator {
public:
    explicit CandleAggregator(int64_t interval = 1000) : interval_(interval) {}

    // Trades are recorded from successful EXECUTE events. Interval is in ms.
    void processTrade(int64_t timestamp, int64_t price, uint64_t volume);
    void reset();

    std::vector<Candle> getCandles(size_t max_count = 100) const;
    std::string toJson(size_t max_count = 100) const;

    int64_t getInterval() const { return interval_; }
    void setInterval(int64_t interval) { interval_ = interval; }

    // State export/import for Checkpoint
    const std::vector<Candle>& getAllCandles() const { return completed_candles_; }
    const Candle& getCurrentCandle() const { return current_candle_; }
    bool hasCurrentCandle() const { return has_active_candle_; }
    void restoreState(const std::vector<Candle>& completed, bool has_active, const Candle& active);

private:
    int64_t interval_{1000}; // candle time window unit (e.g. ms)
    bool has_active_candle_{false};
    Candle current_candle_{};
    std::vector<Candle> completed_candles_;
};

} // namespace queueglass
