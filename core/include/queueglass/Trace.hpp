#pragma once

#include "Types.hpp"
#include "MarketEvent.hpp"
#include "OrderBook.hpp"
#include <vector>
#include <string>

namespace queueglass {

struct TraceRecord {
    MarketEvent event;
    bool valid{true};
    ValidationCode code{ValidationCode::OK};
    std::string message;
    BookSummary book_state;

    std::string toJson() const;
};

class TraceLogger {
public:
    explicit TraceLogger(size_t max_records = 10000) : max_records_(max_records) {}

    void log(const MarketEvent& event, bool valid, ValidationCode code, const std::string& msg, const BookSummary& summary);
    void clear();

    std::vector<TraceRecord> getRecords(size_t max_lines = 100) const;
    std::string toJson(size_t max_lines = 100) const;
    size_t maxRecords() const { return max_records_; }

    // State export/import for Checkpoint
    void restoreState(const std::vector<TraceRecord>& records);

private:
    size_t max_records_{10000};
    std::vector<TraceRecord> records_;
};

} // namespace queueglass
