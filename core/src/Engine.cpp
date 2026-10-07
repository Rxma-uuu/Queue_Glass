#include "queueglass/Engine.hpp"
#include "queueglass/Experiment.hpp"
#include <algorithm>
#include <chrono>
#include <cmath>
#include <sstream>

namespace queueglass {

namespace {

inline uint64_t nowNs() {
    return static_cast<uint64_t>(
        std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count());
}

inline double ticksToPrice(int64_t ticks) {
    return static_cast<double>(ticks) / 100.0;
}

} // namespace

Engine::Engine(uint64_t seed) : seed_(seed), event_generator_(seed) {}

ValidationCode Engine::validateEvent(const MarketEvent& event) const {
    // Sequence duplicates are hard errors where seq numbers are used.
    if (event.seq_num != 0 && processed_seq_nums_.count(event.seq_num) > 0) {
        return ValidationCode::DUPLICATE_SEQUENCE;
    }
    switch (event.event_type) {
        case EventType::ADD:
        case EventType::SNAPSHOT:
            if (event.price_ticks <= 0) return ValidationCode::INVALID_PRICE;
            if (event.qty_units == 0) return ValidationCode::INVALID_QUANTITY;
            if (event.order_id == 0) return ValidationCode::ORDER_NOT_FOUND;
            if (event.event_type == EventType::ADD && order_book_.hasOrder(event.order_id)) {
                return ValidationCode::DUPLICATE_ORDER_ID;
            }
            break;
        case EventType::CANCEL:
        case EventType::MODIFY:
        case EventType::EXECUTE:
            if (event.order_id == 0) return ValidationCode::ORDER_NOT_FOUND;
            break;
    }
    return ValidationCode::OK;
}

ValidationCode Engine::processEvent(const MarketEvent& event) {
    // Explicit sequence-gap detection: a gap is any seq_num more than 1 above
    // the last processed seq. Gaps are counted and reported, never silently
    // filled with invented events.
    if (event.seq_num != 0 && last_seq_ != 0 && event.seq_num > last_seq_ + 1) {
        ++gaps_detected_;
    }
    if (event.seq_num != 0 && event.seq_num > last_seq_) {
        last_seq_ = event.seq_num;
    }

    const ValidationCode pre = validateEvent(event);
    if (pre != ValidationCode::OK) {
        ++rejected_count_;
        trace_logger_.log(event, false, pre, validationCodeToString(pre), order_book_.getSummary());
        return pre;
    }

    const uint64_t t0 = nowNs();
    ValidationCode code = ValidationCode::OK;
    uint64_t executed_qty = 0;

    switch (event.event_type) {
        case EventType::ADD:
        case EventType::SNAPSHOT:
            code = order_book_.addOrder(event);
            break;
        case EventType::CANCEL:
            code = order_book_.cancelOrder(event);
            break;
        case EventType::MODIFY:
            code = order_book_.modifyOrder(event);
            break;
        case EventType::EXECUTE:
            code = order_book_.executeOrder(event, executed_qty);
            break;
    }

    const uint64_t t1 = nowNs();
    book_ns_accum_ += (t1 - t0);

    if (code != ValidationCode::OK) {
        ++rejected_count_;
        trace_logger_.log(event, false, code, validationCodeToString(code), order_book_.getSummary());
        return code;
    }

    if (event.seq_num != 0) {
        processed_seq_nums_.insert(event.seq_num);
    }
    event_log_.push_back(event);
    ++total_events_processed_;

    if (event.event_type == EventType::EXECUTE && executed_qty > 0) {
        ++total_trades_;
        if (safeAddUint(total_volume_, executed_qty, total_volume_) == false) {
            total_volume_ = UINT64_MAX;
        }
        const uint64_t tc0 = nowNs();
        candle_aggregator_.processTrade(event.timestamp, event.price_ticks, executed_qty);
        candle_ns_accum_ += (nowNs() - tc0);
        volatility_.addPrice(event.price_ticks);
    }

    trace_logger_.log(event, true, ValidationCode::OK, "", order_book_.getSummary());
    return ValidationCode::OK;
}

StepResult Engine::stepEvents(size_t count) {
    StepResult result;
    result.requested_count = count;

    const uint64_t replay_start = nowNs();

    for (size_t i = 0; i < count; ++i) {
        const MarketEvent ev = event_generator_.generateEvent();
        const ValidationCode code = processEvent(ev);
        ++result.processed_count;
        if (code == ValidationCode::OK) {
            ++result.valid_count;
        } else {
            ++result.rejected_count;
        }
    }

    result.total_trades = total_trades_;
    result.total_volume = total_volume_;
    result.current_book_summary = order_book_.getSummary();

    const uint64_t elapsed = nowNs() - replay_start;
    perf_.replay_ns += elapsed;
    perf_.events_processed += result.processed_count;
    if (elapsed > 0) {
        const uint64_t eps = result.processed_count * 1'000'000'000ULL / elapsed;
        perf_.peak_events_per_sec = std::max(perf_.peak_events_per_sec, eps);
    }
    perf_.book_update_ns = book_ns_accum_;
    perf_.candles_ns = candle_ns_accum_;

    return result;
}

BookSummary Engine::getBookSummary() const {
    return order_book_.getSummary();
}

std::string Engine::getBookSummaryJson(size_t depth_levels) const {
    return order_book_.toJson(depth_levels);
}

std::string Engine::getCandlesJson(size_t max_count) const {
    return candle_aggregator_.toJson(max_count);
}

std::string Engine::getCandlesJsonForInterval(int64_t interval_ms, size_t max_count) const {
    if (interval_ms <= 0) interval_ms = 1000;
    CandleAggregator agg(interval_ms);
    for (const MarketEvent& ev : event_log_) {
        if (ev.event_type != EventType::EXECUTE) continue;
        agg.processTrade(ev.timestamp, ev.price_ticks, ev.qty_units);
    }
    return agg.toJson(max_count);
}

std::string Engine::getEventTraceJson(size_t max_lines) const {
    return trace_logger_.toJson(max_lines);
}

uint64_t Engine::createCheckpoint() {
    Checkpoint cp;
    cp.checkpoint_id = next_checkpoint_id_++;
    cp.timestamp = event_generator_.getState().timestamp;
    cp.next_seq_num = event_generator_.getState().seq_num;
    cp.total_events_processed = total_events_processed_;
    cp.total_trades = total_trades_;
    cp.total_volume = total_volume_;
    cp.active_orders = order_book_.getAllActiveOrders();
    cp.completed_candles = candle_aggregator_.getAllCandles();
    cp.has_active_candle = candle_aggregator_.hasCurrentCandle();
    cp.active_candle = candle_aggregator_.getCurrentCandle();
    cp.volatility_prices = volatility_.getPrices();
    cp.trace_records = trace_logger_.getRecords(trace_logger_.maxRecords());
    cp.generator_state = event_generator_.getState();
    checkpoints_[cp.checkpoint_id] = std::move(cp);
    return next_checkpoint_id_ - 1;
}

bool Engine::restoreCheckpoint(uint64_t checkpoint_id) {
    auto it = checkpoints_.find(checkpoint_id);
    if (it == checkpoints_.end()) return false;

    const Checkpoint& cp = it->second;
    total_events_processed_ = cp.total_events_processed;
    total_trades_ = cp.total_trades;
    total_volume_ = cp.total_volume;
    order_book_.restoreOrders(cp.active_orders);
    candle_aggregator_.restoreState(cp.completed_candles, cp.has_active_candle, cp.active_candle);
    volatility_.restoreState(volatility_.getWindowSize(), cp.volatility_prices);
    trace_logger_.restoreState(cp.trace_records);
    event_generator_.restoreState(cp.generator_state);

    processed_seq_nums_.clear();
    for (const auto& order : cp.active_orders) {
        if (order.seq_num != 0) processed_seq_nums_.insert(order.seq_num);
    }
    last_seq_ = 0;
    for (const auto& order : cp.active_orders) {
        if (order.seq_num > last_seq_) last_seq_ = order.seq_num;
    }
    event_log_.clear();
    rejected_count_ = 0;
    gaps_detected_ = 0;
    return true;
}

bool Engine::hasCheckpoint(uint64_t checkpoint_id) const {
    return checkpoints_.count(checkpoint_id) > 0;
}

std::vector<uint64_t> Engine::listCheckpoints() const {
    std::vector<uint64_t> ids;
    ids.reserve(checkpoints_.size());
    for (const auto& [id, _] : checkpoints_) ids.push_back(id);
    std::sort(ids.begin(), ids.end());
    return ids;
}

int64_t Engine::computeMidpointUpTo(size_t index) const {
    // Rebuild the best bid/ask by scanning the event log up to index. Used for
    // arrival-mid evidence in experiments; experiments are bounded so O(n) is
    // acceptable and deterministic.
    OrderBook replay_book;
    for (size_t i = 0; i < index && i < event_log_.size(); ++i) {
        const MarketEvent& ev = event_log_[i];
        switch (ev.event_type) {
            case EventType::ADD:
            case EventType::SNAPSHOT:
                replay_book.addOrder(ev);
                break;
            case EventType::CANCEL:
                replay_book.cancelOrder(ev);
                break;
            case EventType::MODIFY:
                replay_book.modifyOrder(ev);
                break;
            case EventType::EXECUTE: {
                uint64_t filled = 0;
                replay_book.executeOrder(ev, filled);
                break;
            }
        }
    }
    const double mid = replay_book.getMidpoint();
    return static_cast<int64_t>(std::llround(mid * 100.0));
}

ExperimentResult Engine::runExperiment(const ExperimentConfig& config) {
    const uint64_t t_start = nowNs();

    ExperimentResult result;
    result.config = config;
    result.arrival_mid_ticks = computeMidpointUpTo(
        config.start_seq == 0 ? 0 : std::min<size_t>(config.start_seq, event_log_.size()));

    const Side side = stringToSide(config.side);
    const bool is_buy = (side == Side::BUY);

    uint64_t remaining = config.qty_units;
    uint64_t last_seq_seen = 0;
    uint64_t prev_seq = 0;
    bool first = true;

    const size_t begin_idx = std::min<size_t>(config.start_seq, event_log_.size());
    const size_t end_idx = config.end_seq == 0
        ? event_log_.size()
        : std::min<size_t>(config.end_seq, event_log_.size());

    for (size_t i = begin_idx; i < end_idx && remaining > 0; ++i) {
        const MarketEvent& ev = event_log_[i];
        ++result.events_evaluated;

        // Track sequence gaps in the evaluated window.
        if (ev.seq_num != 0) {
            if (!first && ev.seq_num > prev_seq + 1) ++result.gaps_detected;
            prev_seq = ev.seq_num;
            first = false;
            last_seq_seen = ev.seq_num;
        }

        // A resting ask can fill our BUY; a resting bid can fill our SELL.
        const bool opposite = is_buy ? (ev.side == Side::SELL) : (ev.side == Side::BUY);
        if (!opposite) continue;
        if (ev.event_type != EventType::ADD && ev.event_type != EventType::SNAPSHOT) continue;

        const bool price_ok = is_buy
            ? (config.limit_price_ticks == 0 || ev.price_ticks <= config.limit_price_ticks)
            : (config.limit_price_ticks == 0 || ev.price_ticks >= config.limit_price_ticks);
        if (!price_ok) continue;

        const uint64_t take = std::min(remaining, ev.qty_units);
        if (take == 0) continue;

        FillRecord fr;
        fr.timestamp = ev.timestamp;
        fr.seq_num = ev.seq_num;
        fr.price_ticks = ev.price_ticks;
        fr.qty_units = take;

        int64_t notional_ticks = 0;
        if (!safeMul(ev.price_ticks, static_cast<int64_t>(take), notional_ticks)) {
            // Overflow-safe: mark as error evidence, stop the experiment.
            break;
        }
        fr.notional = static_cast<double>(notional_ticks) / (100.0 * 100.0);
        result.total_notional += fr.notional;
        result.fills.push_back(fr);
        result.filled_qty += take;
        remaining -= take;
        ++result.levels_consumed;
    }

    result.unfilled_qty = remaining;

    if (result.filled_qty > 0) {
        result.avg_fill_price = result.total_notional / static_cast<double>(result.filled_qty);
        // Fee model: 5 bps, documented and explicit.
        result.fees = result.total_notional * 0.0005;

        if (result.arrival_mid_ticks > 0) {
            const double arrival_mid = ticksToPrice(result.arrival_mid_ticks);
            if (is_buy) {
                result.slippage_vs_arrival_mid = result.avg_fill_price - arrival_mid;
            } else {
                result.slippage_vs_arrival_mid = arrival_mid - result.avg_fill_price;
            }
        }
    }

    perf_.experiment_ns = nowNs() - t_start;
    return result;
}

PolicyComparisonResult Engine::compareExecutionPolicies(const ExecutionPolicyConfig& config) {
    const uint64_t t_start = nowNs();
    PolicyComparisonResult res = ExecutionEngine::comparePolicies(event_log_, config);
    perf_.experiment_ns = nowNs() - t_start;
    return res;
}

StrategyPerformanceResult Engine::runStrategyBacktest(const MicrostructureStrategyConfig& config) {
    const uint64_t t_start = nowNs();
    StrategyPerformanceResult res = MicrostructureStrategyEngine::runBacktest(event_log_, config);
    perf_.experiment_ns = nowNs() - t_start;
    return res;
}

PerfStats Engine::getPerfStats() const {
    PerfStats p = perf_;
    p.book_update_ns = book_ns_accum_;
    p.candles_ns = candle_ns_accum_;
    return p;
}

// ---------- JSON serialization of the new structs ----------

std::string ExperimentResult::toJson() const {
    std::ostringstream json;
    json << "{";
    json << "\"side\":\"" << config.side << "\",";
    json << "\"limit_price_ticks\":" << config.limit_price_ticks << ",";
    json << "\"qty_units\":" << config.qty_units << ",";
    json << "\"start_seq\":" << config.start_seq << ",";
    json << "\"end_seq\":" << config.end_seq << ",";
    json << "\"filled_qty\":" << filled_qty << ",";
    json << "\"unfilled_qty\":" << unfilled_qty << ",";
    json << "\"avg_fill_price\":" << avg_fill_price << ",";
    json << "\"total_notional\":" << total_notional << ",";
    json << "\"fees\":" << fees << ",";
    json << "\"slippage_vs_arrival_mid\":" << slippage_vs_arrival_mid << ",";
    json << "\"arrival_mid_ticks\":" << arrival_mid_ticks << ",";
    json << "\"levels_consumed\":" << levels_consumed << ",";
    json << "\"events_evaluated\":" << events_evaluated << ",";
    json << "\"gaps_detected\":" << gaps_detected << ",";
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

std::string PerfStats::toJson() const {
    std::ostringstream json;
    json << "{";
    json << "\"events_processed\":" << events_processed << ",";
    json << "\"replay_ns\":" << replay_ns << ",";
    json << "\"book_update_ns\":" << book_update_ns << ",";
    json << "\"candles_ns\":" << candles_ns << ",";
    json << "\"experiment_ns\":" << experiment_ns << ",";
    json << "\"peak_events_per_sec\":" << peak_events_per_sec << ",";
    json << "\"rss_bytes\":" << rss_bytes;
    json << "}";
    return json.str();
}

std::string StepResult::toJson() const {
    std::ostringstream json;
    json << "{";
    json << "\"requested_count\":" << requested_count << ",";
    json << "\"processed_count\":" << processed_count << ",";
    json << "\"valid_count\":" << valid_count << ",";
    json << "\"rejected_count\":" << rejected_count << ",";
    json << "\"total_trades\":" << total_trades << ",";
    json << "\"total_volume\":" << total_volume << ",";
    json << "\"best_bid\":" << current_book_summary.best_bid << ",";
    json << "\"best_ask\":" << current_book_summary.best_ask << ",";
    json << "\"spread\":" << current_book_summary.spread << ",";
    json << "\"midpoint\":" << current_book_summary.midpoint;
    json << "}";
    return json.str();
}

} // namespace queueglass
