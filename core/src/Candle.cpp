#include "queueglass/Candle.hpp"
#include <algorithm>

namespace queueglass {

void CandleAggregator::processTrade(int64_t timestamp, int64_t price, uint64_t volume) {
    if (interval_ <= 0) interval_ = 1000;
    int64_t window_start = (timestamp / interval_) * interval_;
    int64_t window_end = window_start + interval_;

    if (!has_active_candle_) {
        current_candle_ = Candle{
            window_start,
            window_end,
            price,
            price,
            price,
            price,
            volume,
            1
        };
        has_active_candle_ = true;
    } else {
        if (timestamp < current_candle_.close_time) {
            current_candle_.high = std::max(current_candle_.high, price);
            current_candle_.low = std::min(current_candle_.low, price);
            current_candle_.close = price;
            current_candle_.volume += volume;
            current_candle_.trade_count++;
        } else {
            completed_candles_.push_back(current_candle_);
            current_candle_ = Candle{
                window_start,
                window_end,
                price,
                price,
                price,
                price,
                volume,
                1
            };
        }
    }
}

void CandleAggregator::reset() {
    has_active_candle_ = false;
    current_candle_ = Candle{};
    completed_candles_.clear();
}

std::vector<Candle> CandleAggregator::getCandles(size_t max_count) const {
    std::vector<Candle> all = completed_candles_;
    if (has_active_candle_) {
        all.push_back(current_candle_);
    }

    if (all.size() > max_count) {
        return std::vector<Candle>(all.end() - max_count, all.end());
    }
    return all;
}

std::string CandleAggregator::toJson(size_t max_count) const {
    auto candles = getCandles(max_count);
    std::string json = "[";
    for (size_t i = 0; i < candles.size(); ++i) {
        if (i > 0) json += ",";
        json += candles[i].toJson();
    }
    json += "]";
    return json;
}

void CandleAggregator::restoreState(const std::vector<Candle>& completed, bool has_active, const Candle& active) {
    completed_candles_ = completed;
    has_active_candle_ = has_active;
    current_candle_ = active;
}

} // namespace queueglass
