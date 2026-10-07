#pragma once

#include "Types.hpp"
#include "MarketEvent.hpp"
#include <map>
#include <list>
#include <unordered_map>
#include <vector>
#include <string>

namespace queueglass {

struct Order {
    uint64_t order_id{0};
    Side side{Side::BUY};
    int64_t price_ticks{0};
    uint64_t initial_qty{0};
    uint64_t remaining_qty{0};
    int64_t timestamp{0};
    uint64_t seq_num{0};
};

struct LevelSummary {
    int64_t price_ticks{0};
    uint64_t total_qty{0};
    uint32_t order_count{0};
};

struct BookSummary {
    int64_t best_bid{0};
    int64_t best_ask{0};
    int64_t spread{0};
    double midpoint{0.0};
    uint64_t bid_depth_5{0};
    uint64_t ask_depth_5{0};
    double depth_imbalance{0.0};
    uint32_t active_bid_orders{0};
    uint32_t active_ask_orders{0};
};

class OrderBook {
public:
    OrderBook() = default;

    // Operations
    ValidationCode addOrder(const MarketEvent& event);
    ValidationCode cancelOrder(const MarketEvent& event);
    ValidationCode modifyOrder(const MarketEvent& event);
    ValidationCode executeOrder(const MarketEvent& event, uint64_t& executed_qty);
    void clear();

    // Queries
    bool hasOrder(uint64_t order_id) const;
    const Order* getOrder(uint64_t order_id) const;

    int64_t getBestBid() const;
    int64_t getBestAsk() const;
    int64_t getSpread() const;
    double getMidpoint() const;

    uint64_t getBidDepth(size_t levels = 5) const;
    uint64_t getAskDepth(size_t levels = 5) const;
    double getDepthImbalance(size_t levels = 5) const;

    std::vector<LevelSummary> getBidLevels(size_t max_levels = 5) const;
    std::vector<LevelSummary> getAskLevels(size_t max_levels = 5) const;

    BookSummary getSummary(size_t depth_levels = 5) const;
    std::string toJson(size_t depth_levels = 5) const;

    // State export / import for Checkpoint
    std::vector<Order> getAllActiveOrders() const;
    void restoreOrders(const std::vector<Order>& orders);

private:
    struct OrderLocation {
        int64_t price_ticks;
        Side side;
        std::list<Order>::iterator iterator;
    };

    // Price levels:
    // Bids sorted descending
    std::map<int64_t, std::list<Order>, std::greater<int64_t>> bids_;
    // Asks sorted ascending
    std::map<int64_t, std::list<Order>, std::less<int64_t>> asks_;

    // Order lookup index
    std::unordered_map<uint64_t, OrderLocation> orders_index_;

    void removeOrderInternal(uint64_t order_id);
};

} // namespace queueglass
