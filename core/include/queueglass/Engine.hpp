#pragma once

#include "Types.hpp"
#include "MarketEvent.hpp"
#include "OrderBook.hpp"
#include "Candle.hpp"
#include "Volatility.hpp"
#include "Trace.hpp"
#include "Checkpoint.hpp"
#include "EventGenerator.hpp"
#include "Experiment.hpp"
#include "ExecutionEngine.hpp"
#include "StrategyModule.hpp"
#include <map>
#include <unordered_map>
#include <unordered_set>
#include <memory>
#include <string>

namespace queueglass {

struct StepResult {
    size_t requested_count{0};
    size_t processed_count{0};
    size_t valid_count{0};
    size_t rejected_count{0};
    uint64_t total_trades{0};
    uint64_t total_volume{0};
    BookSummary current_book_summary{};

    std::string toJson() const;
};

class Engine {
public:
    explicit Engine(uint64_t seed = 42);

    // Core processing
    ValidationCode processEvent(const MarketEvent& event);
    StepResult stepEvents(size_t count);

    // Validation & Lifecycle
    ValidationCode validateEvent(const MarketEvent& event) const;

    // Queries
    BookSummary getBookSummary() const;
    std::string getBookSummaryJson(size_t depth_levels = 5) const;
    std::string getCandlesJson(size_t max_count = 50) const;
    // Re-aggregate the recorded trade log at an arbitrary interval (ms).
    // Deterministic: replays recorded executes through a fresh aggregator.
    std::string getCandlesJsonForInterval(int64_t interval_ms, size_t max_count = 200) const;
    std::string getEventTraceJson(size_t max_lines = 100) const;
    double currentVolatility() const { return volatility_.getVolatility(); }

    // Checkpoint operations
    uint64_t createCheckpoint();
    bool restoreCheckpoint(uint64_t checkpoint_id);
    bool hasCheckpoint(uint64_t checkpoint_id) const;
    std::vector<uint64_t> listCheckpoints() const;

    // Experiments: evaluate a hypothetical order against the replayed book.
    // Runs deterministically over the recorded event window and never mutates
    // the live book.
    ExperimentResult runExperiment(const ExperimentConfig& config);

    // Multi-policy execution evaluation
    PolicyComparisonResult compareExecutionPolicies(const ExecutionPolicyConfig& config);

    // Microstructure Strategy Backtest
    StrategyPerformanceResult runStrategyBacktest(const MicrostructureStrategyConfig& config);

    // Performance stats actually measured in-engine.
    PerfStats getPerfStats() const;

    // Direct Accessors
    const OrderBook& getOrderBook() const { return order_book_; }
    const CandleAggregator& getCandleAggregator() const { return candle_aggregator_; }
    const RollingVolatility& getVolatility() const { return volatility_; }
    const TraceLogger& getTraceLogger() const { return trace_logger_; }
    EventGenerator& getEventGenerator() { return event_generator_; }

    uint64_t getTotalEventsProcessed() const { return total_events_processed_; }
    uint64_t getTotalTrades() const { return total_trades_; }
    uint64_t getTotalVolume() const { return total_volume_; }
    uint64_t getGapsDetected() const { return gaps_detected_; }
    uint64_t getRejectedCount() const { return rejected_count_; }

private:
    uint64_t seed_{42};
    EventGenerator event_generator_;
    OrderBook order_book_;
    CandleAggregator candle_aggregator_{1000};
    RollingVolatility volatility_{20};
    TraceLogger trace_logger_{10000};

    uint64_t total_events_processed_{0};
    uint64_t total_trades_{0};
    uint64_t total_volume_{0};
    uint64_t rejected_count_{0};
    uint64_t gaps_detected_{0};
    uint64_t last_seq_{0};
    uint64_t next_checkpoint_id_{1};

    std::unordered_set<uint64_t> processed_seq_nums_;
    std::map<uint64_t, Checkpoint> checkpoints_;

    // Measured performance counters.
    PerfStats perf_{};
    uint64_t book_ns_accum_{0};
    uint64_t candle_ns_accum_{0};

    // Full event log for deterministic replay / experiments.
    std::vector<MarketEvent> event_log_;

    // Helper: mid price as of a given point in the event log.
    int64_t computeMidpointUpTo(size_t index) const;
};

} // namespace queueglass
