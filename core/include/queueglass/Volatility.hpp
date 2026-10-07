#pragma once

#include <cstdint>
#include <vector>
#include <deque>
#include <string>

namespace queueglass {

class RollingVolatility {
public:
    explicit RollingVolatility(size_t window_size = 20) : window_size_(window_size) {}

    void addPrice(int64_t price);
    void reset();

    double getVolatility() const;
    size_t getWindowSize() const { return window_size_; }
    size_t getSampleCount() const { return prices_.size(); }

    // State export/import for Checkpoint
    std::vector<int64_t> getPrices() const;
    void restoreState(size_t window_size, const std::vector<int64_t>& prices);

private:
    size_t window_size_{20};
    std::deque<int64_t> prices_;
};

} // namespace queueglass
