#include "queueglass/StrategyModule.hpp"
#include <algorithm>
#include <cmath>
#include <sstream>
#include <iomanip>

namespace queueglass {

namespace {

inline double ticksToPrice(int64_t ticks) {
    return static_cast<double>(ticks) / 100.0;
}

} // namespace

std::string StrategyPerformanceResult::toJson() const {
    std::ostringstream json;
    json << "{";
    json << "\"initial_capital\":" << initial_capital << ",";
    json << "\"ending_cash\":" << ending_cash << ",";
    json << "\"ending_position_units\":" << ending_position_units << ",";
    json << "\"ending_unrealized_pnl\":" << ending_unrealized_pnl << ",";
    json << "\"total_realized_pnl\":" << total_realized_pnl << ",";
    json << "\"total_fees_paid\":" << total_fees_paid << ",";
    json << "\"mark_to_market_equity\":" << mark_to_market_equity << ",";
    json << "\"total_trades_count\":" << total_trades_count << ",";
    json << "\"winning_trades_count\":" << winning_trades_count << ",";
    json << "\"losing_trades_count\":" << losing_trades_count << ",";
    json << "\"win_rate_pct\":" << win_rate_pct << ",";
    json << "\"sharpe_available\":" << (sharpe_available ? "true" : "false") << ",";
    json << "\"sharpe_ratio\":" << sharpe_ratio << ",";
    json << "\"sharpe_sampling_frequency\":\"" << sharpe_sampling_frequency << "\",";
    json << "\"sharpe_annualization_assumption\":\"" << sharpe_annualization_assumption << "\",";
    json << "\"strategy_disclaimer\":\"" << strategy_disclaimer << "\",";

    json << "\"equity_curve\":[";
    for (size_t i = 0; i < equity_curve.size(); ++i) {
        if (i > 0) json << ",";
        json << "{\"timestamp\":" << (i < timestamps.size() ? timestamps[i] : 0)
             << ",\"equity\":" << equity_curve[i]
             << ",\"drawdown_pct\":" << (i < drawdown_curve_pct.size() ? drawdown_curve_pct[i] : 0.0) << "}";
    }
    json << "],";

    json << "\"trades\":[";
    for (size_t i = 0; i < trades.size(); ++i) {
        if (i > 0) json << ",";
        json << "{\"trade_id\":" << trades[i].trade_id
             << ",\"side\":\"" << trades[i].side << "\""
             << ",\"entry_time\":" << trades[i].entry_timestamp
             << ",\"exit_time\":" << trades[i].exit_timestamp
             << ",\"entry_price_ticks\":" << trades[i].entry_price_ticks
             << ",\"exit_price_ticks\":" << trades[i].exit_price_ticks
             << ",\"qty_units\":" << trades[i].qty_units
             << ",\"realized_pnl\":" << trades[i].realized_pnl
             << ",\"total_fees\":" << trades[i].total_fees
             << ",\"return_pct\":" << trades[i].return_pct
             << ",\"is_closed\":" << (trades[i].is_closed ? "true" : "false") << "}";
    }
    json << "]}";
    return json.str();
}

StrategyPerformanceResult MicrostructureStrategyEngine::runBacktest(
    const std::vector<MarketEvent>& event_log,
    const MicrostructureStrategyConfig& config
) {
    StrategyPerformanceResult res;
    res.initial_capital = config.initial_capital;

    double cash = config.initial_capital;
    int64_t position = 0;
    double realized_pnl = 0.0;
    double total_fees = 0.0;
    uint64_t next_trade_id = 1;

    OrderBook book;
    double peak_equity = config.initial_capital;

    std::vector<double> returns_series;

    struct ActivePosition {
        uint64_t trade_id;
        std::string side;
        int64_t entry_time;
        int64_t entry_price;
        uint64_t qty;
        int64_t exit_deadline;
    };
    std::vector<ActivePosition> active_positions;

    int64_t last_sample_time = 0;

    for (size_t i = 0; i < event_log.size(); ++i) {
        const auto& ev = event_log[i];

        // Process event in book first
        switch (ev.event_type) {
            case EventType::ADD: case EventType::SNAPSHOT: book.addOrder(ev); break;
            case EventType::CANCEL: book.cancelOrder(ev); break;
            case EventType::MODIFY: book.modifyOrder(ev); break;
            case EventType::EXECUTE: { uint64_t q = 0; book.executeOrder(ev, q); break; }
        }

        // Check active positions for exit deadline
        auto pos_it = active_positions.begin();
        while (pos_it != active_positions.end()) {
            if (ev.timestamp >= pos_it->exit_deadline) {
                // Exit position at current best bid / ask
                int64_t exit_price = (pos_it->side == "BUY") ? book.getBestBid() : book.getBestAsk();
                if (exit_price == 0) exit_price = pos_it->entry_price;

                double entry_p = ticksToPrice(pos_it->entry_price);
                double exit_p = ticksToPrice(exit_price);
                double notional_entry = entry_p * pos_it->qty;
                double notional_exit = exit_p * pos_it->qty;

                double entry_fee = notional_entry * config.taker_fee_rate;
                double exit_fee = notional_exit * config.taker_fee_rate;
                double trade_fees = entry_fee + exit_fee;

                double trade_pnl = 0.0;
                if (pos_it->side == "BUY") {
                    trade_pnl = (notional_exit - notional_entry) - trade_fees;
                    position -= static_cast<int64_t>(pos_it->qty);
                } else {
                    trade_pnl = (notional_entry - notional_exit) - trade_fees;
                    position += static_cast<int64_t>(pos_it->qty);
                }

                cash += trade_pnl;
                realized_pnl += trade_pnl;
                total_fees += trade_fees;

                StrategyTradeRecord tr;
                tr.trade_id = pos_it->trade_id;
                tr.side = pos_it->side;
                tr.entry_timestamp = pos_it->entry_time;
                tr.exit_timestamp = ev.timestamp;
                tr.entry_price_ticks = pos_it->entry_price;
                tr.exit_price_ticks = exit_price;
                tr.qty_units = pos_it->qty;
                tr.realized_pnl = trade_pnl;
                tr.total_fees = trade_fees;
                tr.return_pct = (notional_entry > 0) ? (trade_pnl / notional_entry) * 100.0 : 0.0;
                tr.is_closed = true;

                res.trades.push_back(tr);
                if (trade_pnl > 0) res.winning_trades_count++;
                else if (trade_pnl < 0) res.losing_trades_count++;

                pos_it = active_positions.erase(pos_it);
            } else {
                ++pos_it;
            }
        }

        // Check entry conditions based on depth imbalance
        double imbalance = book.getDepthImbalance(5);
        int64_t best_bid = book.getBestBid();
        int64_t best_ask = book.getBestAsk();

        if (best_bid > 0 && best_ask > 0 && std::abs(imbalance) >= config.imbalance_entry_threshold) {
            // Signal detected: Positive imbalance -> BUY (aggress ask), Negative imbalance -> SELL (aggress bid)
            std::string signal_side = (imbalance > 0) ? "BUY" : "SELL";
            int64_t entry_price = (signal_side == "BUY") ? best_ask : best_bid;

            bool can_enter = false;
            if (signal_side == "BUY" && (position + static_cast<int64_t>(config.trade_size_units)) <= static_cast<int64_t>(config.max_position_units)) {
                can_enter = true;
            } else if (signal_side == "SELL" && (position - static_cast<int64_t>(config.trade_size_units)) >= -static_cast<int64_t>(config.max_position_units)) {
                can_enter = true;
            }

            if (can_enter) {
                ActivePosition new_pos;
                new_pos.trade_id = next_trade_id++;
                new_pos.side = signal_side;
                new_pos.entry_time = ev.timestamp;
                new_pos.entry_price = entry_price;
                new_pos.qty = config.trade_size_units;
                new_pos.exit_deadline = ev.timestamp + (config.hold_duration_ms * 1000000);

                if (signal_side == "BUY") position += config.trade_size_units;
                else position -= config.trade_size_units;

                active_positions.push_back(new_pos);
            }
        }

        // Periodic sample for equity curve and Sharpe ratio (every ~1s = 1,000,000,000ns)
        if (ev.timestamp - last_sample_time >= 1000000000LL || last_sample_time == 0) {
            double current_mid = ticksToPrice(book.getMidpoint());
            double unrealized_pnl = position * (current_mid - ticksToPrice(10000)); // mark to market against base
            double current_equity = cash + unrealized_pnl;

            res.timestamps.push_back(ev.timestamp);
            res.equity_curve.push_back(current_equity);

            peak_equity = std::max(peak_equity, current_equity);
            double dd_pct = (peak_equity > 0) ? ((peak_equity - current_equity) / peak_equity) * 100.0 : 0.0;
            res.drawdown_curve_pct.push_back(dd_pct);

            if (!res.equity_curve.empty() && res.equity_curve.size() > 1) {
                double prev_eq = res.equity_curve[res.equity_curve.size() - 2];
                if (prev_eq > 0) {
                    returns_series.push_back((current_equity - prev_eq) / prev_eq);
                }
            }

            last_sample_time = ev.timestamp;
        }
    }

    // Wrap up final states
    double final_mid = ticksToPrice(book.getMidpoint());
    double ending_unrealized = position * (final_mid - ticksToPrice(10000));

    res.ending_cash = cash;
    res.ending_position_units = position;
    res.ending_unrealized_pnl = ending_unrealized;
    res.total_realized_pnl = realized_pnl;
    res.total_fees_paid = total_fees;
    res.mark_to_market_equity = cash + ending_unrealized;
    res.total_trades_count = static_cast<uint32_t>(res.trades.size());

    if (res.total_trades_count > 0) {
        res.win_rate_pct = (static_cast<double>(res.winning_trades_count) / static_cast<double>(res.total_trades_count)) * 100.0;
    }

    // Sharpe ratio calculation
    if (returns_series.size() >= 10) {
        double sum = 0.0;
        for (double r : returns_series) sum += r;
        double mean = sum / returns_series.size();

        double sq_sum = 0.0;
        for (double r : returns_series) sq_sum += (r - mean) * (r - mean);
        double variance = sq_sum / (returns_series.size() - 1);

        if (variance > 1e-12) {
            double std_dev = std::sqrt(variance);
            // Annualize assuming 1s samples -> sqrt(252 * 86400) ~ sqrt(21772800) ~ 4666.13
            double sharpe = (mean / std_dev) * 4666.13;
            res.sharpe_ratio = sharpe;
            res.sharpe_available = true;
        } else {
            res.sharpe_available = false; // Zero variance
        }
    } else {
        res.sharpe_available = false; // Insufficient observations
    }

    return res;
}

} // namespace queueglass
