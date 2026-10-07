#include "queueglass/Volatility.hpp"
#include <cmath>

namespace queueglass {

// Rolling standard deviation of log returns of trade prices.
// Sampling: population std-dev (divide by N) over the trailing window of the
// last `window_size_` trade prices, expressed in basis points of the mean
// price so it is comparable across price levels.

void RollingVolatility::addPrice(int64_t price) {
    if (price <= 0) return; // ignore invalid ticks
    prices_.push_back(price);
    while (prices_.size() > window_size_) prices_.pop_front();
}

void RollingVolatility::reset() {
    prices_.clear();
}

double RollingVolatility::getVolatility() const {
    // Need at least 2 prices to form 1 return.
    if (prices_.size() < 2) return 0.0;

    std::vector<double> log_returns;
    log_returns.reserve(prices_.size() - 1);
    for (size_t i = 1; i < prices_.size(); ++i) {
        const double prev = static_cast<double>(prices_[i - 1]);
        const double curr = static_cast<double>(prices_[i]);
        if (prev > 0.0 && curr > 0.0) {
            log_returns.push_back(std::log(curr / prev));
        }
    }
    if (log_returns.empty()) return 0.0;

    double mean = 0.0;
    for (double r : log_returns) mean += r;
    mean /= static_cast<double>(log_returns.size());

    double var = 0.0;
    for (double r : log_returns) {
        const double d = r - mean;
        var += d * d;
    }
    var /= static_cast<double>(log_returns.size()); // population variance
    const double stddev = std::sqrt(var);

    // Convert to bps relative to mean price level: stddev per trade * 10000.
    return stddev * 10000.0;
}

std::vector<int64_t> RollingVolatility::getPrices() const {
    return std::vector<int64_t>(prices_.begin(), prices_.end());
}

void RollingVolatility::restoreState(size_t window_size, const std::vector<int64_t>& prices) {
    window_size_ = window_size;
    prices_.assign(prices.begin(), prices.end());
    while (prices_.size() > window_size_) prices_.pop_front();
}

} // namespace queueglass
