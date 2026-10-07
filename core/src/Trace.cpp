#include "queueglass/Trace.hpp"
#include <sstream>

namespace queueglass {

std::string TraceRecord::toJson() const {
    std::ostringstream json;
    json << "{";
    json << "\"event\":" << event.toJson() << ",";
    json << "\"valid\":" << (valid ? "true" : "false") << ",";
    json << "\"code\":\"" << validationCodeToString(code) << "\",";
    json << "\"message\":\"" << message << "\",";
    json << "\"best_bid\":" << book_state.best_bid << ",";
    json << "\"best_ask\":" << book_state.best_ask << ",";
    json << "\"spread\":" << book_state.spread << ",";
    json << "\"midpoint\":" << book_state.midpoint;
    json << "}";
    return json.str();
}

void TraceLogger::log(const MarketEvent& event, bool valid, ValidationCode code,
                      const std::string& msg, const BookSummary& summary) {
    TraceRecord rec;
    rec.event = event;
    rec.valid = valid;
    rec.code = code;
    rec.message = msg;
    rec.book_state = summary;
    records_.push_back(std::move(rec));
    while (records_.size() > max_records_) records_.erase(records_.begin());
}

void TraceLogger::clear() {
    records_.clear();
}

std::vector<TraceRecord> TraceLogger::getRecords(size_t max_lines) const {
    if (records_.size() <= max_lines) return records_;
    return std::vector<TraceRecord>(records_.end() - static_cast<long>(max_lines), records_.end());
}

std::string TraceLogger::toJson(size_t max_lines) const {
    auto recs = getRecords(max_lines);
    std::ostringstream json;
    json << "[";
    for (size_t i = 0; i < recs.size(); ++i) {
        if (i > 0) json << ",";
        json << recs[i].toJson();
    }
    json << "]";
    return json.str();
}

void TraceLogger::restoreState(const std::vector<TraceRecord>& records) {
    records_ = records;
    while (records_.size() > max_records_) records_.erase(records_.begin());
}

} // namespace queueglass
