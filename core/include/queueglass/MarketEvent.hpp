#pragma once

#include "Types.hpp"
#include <string>

namespace queueglass {

struct MarketEvent {
    int64_t timestamp{0};
    uint64_t seq_num{0};
    uint64_t order_id{0};
    EventType event_type{EventType::ADD};
    Side side{Side::BUY};
    int64_t price_ticks{0};
    uint64_t qty_units{0};

    std::string toJson() const {
        std::string json = "{";
        json += "\"timestamp\":" + std::to_string(timestamp) + ",";
        json += "\"seq_num\":" + std::to_string(seq_num) + ",";
        json += "\"order_id\":" + std::to_string(order_id) + ",";
        json += "\"event_type\":\"" + eventTypeToString(event_type) + "\",";
        json += "\"side\":\"" + sideToString(side) + "\",";
        json += "\"price_ticks\":" + std::to_string(price_ticks) + ",";
        json += "\"qty_units\":" + std::to_string(qty_units);
        json += "}";
        return json;
    }
};

} // namespace queueglass
