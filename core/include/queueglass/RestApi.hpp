#pragma once

#include <string>
#include <vector>
#include <unordered_map>
#include <memory>
#include <cstdint>

namespace queueglass {

class Engine;
class IntegrationHub;
class McpServer;

struct RestRequest {
    std::string method;         // "GET", "POST", etc.
    std::string path;           // e.g. "/health", "/datasets/DS-1/capabilities"
    std::string request_id;     // "req_..."
    std::string idempotency_key;// "X-Idempotency-Key" header value
    std::string body_json;
    std::unordered_map<std::string, std::string> params;
    int page{1};
    int limit{20};
};

struct RestResponse {
    int status_code{200};
    std::string request_id;
    std::string idempotency_key;
    std::string body_json;

    std::string toJson() const;
};

struct StreamingEvent {
    std::string event_type;  // "job.progress", "job.completed", "job.failed", "integration.status_changed", "research.evidence_ready"
    uint64_t timestamp_ms{0};
    std::string payload_json;

    std::string toSseString() const;
};

class RestApiService {
public:
    explicit RestApiService();

    // Central HTTP Request Router covering all 16 required endpoints
    RestResponse handleHttpRequest(const RestRequest& req, Engine& engine, IntegrationHub& hub, McpServer& mcp);

    // OpenAPI Specification Generator
    std::string getOpenApiSpecJson() const;

    // Streaming Event Queue
    void pushStreamingEvent(const std::string& event_type, const std::string& payload_json);
    std::vector<StreamingEvent> pollStreamingEvents();

private:
    std::vector<StreamingEvent> pending_events_;
    uint64_t request_counter_{100};

    RestResponse buildErrorResponse(int status, const std::string& req_id, const std::string& code, const std::string& message) const;
    std::string generateRequestId();
};

} // namespace queueglass
