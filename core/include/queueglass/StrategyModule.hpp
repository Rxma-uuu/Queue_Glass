#pragma once

#include "Types.hpp"
#include "MarketEvent.hpp"
#include "OrderBook.hpp"
#include <vector>
#include <string>
#include <cmath>

namespace queueglass {

struct MicrostructureStrategyConfig {
    double initial_capital{100000.0};    // Initial cash
    uint64_t max_position_units{500};     // Position exposure limit
    double imbalance_entry_threshold{0.30}; // Order book depth imbalance trigger threshold (|imbalance| > threshold)
    uint64_t trade_size_units{100};       // Fixed order size per entry signal
    double taker_fee_rate{0.0005};        // 5 bps
    int64_t hold_duration_ms{2000};       // Exit trade after N ms
    uint64_t start_seq{1};
    uint64_t end_seq{0};
};

struct StrategyTradeRecord {
    uint64_t trade_id{0};
    std::string side{"BUY"};               // "BUY" or "SELL"
    int64_t entry_timestamp{0};
    int64_t exit_timestamp{0};
    int64_t entry_price_ticks{0};
    int64_t exit_price_ticks{0};
    uint64_t qty_units{0};
    double realized_pnl{0.0};
    double total_fees{0.0};
    double return_pct{0.0};
    bool is_closed{false};
};

struct StrategyPerformanceResult {
    double initial_capital{100000.0};
    double ending_cash{100000.0};
    int64_t ending_position_units{0};
    double ending_unrealized_pnl{0.0};
    double total_realized_pnl{0.0};
    double total_fees_paid{0.0};
    double mark_to_market_equity{100000.0};

    uint32_t total_trades_count{0};
    uint32_t winning_trades_count{0};
    uint32_t losing_trades_count{0};
    double win_rate_pct{0.0};             // closed winning trades / closed total trades

    // Sharpe ratio calculation metadata
    bool sharpe_available{false};
    double sharpe_ratio{0.0};
    std::string sharpe_sampling_frequency{"1s sampling"};
    std::string sharpe_annualization_assumption{"Annualized assuming 252 trading days / 86400s per day"};

    // Time series for charts
    std::vector<int64_t> timestamps;
    std::vector<double> equity_curve;
    std::vector<double> drawdown_curve_pct;

    std::vector<StrategyTradeRecord> trades;

    // Synthetic disclaimer
    std::string strategy_disclaimer{"Research microstructure strategy model for software demonstration purposes; not indicative of live market profitability."};

    std::string toJson() const;
};

class MicrostructureStrategyEngine {
public:
    static StrategyPerformanceResult runBacktest(
        const std::vector<MarketEvent>& event_log,
        const MicrostructureStrategyConfig& config
    );
};

} // namespace queueglass
