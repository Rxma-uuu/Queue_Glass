#pragma once

#include <cstdint>
#include <string>
#include <climits>

namespace queueglass {

enum class EventType {
    ADD = 0,
    CANCEL = 1,
    MODIFY = 2,
    EXECUTE = 3,
    SNAPSHOT = 4
};

enum class Side {
    BUY = 0,
    SELL = 1
};

enum class OrderStatus {
    ACTIVE = 0,
    CANCELLED = 1,
    FILLED = 2,
    PARTIALLY_FILLED = 3,
    REJECTED = 4
};

enum class ValidationCode {
    OK = 0,
    INVALID_PRICE = 1,
    INVALID_QUANTITY = 2,
    DUPLICATE_SEQUENCE = 3,
    ORDER_NOT_FOUND = 4,
    INVALID_TRANSITION = 5,
    OVER_CANCEL = 6,
    OVER_EXECUTE = 7,
    ARITHMETIC_OVERFLOW = 8,
    DUPLICATE_ORDER_ID = 9
};

inline std::string eventTypeToString(EventType type) {
    switch (type) {
        case EventType::ADD: return "ADD";
        case EventType::CANCEL: return "CANCEL";
        case EventType::MODIFY: return "MODIFY";
        case EventType::EXECUTE: return "EXECUTE";
        case EventType::SNAPSHOT: return "SNAPSHOT";
    }
    return "UNKNOWN";
}

inline EventType stringToEventType(const std::string& str) {
    if (str == "ADD") return EventType::ADD;
    if (str == "CANCEL") return EventType::CANCEL;
    if (str == "MODIFY") return EventType::MODIFY;
    if (str == "EXECUTE") return EventType::EXECUTE;
    if (str == "SNAPSHOT") return EventType::SNAPSHOT;
    return EventType::ADD;
}

inline std::string sideToString(Side side) {
    return side == Side::BUY ? "BUY" : "SELL";
}

inline Side stringToSide(const std::string& str) {
    return (str == "SELL" || str == "ASK") ? Side::SELL : Side::BUY;
}

inline std::string validationCodeToString(ValidationCode code) {
    switch (code) {
        case ValidationCode::OK: return "OK";
        case ValidationCode::INVALID_PRICE: return "INVALID_PRICE";
        case ValidationCode::INVALID_QUANTITY: return "INVALID_QUANTITY";
        case ValidationCode::DUPLICATE_SEQUENCE: return "DUPLICATE_SEQUENCE";
        case ValidationCode::ORDER_NOT_FOUND: return "ORDER_NOT_FOUND";
        case ValidationCode::INVALID_TRANSITION: return "INVALID_TRANSITION";
        case ValidationCode::OVER_CANCEL: return "OVER_CANCEL";
        case ValidationCode::OVER_EXECUTE: return "OVER_EXECUTE";
        case ValidationCode::ARITHMETIC_OVERFLOW: return "ARITHMETIC_OVERFLOW";
        case ValidationCode::DUPLICATE_ORDER_ID: return "DUPLICATE_ORDER_ID";
    }
    return "UNKNOWN_ERROR";
}

// Overflow-safe integer arithmetic helpers
inline bool safeAdd(int64_t a, int64_t b, int64_t& result) {
#if defined(__GNUC__) || defined(__clang__)
    return !__builtin_add_overflow(a, b, &result);
#else
    if ((b > 0 && a > INT64_MAX - b) || (b < 0 && a < INT64_MIN - b)) return false;
    result = a + b;
    return true;
#endif
}

inline bool safeAddUint(uint64_t a, uint64_t b, uint64_t& result) {
#if defined(__GNUC__) || defined(__clang__)
    return !__builtin_add_overflow(a, b, &result);
#else
    if (a > UINT64_MAX - b) return false;
    result = a + b;
    return true;
#endif
}

inline bool safeMul(int64_t a, int64_t b, int64_t& result) {
#if defined(__GNUC__) || defined(__clang__)
    return !__builtin_mul_overflow(a, b, &result);
#else
    if (a > 0 && b > 0 && a > INT64_MAX / b) return false;
    if (a > 0 && b < 0 && b < INT64_MIN / a) return false;
    if (a < 0 && b > 0 && a < INT64_MIN / b) return false;
    if (a < 0 && b < 0 && a < INT64_MAX / b) return false;
    result = a * b;
    return true;
#endif
}

} // namespace queueglass
