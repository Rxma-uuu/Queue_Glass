#pragma once

#include "Types.hpp"
#include "MarketEvent.hpp"
#include <random>
#include <unordered_map>
#include <vector>

namespace queueglass {

struct SyntheticOrderInfo {
    uint64_t order_id;
    Side side;
    int64_t price_ticks;
    uint64_t remaining_qty;
};

class EventGenerator {
public:
    explicit EventGenerator(uint64_t seed = 42, int64_t base_price = 10000)
        : seed_(seed), base_price_(base_price), rng_(seed) {
        reset(seed);
    }

    void reset(uint64_t seed);
    MarketEvent generateEvent();
    std::vector<MarketEvent> generateBatch(size_t count);

    uint64_t getSeed() const { return seed_; }
    uint64_t getNextSeqNum() const { return current_seq_num_; }

    // State for checkpoints
    struct GeneratorState {
        uint64_t seed;
        uint64_t seq_num;
        int64_t timestamp;
        int64_t mid;
        uint64_t next_order_id;
        std::vector<SyntheticOrderInfo> active_synthetic_orders;
        std::string rng_state_str; // serialized std::mt19937_64 state
    };

    GeneratorState getState() const;
    void restoreState(const GeneratorState& state);

private:
    uint64_t seed_{42};
    int64_t base_price_{10000};
    std::mt19937_64 rng_;

    int64_t mid_{10000};            // current simulated mid price (ticks)
    uint64_t current_seq_num_{1};
    int64_t current_timestamp_{1000000000};
    uint64_t next_order_id_{1};

    std::unordered_map<uint64_t, SyntheticOrderInfo> active_orders_;
    std::vector<uint64_t> active_order_ids_;

    void removeSyntheticOrder(uint64_t order_id);
};

} // namespace queueglass
