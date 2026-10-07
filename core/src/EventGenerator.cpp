#include "queueglass/EventGenerator.hpp"
#include <algorithm>
#include <cstring>
#include <sstream>

namespace queueglass {

void EventGenerator::reset(uint64_t seed) {
    seed_ = seed;
    rng_.seed(seed);
    mid_ = base_price_;
    current_seq_num_ = 1;
    current_timestamp_ = 1000000000;
    next_order_id_ = 1;
    active_orders_.clear();
    active_order_ids_.clear();
}

MarketEvent EventGenerator::generateEvent() {
    std::uniform_int_distribution<int> action_dist(0, 99);

    MarketEvent ev;
    ev.timestamp = current_timestamp_;
    ev.seq_num = current_seq_num_;
    current_timestamp_ += 1 + static_cast<int64_t>(rng_() % 5);   // 1-5 ms per event
    ++current_seq_num_;

    // Choose an action: mostly adds, some cancels and executions of resting
    // synthetic orders, plus a rare modify. Mirrors plausible activity mix.
    const int action = action_dist(rng_);

    if (!active_orders_.empty() && action >= 60 && action < 75) {
        // CANCEL a random resting synthetic order (partial or full).
        std::uniform_int_distribution<size_t> pick(0, active_order_ids_.size() - 1);
        const uint64_t victim_id = active_order_ids_[pick(rng_)];
        auto it = active_orders_.find(victim_id);
        if (it == active_orders_.end()) return ev; // unreachable, defensive

        ev.order_id = victim_id;
        ev.event_type = EventType::CANCEL;
        ev.side = it->second.side;
        ev.price_ticks = it->second.price_ticks;
        // 50% full cancel, 50% partial.
        if (rng_() % 2 == 0) {
            ev.qty_units = it->second.remaining_qty;
        } else {
            ev.qty_units = 1 + it->second.remaining_qty / 2;
        }

        it->second.remaining_qty -= ev.qty_units;
        if (it->second.remaining_qty == 0) {
            removeSyntheticOrder(victim_id);
        }
        return ev;
    }

    if (!active_orders_.empty() && action >= 75 && action < 92) {
        // EXECUTE (trade) against a random resting synthetic order.
        std::uniform_int_distribution<size_t> pick(0, active_order_ids_.size() - 1);
        const uint64_t victim_id = active_order_ids_[pick(rng_)];
        auto it = active_orders_.find(victim_id);
        if (it == active_orders_.end()) return ev;

        ev.order_id = victim_id;
        ev.event_type = EventType::EXECUTE;
        ev.side = it->second.side;
        ev.price_ticks = it->second.price_ticks;
        ev.qty_units = std::min<uint64_t>(it->second.remaining_qty, 1 + rng_() % 50);

        it->second.remaining_qty -= ev.qty_units;
        if (it->second.remaining_qty == 0) {
            removeSyntheticOrder(victim_id);
        }
        return ev;
    }

    if (!active_orders_.empty() && action >= 92 && action < 95) {
        // MODIFY: move a resting order to a new nearby price.
        std::uniform_int_distribution<size_t> pick(0, active_order_ids_.size() - 1);
        const uint64_t victim_id = active_order_ids_[pick(rng_)];
        auto it = active_orders_.find(victim_id);
        if (it == active_orders_.end()) return ev;

        ev.order_id = victim_id;
        ev.event_type = EventType::MODIFY;
        ev.side = it->second.side;

        const int64_t drift = static_cast<int64_t>(rng_() % 11) - 5; // -5..+5 ticks
        int64_t new_price = it->second.price_ticks + drift;
        if (new_price < 1) new_price = 1;
        ev.price_ticks = new_price;
        ev.qty_units = it->second.remaining_qty;

        removeSyntheticOrder(victim_id);
        active_orders_[ev.order_id] = SyntheticOrderInfo{ev.order_id, ev.side, ev.price_ticks, ev.qty_units};
        active_order_ids_.push_back(ev.order_id);
        return ev;
    }

    // Default: ADD a new resting order near the current mid.
    ev.order_id = next_order_id_++;
    ev.event_type = EventType::ADD;
    ev.side = (rng_() % 2 == 0) ? Side::BUY : Side::SELL;

    // Random walk of the mid so the market trends and mean-reverts.
    const int64_t walk = static_cast<int64_t>(rng_() % 13) - 6; // -6..+6 ticks
    mid_ += walk;
    if (mid_ < base_price_ / 10) mid_ = base_price_ / 10;
    if (mid_ > base_price_ * 4) mid_ = base_price_ * 4;

    // Offset from mid: buys below, asks above; occasionally crosses to make the
    // book interesting.
    const int64_t offset = static_cast<int64_t>(rng_() % 31) + 1; // 1..30 ticks
    ev.price_ticks = (ev.side == Side::BUY) ? mid_ - offset : mid_ + offset;
    if (ev.price_ticks < 1) ev.price_ticks = 1;

    ev.qty_units = 10 + rng_() % 500;
    active_orders_[ev.order_id] = SyntheticOrderInfo{ev.order_id, ev.side, ev.price_ticks, ev.qty_units};
    // The id must be registered in the parallel index vector; every
    // cancel/execute/modify branch selects its victim from this vector.
    active_order_ids_.push_back(ev.order_id);
    return ev;
}

std::vector<MarketEvent> EventGenerator::generateBatch(size_t count) {
    std::vector<MarketEvent> batch;
    batch.reserve(count);
    for (size_t i = 0; i < count; ++i) batch.push_back(generateEvent());
    return batch;
}

// Removes an order from both the lookup map and the id-selection vector.
// The two containers must stay consistent: branches above guard on
// `!active_orders_.empty()` but index into `active_order_ids_`, so any
// divergence causes an out-of-bounds access (SIGSEGV).
void EventGenerator::removeSyntheticOrder(uint64_t order_id) {
    active_orders_.erase(order_id);
    auto vec_it = std::find(active_order_ids_.begin(), active_order_ids_.end(), order_id);
    if (vec_it != active_order_ids_.end()) active_order_ids_.erase(vec_it);
}

EventGenerator::GeneratorState EventGenerator::getState() const {
    GeneratorState s;
    s.seed = seed_;
    s.seq_num = current_seq_num_;
    s.timestamp = current_timestamp_;
    s.mid = mid_;
    s.next_order_id = next_order_id_;
    s.rng_state_str = "";

    std::ostringstream os;
    os << rng_;
    s.rng_state_str = os.str();

    for (const auto& [id, info] : active_orders_) {
        s.active_synthetic_orders.push_back(info);
    }
    return s;
}

void EventGenerator::restoreState(const GeneratorState& state) {
    seed_ = state.seed;
    current_seq_num_ = state.seq_num;
    current_timestamp_ = state.timestamp;
    mid_ = state.mid;
    next_order_id_ = state.next_order_id;
    active_orders_.clear();
    active_order_ids_.clear();

    std::istringstream is(state.rng_state_str);
    if (state.rng_state_str.empty()) {
        rng_.seed(seed_);
    } else {
        is >> rng_;
    }

    for (const auto& info : state.active_synthetic_orders) {
        active_orders_[info.order_id] = info;
        active_order_ids_.push_back(info.order_id);
    }
}

} // namespace queueglass
