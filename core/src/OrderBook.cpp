#include "queueglass/OrderBook.hpp"
#include <algorithm>
#include <numeric>

namespace queueglass {

ValidationCode OrderBook::addOrder(const MarketEvent& event) {
    if (orders_index_.find(event.order_id) != orders_index_.end()) {
        return ValidationCode::DUPLICATE_ORDER_ID;
    }
    if (event.price_ticks <= 0) {
        return ValidationCode::INVALID_PRICE;
    }
    if (event.qty_units == 0) {
        return ValidationCode::INVALID_QUANTITY;
    }

    Order order{
        event.order_id,
        event.side,
        event.price_ticks,
        event.qty_units,
        event.qty_units,
        event.timestamp,
        event.seq_num
    };

    if (event.side == Side::BUY) {
        auto& list = bids_[event.price_ticks];
        list.push_back(order);
        auto it = --list.end();
        orders_index_[event.order_id] = {event.price_ticks, Side::BUY, it};
    } else {
        auto& list = asks_[event.price_ticks];
        list.push_back(order);
        auto it = --list.end();
        orders_index_[event.order_id] = {event.price_ticks, Side::SELL, it};
    }

    return ValidationCode::OK;
}

ValidationCode OrderBook::cancelOrder(const MarketEvent& event) {
    auto it = orders_index_.find(event.order_id);
    if (it == orders_index_.end()) {
        return ValidationCode::ORDER_NOT_FOUND;
    }

    OrderLocation& loc = it->second;
    Order& order = *(loc.iterator);

    uint64_t cancel_qty = (event.qty_units == 0) ? order.remaining_qty : event.qty_units;

    if (cancel_qty > order.remaining_qty) {
        return ValidationCode::OVER_CANCEL;
    }

    order.remaining_qty -= cancel_qty;

    if (order.remaining_qty == 0) {
        removeOrderInternal(event.order_id);
    }

    return ValidationCode::OK;
}

ValidationCode OrderBook::modifyOrder(const MarketEvent& event) {
    auto it = orders_index_.find(event.order_id);
    if (it == orders_index_.end()) {
        return ValidationCode::ORDER_NOT_FOUND;
    }

    OrderLocation loc = it->second;
    Order& order = *(loc.iterator);

    if (event.price_ticks <= 0) {
        return ValidationCode::INVALID_PRICE;
    }
    if (event.qty_units == 0) {
        return ValidationCode::INVALID_QUANTITY;
    }

    // If price or side changed, or quantity increased, price-time priority resets
    if (event.price_ticks != order.price_ticks || event.side != order.side || event.qty_units > order.remaining_qty) {
        removeOrderInternal(event.order_id);

        MarketEvent add_event = event;
        add_event.event_type = EventType::ADD;
        add_event.qty_units = event.qty_units;
        return addOrder(add_event);
    } else {
        // Quantity decreased, keep priority
        order.remaining_qty = event.qty_units;
        return ValidationCode::OK;
    }
}

ValidationCode OrderBook::executeOrder(const MarketEvent& event, uint64_t& executed_qty) {
    executed_qty = 0;
    auto it = orders_index_.find(event.order_id);
    if (it == orders_index_.end()) {
        return ValidationCode::ORDER_NOT_FOUND;
    }

    OrderLocation& loc = it->second;
    Order& order = *(loc.iterator);

    if (event.qty_units > order.remaining_qty) {
        return ValidationCode::OVER_EXECUTE;
    }

    executed_qty = event.qty_units;
    order.remaining_qty -= event.qty_units;

    if (order.remaining_qty == 0) {
        removeOrderInternal(event.order_id);
    }

    return ValidationCode::OK;
}

void OrderBook::removeOrderInternal(uint64_t order_id) {
    auto it = orders_index_.find(order_id);
    if (it == orders_index_.end()) return;

    const OrderLocation& loc = it->second;
    if (loc.side == Side::BUY) {
        auto list_it = bids_.find(loc.price_ticks);
        if (list_it != bids_.end()) {
            list_it->second.erase(loc.iterator);
            if (list_it->second.empty()) {
                bids_.erase(list_it);
            }
        }
    } else {
        auto list_it = asks_.find(loc.price_ticks);
        if (list_it != asks_.end()) {
            list_it->second.erase(loc.iterator);
            if (list_it->second.empty()) {
                asks_.erase(list_it);
            }
        }
    }

    orders_index_.erase(it);
}

void OrderBook::clear() {
    bids_.clear();
    asks_.clear();
    orders_index_.clear();
}

bool OrderBook::hasOrder(uint64_t order_id) const {
    return orders_index_.find(order_id) != orders_index_.end();
}

const Order* OrderBook::getOrder(uint64_t order_id) const {
    auto it = orders_index_.find(order_id);
    if (it == orders_index_.end()) return nullptr;
    return &(*(it->second.iterator));
}

int64_t OrderBook::getBestBid() const {
    if (bids_.empty()) return 0;
    return bids_.begin()->first;
}

int64_t OrderBook::getBestAsk() const {
    if (asks_.empty()) return 0;
    return asks_.begin()->first;
}

int64_t OrderBook::getSpread() const {
    int64_t bb = getBestBid();
    int64_t ba = getBestAsk();
    if (bb > 0 && ba > 0) {
        return ba - bb;
    }
    return 0;
}

double OrderBook::getMidpoint() const {
    int64_t bb = getBestBid();
    int64_t ba = getBestAsk();
    if (bb > 0 && ba > 0) {
        return (static_cast<double>(bb) + ba) / 2.0;
    }
    if (bb > 0) return static_cast<double>(bb);
    if (ba > 0) return static_cast<double>(ba);
    return 0.0;
}

uint64_t OrderBook::getBidDepth(size_t levels) const {
    uint64_t depth = 0;
    size_t count = 0;
    for (const auto& [price, list] : bids_) {
        if (count >= levels) break;
        for (const auto& order : list) {
            depth += order.remaining_qty;
        }
        count++;
    }
    return depth;
}

uint64_t OrderBook::getAskDepth(size_t levels) const {
    uint64_t depth = 0;
    size_t count = 0;
    for (const auto& [price, list] : asks_) {
        if (count >= levels) break;
        for (const auto& order : list) {
            depth += order.remaining_qty;
        }
        count++;
    }
    return depth;
}

double OrderBook::getDepthImbalance(size_t levels) const {
    uint64_t bd = getBidDepth(levels);
    uint64_t ad = getAskDepth(levels);
    uint64_t total = bd + ad;
    if (total == 0) return 0.0;
    return (static_cast<double>(bd) - static_cast<double>(ad)) / static_cast<double>(total);
}

std::vector<LevelSummary> OrderBook::getBidLevels(size_t max_levels) const {
    std::vector<LevelSummary> res;
    size_t count = 0;
    for (const auto& [price, list] : bids_) {
        if (count >= max_levels) break;
        uint64_t qty = 0;
        for (const auto& o : list) qty += o.remaining_qty;
        res.push_back({price, qty, static_cast<uint32_t>(list.size())});
        count++;
    }
    return res;
}

std::vector<LevelSummary> OrderBook::getAskLevels(size_t max_levels) const {
    std::vector<LevelSummary> res;
    size_t count = 0;
    for (const auto& [price, list] : asks_) {
        if (count >= max_levels) break;
        uint64_t qty = 0;
        for (const auto& o : list) qty += o.remaining_qty;
        res.push_back({price, qty, static_cast<uint32_t>(list.size())});
        count++;
    }
    return res;
}

BookSummary OrderBook::getSummary(size_t depth_levels) const {
    BookSummary s;
    s.best_bid = getBestBid();
    s.best_ask = getBestAsk();
    s.spread = getSpread();
    s.midpoint = getMidpoint();
    s.bid_depth_5 = getBidDepth(depth_levels);
    s.ask_depth_5 = getAskDepth(depth_levels);
    s.depth_imbalance = getDepthImbalance(depth_levels);

    uint32_t bid_orders = 0;
    for (const auto& [price, list] : bids_) bid_orders += static_cast<uint32_t>(list.size());
    s.active_bid_orders = bid_orders;

    uint32_t ask_orders = 0;
    for (const auto& [price, list] : asks_) ask_orders += static_cast<uint32_t>(list.size());
    s.active_ask_orders = ask_orders;

    return s;
}

std::string OrderBook::toJson(size_t depth_levels) const {
    BookSummary s = getSummary(depth_levels);
    auto bid_lvls = getBidLevels(depth_levels);
    auto ask_lvls = getAskLevels(depth_levels);

    std::string json = "{";
    json += "\"best_bid\":" + std::to_string(s.best_bid) + ",";
    json += "\"best_ask\":" + std::to_string(s.best_ask) + ",";
    json += "\"spread\":" + std::to_string(s.spread) + ",";
    json += "\"midpoint\":" + std::to_string(s.midpoint) + ",";
    json += "\"bid_depth_5\":" + std::to_string(s.bid_depth_5) + ",";
    json += "\"ask_depth_5\":" + std::to_string(s.ask_depth_5) + ",";
    json += "\"depth_imbalance\":" + std::to_string(s.depth_imbalance) + ",";
    json += "\"active_bid_orders\":" + std::to_string(s.active_bid_orders) + ",";
    json += "\"active_ask_orders\":" + std::to_string(s.active_ask_orders) + ",";

    json += "\"bids\":[";
    for (size_t i = 0; i < bid_lvls.size(); ++i) {
        if (i > 0) json += ",";
        json += "{\"price\":" + std::to_string(bid_lvls[i].price_ticks) +
                ",\"qty\":" + std::to_string(bid_lvls[i].total_qty) +
                ",\"orders\":" + std::to_string(bid_lvls[i].order_count) + "}";
    }
    json += "],\"asks\":[";
    for (size_t i = 0; i < ask_lvls.size(); ++i) {
        if (i > 0) json += ",";
        json += "{\"price\":" + std::to_string(ask_lvls[i].price_ticks) +
                ",\"qty\":" + std::to_string(ask_lvls[i].total_qty) +
                ",\"orders\":" + std::to_string(ask_lvls[i].order_count) + "}";
    }
    json += "]}";
    return json;
}

std::vector<Order> OrderBook::getAllActiveOrders() const {
    std::vector<Order> res;
    res.reserve(orders_index_.size());
    for (const auto& [price, list] : bids_) {
        for (const auto& o : list) res.push_back(o);
    }
    for (const auto& [price, list] : asks_) {
        for (const auto& o : list) res.push_back(o);
    }
    return res;
}

void OrderBook::restoreOrders(const std::vector<Order>& orders) {
    clear();
    for (const auto& order : orders) {
        MarketEvent event{
            order.timestamp,
            order.seq_num,
            order.order_id,
            EventType::ADD,
            order.side,
            order.price_ticks,
            order.remaining_qty
        };
        addOrder(event);
    }
}

} // namespace queueglass
