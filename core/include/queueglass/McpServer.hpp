#pragma once

#include <string>
#include <vector>
#include <unordered_map>
#include <memory>
#include <cstdint>

namespace queueglass {

class Engine;

struct McpToolDefinition {
    std::string name;
    std::string description;
    std::string input_schema_json;

    std::string toJson() const;
};

enum class McpJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
};

inline std::string mcpJobStatusToString(McpJobStatus status) {
    switch (status) {
        case McpJobStatus::PENDING: return "PENDING";
        case McpJobStatus::RUNNING: return "RUNNING";
        case McpJobStatus::COMPLETED: return "COMPLETED";
        case McpJobStatus::FAILED: return "FAILED";
        case McpJobStatus::CANCELLED: return "CANCELLED";
    }
    return "UNKNOWN";
}

struct McpJob {
    std::string job_id;
    std::string tool_name;
    std::string params_json;
    McpJobStatus status{McpJobStatus::PENDING};
    uint32_t progress_pct{0};
    uint64_t created_at_ms{0};
    uint64_t completed_at_ms{0};
    std::string result_json;
    std::string error_message;

    std::string toJson() const;
};

struct McpAuditLogEntry {
    uint64_t timestamp_ms{0};
    std::string client_id;
    std::string tool_name;
    std::string request_id;
    std::string status;
    double duration_ms{0.0};

    std::string toJson() const;
};

class McpServer {
public:
    explicit McpServer();

    // MCP Protocol Handler (handles JSON-RPC stdio or streamable HTTP request)
    std::string handleMcpProtocolRequest(const std::string& transport, const std::string& request_json, Engine& engine);

    // Direct Tool Dispatch
    std::string callTool(const std::string& name, const std::string& arguments_json, Engine& engine);

    // Job Management
    std::string submitJob(const std::string& tool_name, const std::string& params_json, Engine& engine);
    McpJob getJob(const std::string& job_id) const;
    std::string getJobStatusJson(const std::string& job_id) const;
    bool cancelJob(const std::string& job_id);

    // Schema & Tool Metadata
    std::string listToolsJson() const;
    std::string getProtocolVersion() const { return protocol_version_; }
    std::string getAuditLogsJson(size_t limit = 50) const;

private:
    std::string protocol_version_{"2024-11-05"};
    std::unordered_map<std::string, McpToolDefinition> tools_;
    std::unordered_map<std::string, McpJob> jobs_;
    std::vector<McpAuditLogEntry> audit_logs_;
    uint64_t next_job_id_{1001};

    void registerTools();
    void logAudit(const std::string& client_id, const std::string& tool, const std::string& req_id, const std::string& status, double duration_ms);

    // Bounded metadata helper
    std::string buildResultMetadataJson(const std::string& dataset_id, const std::string& run_id, uint64_t start_seq, uint64_t end_seq) const;
};

} // namespace queueglass
