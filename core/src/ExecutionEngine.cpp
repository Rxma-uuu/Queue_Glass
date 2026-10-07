#include "queueglass/ExecutionEngine.hpp"
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

std::string ExecutionPolicyResult::toJson() const {
    std::ostringstream json;
    json << "{";
    json << "\"policy_type\":\"" << policyTypeToString(policy_type) << "\",";
    json << "\"side\":\"" << side << "\",";
    json << "\"total_qty_units\":" << total_qty_units << ",";
    json << "\"filled_qty\":" << filled_qty << ",";
    json << "\"remaining_qty\":" << remaining_qty << ",";
    json << "\"fill_ratio\":" << fill_ratio << ",";
    json << "\"avg_fill_price\":" << avg_fill_price << ",";
    json << "\"total_notional\":" << total_notional << ",";
    json << "\"net_fees\":" << net_fees << ",";
    json << "\"arrival_timestamp\":" << arrival_timestamp << ",";
    json << "\"arrival_mid_ticks\":" << arrival_mid_ticks << ",";
    json << "\"completion_timestamp\":" << completion_timestamp << ",";
    json << "\"time_to_completion_ms\":" << time_to_completion_ms << ",";
    json << "\"execution_cost_bps\":" << execution_cost_bps << ",";
    json << "\"total_fills_count\":" << total_fills_count << ",";
    json << "\"slices_executed\":" << slices_executed << ",";
    json << "\"is_completed\":" << (is_completed ? "true" : "false") << ",";
    json << "\"completion_reason\":\"" << completion_reason << "\",";
    json << "\"markout_100ms_bps\":" << markout_100ms_bps << ",";
    json << "\"markout_500ms_bps\":" << markout_500ms_bps << ",";
    json << "\"fills\":[";
    for (size_t i = 0; i < fills.size(); ++i) {
        if (i > 0) json << ",";
        json << "{\"timestamp\":" << fills[i].timestamp
             << ",\"seq_num\":" << fills[i].seq_num
             << ",\"price_ticks\":" << fills[i].price_ticks
             << ",\"qty_units\":" << fills[i].qty_units
             << ",\"notional\":" << fills[i].notional << "}";
    }
    json << "]}";
    return json.str();
}

std::string PolicyComparisonResult::toJson() const {
    std::ostringstream json;
    json << "{";
    json << "\"immediate\":" << immediate.toJson() << ",";
    json << "\"twap\":" << twap.toJson() << ",";
    json << "\"passive\":" << passive.toJson() << ",";
    json << "\"cost_diff_twap_vs_immediate_bps\":" << cost_diff_twap_vs_immediate_bps << ",";
    json << "\"cost_diff_passive_vs_immediate_bps\":" << cost_diff_passive_vs_immediate_bps << ",";
    json << "\"is_shadow_execution\":" << (is_shadow_execution ? "true" : "false") << ",";
    json << "\"shadow_disclaimer\":\"" << shadow_disclaimer << "\",";
    json << "\"exceeds_depth_limits\":" << (exceeds_depth_limits ? "true" : "false");
    json << "}";
    return json.str();
}

int64_t ExecutionEngine::findArrivalMidpoint(
    const std::vector<MarketEvent>& event_log,
    size_t start_seq
) {
    OrderBook book;
    for (size_t i = 0; i < event_log.size(); ++i) {
        const auto& ev = event_log[i];
        if (ev.seq_num >= start_seq && start_seq > 0) break;
        switch (ev.event_type) {
            case EventType::ADD:
            case EventType::SNAPSHOT:
                book.addOrder(ev);
                break;
            case EventType::CANCEL:
                book.cancelOrder(ev);
                break;
            case EventType::MODIFY:
                book.modifyOrder(ev);
                break;
            case EventType::EXECUTE: {
                uint64_t q = 0;
                book.executeOrder(ev, q);
                break;
            }
        }
    }
    double mid = book.getMidpoint();
    return static_cast<int64_t>(std::llround(mid * 100.0));
}

ExecutionPolicyResult ExecutionEngine::evaluatePolicy(
    const std::vector<MarketEvent>& event_log,
    const ExecutionPolicyConfig& config
) {
    ExecutionPolicyResult result;
    result.policy_type = config.policy_type;
    result.side = config.side;
    result.total_qty_units = config.total_qty_units;
    result.remaining_qty = config.total_qty_units;

    const bool is_buy = (config.side == "BUY");

    // Determine evaluation window start index
    size_t begin_idx = 0;
    if (config.start_seq > 0) {
        for (size_t i = 0; i < event_log.size(); ++i) {
            if (event_log[i].seq_num >= config.start_seq) {
                begin_idx = i;
                break;
            }
        }
    }

    int64_t arrival_time = (begin_idx < event_log.size()) ? event_log[begin_idx].timestamp : 0;
    result.arrival_timestamp = arrival_time;
    result.arrival_mid_ticks = findArrivalMidpoint(event_log, config.start_seq);

    const int64_t active_time = arrival_time + static_cast<int64_t>(config.order_arrival_delay_ms);
    const int64_t deadline_time = (config.completion_deadline_ms > 0)
        ? (active_time + static_cast<int64_t>(config.completion_deadline_ms))
        : INT64_MAX;

    // Build initial order book up to begin_idx
    OrderBook book;
    size_t current_idx = 0;
    for (; current_idx < begin_idx && current_idx < event_log.size(); ++current_idx) {
        const auto& ev = event_log[current_idx];
        switch (ev.event_type) {
            case EventType::ADD: case EventType::SNAPSHOT: book.addOrder(ev); break;
            case EventType::CANCEL: book.cancelOrder(ev); break;
            case EventType::MODIFY: book.modifyOrder(ev); break;
            case EventType::EXECUTE: { uint64_t q = 0; book.executeOrder(ev, q); break; }
        }
    }

    if (config.policy_type == PolicyType::IMMEDIATE_AGGRESSIVE) {
        // Immediate Aggressive: Consume liquidity across order book immediately at active_time
        // Advance log to active_time
        while (current_idx < event_log.size() && event_log[current_idx].timestamp < active_time) {
            const auto& ev = event_log[current_idx++];
            switch (ev.event_type) {
                case EventType::ADD: case EventType::SNAPSHOT: book.addOrder(ev); break;
                case EventType::CANCEL: book.cancelOrder(ev); break;
                case EventType::MODIFY: book.modifyOrder(ev); break;
                case EventType::EXECUTE: { uint64_t q = 0; book.executeOrder(ev, q); break; }
            }
        }

        // Match against book levels
        auto levels = is_buy ? book.getAskLevels(50) : book.getBidLevels(50);
        uint64_t remaining = config.total_qty_units;

        for (const auto& lvl : levels) {
            if (remaining == 0) break;
            if (config.limit_price_ticks > 0) {
                if (is_buy && lvl.price_ticks > config.limit_price_ticks) break;
                if (!is_buy && lvl.price_ticks < config.limit_price_ticks) break;
            }

            uint64_t fill_qty = std::min(remaining, lvl.total_qty);
            if (fill_qty == 0) continue;

            FillRecord fr;
            fr.timestamp = (current_idx < event_log.size()) ? event_log[current_idx].timestamp : active_time;
            fr.seq_num = (current_idx < event_log.size()) ? event_log[current_idx].seq_num : 0;
            fr.price_ticks = lvl.price_ticks;
            fr.qty_units = fill_qty;
            fr.notional = static_cast<double>(fill_qty * lvl.price_ticks) / (100.0 * 100.0);

            result.fills.push_back(fr);
            result.filled_qty += fill_qty;
            result.total_notional += fr.notional;
            remaining -= fill_qty;
        }

        result.remaining_qty = remaining;
        result.slices_executed = 1;
        result.completion_timestamp = result.fills.empty() ? active_time : result.fills.back().timestamp;

    } else if (config.policy_type == PolicyType::TWAP) {
        // TWAP: Slice order into N equal slices over interval_ms
        uint32_t slices = std::max(1U, config.twap_num_slices);
        uint64_t slice_qty = config.total_qty_units / slices;
        uint64_t slice_rem = config.total_qty_units % slices;

        uint64_t total_remaining = config.total_qty_units;

        for (uint32_t s = 0; s < slices && total_remaining > 0; ++s) {
            uint64_t target_slice = slice_qty + (s == slices - 1 ? slice_rem : 0);
            int64_t slice_target_time = active_time + static_cast<int64_t>(s * config.twap_slice_interval_ms);

            if (slice_target_time > deadline_time) break;

            // Advance book to slice_target_time
            while (current_idx < event_log.size() && event_log[current_idx].timestamp < slice_target_time) {
                const auto& ev = event_log[current_idx++];
                switch (ev.event_type) {
                    case EventType::ADD: case EventType::SNAPSHOT: book.addOrder(ev); break;
                    case EventType::CANCEL: book.cancelOrder(ev); break;
                    case EventType::MODIFY: book.modifyOrder(ev); break;
                    case EventType::EXECUTE: { uint64_t q = 0; book.executeOrder(ev, q); break; }
                }
            }

            // Fill slice aggressively against book
            auto levels = is_buy ? book.getAskLevels(50) : book.getBidLevels(50);
            uint64_t current_slice_rem = target_slice;

            for (const auto& lvl : levels) {
                if (current_slice_rem == 0) break;
                if (config.limit_price_ticks > 0) {
                    if (is_buy && lvl.price_ticks > config.limit_price_ticks) break;
                    if (!is_buy && lvl.price_ticks < config.limit_price_ticks) break;
                }

                uint64_t fill_qty = std::min(current_slice_rem, lvl.total_qty);
                if (fill_qty == 0) continue;

                FillRecord fr;
                fr.timestamp = slice_target_time;
                fr.seq_num = (current_idx < event_log.size()) ? event_log[current_idx].seq_num : 0;
                fr.price_ticks = lvl.price_ticks;
                fr.qty_units = fill_qty;
                fr.notional = static_cast<double>(fill_qty * lvl.price_ticks) / (100.0 * 100.0);

                result.fills.push_back(fr);
                result.filled_qty += fill_qty;
                result.total_notional += fr.notional;
                current_slice_rem -= fill_qty;
                total_remaining -= fill_qty;
            }
            result.slices_executed++;
        }

        result.remaining_qty = total_remaining;
        result.completion_timestamp = result.fills.empty() ? active_time : result.fills.back().timestamp;

    } else if (config.policy_type == PolicyType::PASSIVE_LIMIT) {
        // Passive Limit Order with explicit queue model (TAIL vs HEAD vs PROPORTIONAL)
        uint64_t remaining = config.total_qty_units;

        // Advance to active_time
        while (current_idx < event_log.size() && event_log[current_idx].timestamp < active_time) {
            const auto& ev = event_log[current_idx++];
            switch (ev.event_type) {
                case EventType::ADD: case EventType::SNAPSHOT: book.addOrder(ev); break;
                case EventType::CANCEL: book.cancelOrder(ev); break;
                case EventType::MODIFY: book.modifyOrder(ev); break;
                case EventType::EXECUTE: { uint64_t q = 0; book.executeOrder(ev, q); break; }
            }
        }

        // Determine limit price
        int64_t limit_price = config.limit_price_ticks;
        if (limit_price == 0) {
            // Join best bid (if BUY) or best ask (if SELL) at arrival
            limit_price = is_buy ? book.getBestBid() : book.getBestAsk();
            if (limit_price == 0) limit_price = is_buy ? 10000 : 10010;
        }

        // Calculate queue ahead based on queue model
        uint64_t queue_ahead = 0;
        if (config.queue_model == "TAIL") {
            // Must wait for existing depth at limit_price to trade
            auto levels = is_buy ? book.getBidLevels(50) : book.getAskLevels(50);
            for (const auto& l : levels) {
                if (l.price_ticks == limit_price) {
                    queue_ahead = l.total_qty;
                    break;
                }
            }
        } else if (config.queue_model == "PROPORTIONAL") {
            auto levels = is_buy ? book.getBidLevels(50) : book.getAskLevels(50);
            for (const auto& l : levels) {
                if (l.price_ticks == limit_price) {
                    queue_ahead = l.total_qty / 2;
                    break;
                }
            }
        } // HEAD -> queue_ahead = 0

        // Process upcoming market trades up to deadline_time
        for (; current_idx < event_log.size() && remaining > 0; ++current_idx) {
            const auto& ev = event_log[current_idx];
            if (ev.timestamp > deadline_time) break;

            if (ev.event_type == EventType::EXECUTE) {
                bool trade_matches = is_buy ? (ev.side == Side::SELL && ev.price_ticks <= limit_price)
                                            : (ev.side == Side::BUY && ev.price_ticks >= limit_price);
                if (trade_matches) {
                    uint64_t trade_qty = ev.qty_units;
                    if (queue_ahead > 0) {
                        uint64_t queue_consumed = std::min(queue_ahead, trade_qty);
                        queue_ahead -= queue_consumed;
                        trade_qty -= queue_consumed;
                    }

                    if (trade_qty > 0) {
                        uint64_t fill_qty = std::min(remaining, trade_qty);
                        FillRecord fr;
                        fr.timestamp = ev.timestamp;
                        fr.seq_num = ev.seq_num;
                        fr.price_ticks = limit_price;
                        fr.qty_units = fill_qty;
                        fr.notional = static_cast<double>(fill_qty * limit_price) / (100.0 * 100.0);

                        result.fills.push_back(fr);
                        result.filled_qty += fill_qty;
                        result.total_notional += fr.notional;
                        remaining -= fill_qty;
                    }
                }
            }
            // Update book
            switch (ev.event_type) {
                case EventType::ADD: case EventType::SNAPSHOT: book.addOrder(ev); break;
                case EventType::CANCEL: book.cancelOrder(ev); break;
                case EventType::MODIFY: book.modifyOrder(ev); break;
                case EventType::EXECUTE: { uint64_t q = 0; book.executeOrder(ev, q); break; }
            }
        }

        result.remaining_qty = remaining;
        result.slices_executed = 1;
        result.completion_timestamp = result.fills.empty() ? active_time : result.fills.back().timestamp;
    }

    // Common Post-calculations
    result.fill_ratio = (config.total_qty_units > 0)
        ? static_cast<double>(result.filled_qty) / static_cast<double>(config.total_qty_units)
        : 0.0;
    result.total_fills_count = static_cast<uint32_t>(result.fills.size());

    if (result.filled_qty > 0) {
        result.avg_fill_price = result.total_notional / static_cast<double>(result.filled_qty);

        // Calculate net fees (taker vs maker)
        double rate = (config.policy_type == PolicyType::PASSIVE_LIMIT)
            ? config.maker_fee_rate
            : config.taker_fee_rate;
        result.net_fees = result.total_notional * rate;

        // Slippage vs arrival mid in bps
        if (result.arrival_mid_ticks > 0) {
            double arr_mid = ticksToPrice(result.arrival_mid_ticks);
            double diff = is_buy ? (result.avg_fill_price - arr_mid) : (arr_mid - result.avg_fill_price);
            result.execution_cost_bps = (diff / arr_mid) * 10000.0;
        }
    }

    result.is_completed = (result.remaining_qty == 0);
    if (result.is_completed) {
        result.completion_reason = "FILLED";
        if (result.completion_timestamp >= active_time) {
            result.time_to_completion_ms = (result.completion_timestamp - active_time) / 1000000;
        }
    } else {
        result.completion_reason = (result.filled_qty > 0) ? "PARTIAL_DEADLINE" : "DEADLINE_EXPIRED";
        result.time_to_completion_ms = -1; // Unavailable for incomplete orders
    }

    // Markout calculation (+100ms and +500ms after completion)
    if (!result.fills.empty()) {
        int64_t last_fill_time = result.fills.back().timestamp;
        int64_t t100 = last_fill_time + 100'000'000; // +100ms
        int64_t t500 = last_fill_time + 500'000'000; // +500ms

        int64_t mid_100 = 0, mid_500 = 0;
        for (const auto& ev : event_log) {
            if (ev.timestamp >= t100 && mid_100 == 0) mid_100 = ev.price_ticks;
            if (ev.timestamp >= t500 && mid_500 == 0) mid_500 = ev.price_ticks;
        }
        if (mid_100 > 0 && result.avg_fill_price > 0) {
            double p100 = ticksToPrice(mid_100);
            double diff = is_buy ? (p100 - result.avg_fill_price) : (result.avg_fill_price - p100);
            result.markout_100ms_bps = (diff / result.avg_fill_price) * 10000.0;
        }
        if (mid_500 > 0 && result.avg_fill_price > 0) {
            double p500 = ticksToPrice(mid_500);
            double diff = is_buy ? (p500 - result.avg_fill_price) : (result.avg_fill_price - p500);
            result.markout_500ms_bps = (diff / result.avg_fill_price) * 10000.0;
        }
    }

    return result;
}

PolicyComparisonResult ExecutionEngine::comparePolicies(
    const std::vector<MarketEvent>& event_log,
    const ExecutionPolicyConfig& base_config
) {
    PolicyComparisonResult comp;

    ExecutionPolicyConfig cfg_imm = base_config;
    cfg_imm.policy_type = PolicyType::IMMEDIATE_AGGRESSIVE;

    ExecutionPolicyConfig cfg_twap = base_config;
    cfg_twap.policy_type = PolicyType::TWAP;

    ExecutionPolicyConfig cfg_pas = base_config;
    cfg_pas.policy_type = PolicyType::PASSIVE_LIMIT;

    comp.immediate = evaluatePolicy(event_log, cfg_imm);
    comp.twap = evaluatePolicy(event_log, cfg_twap);
    comp.passive = evaluatePolicy(event_log, cfg_pas);

    comp.cost_diff_twap_vs_immediate_bps = comp.twap.execution_cost_bps - comp.immediate.execution_cost_bps;
    comp.cost_diff_passive_vs_immediate_bps = comp.passive.execution_cost_bps - comp.immediate.execution_cost_bps;

    comp.is_shadow_execution = true;
    comp.shadow_disclaimer = "Historical shadow execution assumes market path does not react to hypothetical order.";
    comp.exceeds_depth_limits = (base_config.total_qty_units > 10000);

    return comp;
}

} // namespace queueglass
