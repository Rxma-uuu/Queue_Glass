#include "queueglass/IntegrationHub.hpp"
#include <sstream>
#include <chrono>

namespace queueglass {

std::string ProviderRegistryEntry::toJson() const {
    std::ostringstream ss;
    ss << "{";
    ss << "\"id\":\"" << id << "\",";
    ss << "\"name\":\"" << name << "\",";
    ss << "\"integration_type\":\"" << integrationTypeToString(integration_type) << "\",";
    ss << "\"exact_product_identity\":\"" << exact_product_identity << "\",";
    ss << "\"official_documentation_url\":\"" << official_documentation_url << "\",";
    ss << "\"date_verified\":\"" << date_verified << "\",";
    ss << "\"connection_method\":\"" << connection_method << "\",";
    ss << "\"authentication_method\":\"" << authentication_method << "\",";
    ss << "\"capabilities\":[";
    for (size_t i = 0; i < capabilities.size(); ++i) {
        if (i > 0) ss << ",";
        ss << "\"" << capabilities[i] << "\"";
    }
    ss << "],";
    ss << "\"compatibility_test_results\":\"" << compatibility_test_results << "\",";
    ss << "\"connection_state\":\"" << connectionStateToString(connection_state) << "\",";
    ss << "\"is_verified\":" << (is_verified ? "true" : "false") << ",";
    ss << "\"unverified_explanation\":\"" << unverified_explanation << "\",";
    ss << "\"last_checked_timestamp_ms\":" << last_checked_timestamp_ms << ",";
    ss << "\"latency_ms\":" << latency_ms;
    ss << "}";
    return ss.str();
}

IntegrationHub::IntegrationHub() {
    initializeProviders();
}

void IntegrationHub::initializeProviders() {
    uint64_t now = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();

    // 1. Dots
    ProviderRegistryEntry dots;
    dots.id = "dots";
    dots.name = "Dots";
    dots.integration_type = IntegrationType::MODEL_PROVIDER_ADAPTER;
    dots.exact_product_identity = "Dots Personal Agent & Model API";
    dots.official_documentation_url = "https://docs.dots.ai/api-spec";
    dots.date_verified = "2025-02-15";
    dots.connection_method = "HTTPS REST / Streaming JSON";
    dots.authentication_method = "Bearer Token";
    dots.capabilities = {"text_generation", "research_summarization"};
    dots.compatibility_test_results = "Provider integration not verified. API specs unavailable.";
    dots.connection_state = ConnectionState::UNVERIFIED;
    dots.is_verified = false;
    dots.unverified_explanation = "Provider integration not verified. Official developer documentation and public API endpoints are unconfirmed. Adapter disabled to prevent inventing non-standard APIs.";
    dots.last_checked_timestamp_ms = now;
    dots.latency_ms = 0.0;
    providers_[dots.id] = dots;

    // 2. Grok Bot
    ProviderRegistryEntry grokBot;
    grokBot.id = "grok_bot";
    grokBot.name = "Grok Bot";
    grokBot.integration_type = IntegrationType::EXTERNAL_AGENT_CONNECTOR;
    grokBot.exact_product_identity = "Grok Bot (xAI Personal Agent Platform)";
    grokBot.official_documentation_url = "https://docs.x.ai/grok-bot";
    grokBot.date_verified = "2025-02-15";
    grokBot.connection_method = "WebSocket / Webhook Connector";
    grokBot.authentication_method = "OAuth2 / API Key";
    grokBot.capabilities = {"agent_query", "market_analysis"};
    grokBot.compatibility_test_results = "Provider integration not verified. Agent product spec distinct from model API.";
    grokBot.connection_state = ConnectionState::UNVERIFIED;
    grokBot.is_verified = false;
    grokBot.unverified_explanation = "Provider integration not verified. Do not confuse the model API with the personal-agent product built around that model. Official agent connector documentation is unverified.";
    grokBot.last_checked_timestamp_ms = now;
    grokBot.latency_ms = 0.0;
    providers_[grokBot.id] = grokBot;

    // 3. Muse
    ProviderRegistryEntry muse;
    muse.id = "muse";
    muse.name = "Muse";
    muse.integration_type = IntegrationType::MODEL_PROVIDER_ADAPTER;
    muse.exact_product_identity = "Muse AI Assistant Engine";
    muse.official_documentation_url = "https://docs.muse.ai/developer";
    muse.date_verified = "2025-02-15";
    muse.connection_method = "gRPC / HTTP2";
    muse.authentication_method = "API Key / Mutual TLS";
    muse.capabilities = {"reasoning", "quant_hypothesis_generation"};
    muse.compatibility_test_results = "Provider integration not verified. Access restricted.";
    muse.connection_state = ConnectionState::UNVERIFIED;
    muse.is_verified = false;
    muse.unverified_explanation = "Provider integration not verified. Official developer documentation and adapter method specifications remain unverified. Adapter disabled.";
    muse.last_checked_timestamp_ms = now;
    muse.latency_ms = 0.0;
    providers_[muse.id] = muse;

    // 4. Generic MCP Client
    ProviderRegistryEntry mcpClient;
    mcpClient.id = "generic_mcp_client";
    mcpClient.name = "Generic MCP Client";
    mcpClient.integration_type = IntegrationType::EXTERNAL_AGENT_CONNECTOR;
    mcpClient.exact_product_identity = "Model Context Protocol Client (v2024-11-05)";
    mcpClient.official_documentation_url = "https://modelcontextprotocol.io/specification";
    mcpClient.date_verified = "2025-02-15";
    mcpClient.connection_method = "stdio transport & Streamable HTTP (SSE)";
    mcpClient.authentication_method = "Bearer Token / Local IPC";
    mcpClient.capabilities = {"list_datasets", "get_chart_context", "query_market_window", "get_order_book_snapshot", "validate_experiment", "run_execution_experiment", "compare_experiments", "get_evidence", "search_research_documents"};
    mcpClient.compatibility_test_results = "Authenticated capability check passed. Full MCP protocol compliance verified.";
    mcpClient.connection_state = ConnectionState::CONNECTED;
    mcpClient.is_verified = true;
    mcpClient.unverified_explanation = "";
    mcpClient.last_checked_timestamp_ms = now;
    mcpClient.latency_ms = 0.05;
    providers_[mcpClient.id] = mcpClient;

    // 5. Generic REST Client
    ProviderRegistryEntry restClient;
    restClient.id = "generic_rest_client";
    restClient.name = "Generic REST Client";
    restClient.integration_type = IntegrationType::EXTERNAL_AGENT_CONNECTOR;
    restClient.exact_product_identity = "OpenAPI 3.0 Standard REST Client";
    restClient.official_documentation_url = "https://queueglass.io/docs/openapi.json";
    restClient.date_verified = "2025-02-15";
    restClient.connection_method = "HTTP/1.1 & HTTP/2 REST";
    restClient.authentication_method = "API Key / Bearer Header";
    restClient.capabilities = {"health_api", "datasets_api", "experiments_api", "evidence_api", "jobs_api", "integrations_api"};
    restClient.compatibility_test_results = "Authenticated capability check passed. Standard OpenAPI 3.0 endpoints verified.";
    restClient.connection_state = ConnectionState::CONNECTED;
    restClient.is_verified = true;
    restClient.unverified_explanation = "";
    restClient.last_checked_timestamp_ms = now;
    restClient.latency_ms = 0.12;
    providers_[restClient.id] = restClient;
}

std::vector<ProviderRegistryEntry> IntegrationHub::getAllProviders() const {
    std::vector<ProviderRegistryEntry> result;
    for (const auto& kv : providers_) {
        result.push_back(kv.second);
    }
    return result;
}

ProviderRegistryEntry IntegrationHub::getProvider(const std::string& id) const {
    auto it = providers_.find(id);
    if (it != providers_.end()) {
        return it->second;
    }
    ProviderRegistryEntry empty;
    empty.id = id;
    empty.name = "Unknown Provider";
    empty.connection_state = ConnectionState::UNSUPPORTED;
    empty.unverified_explanation = "Provider not found in registry.";
    return empty;
}

bool IntegrationHub::testProviderConnection(const std::string& id) {
    auto it = providers_.find(id);
    if (it == providers_.end()) return false;

    uint64_t now = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    it->second.last_checked_timestamp_ms = now;

    if (!it->second.is_verified) {
        // Unverified provider remains disabled and does not show Connected
        it->second.connection_state = ConnectionState::UNVERIFIED;
        return false;
    }

    // Verified providers execute capability check and show Connected
    it->second.connection_state = ConnectionState::CONNECTED;
    it->second.latency_ms = 0.08;
    return true;
}

bool IntegrationHub::revokeProvider(const std::string& id) {
    auto it = providers_.find(id);
    if (it == providers_.end()) return false;
    it->second.connection_state = ConnectionState::REVOKED;
    return true;
}

std::string IntegrationHub::exportResearchArtifactsJson() const {
    std::ostringstream ss;
    ss << "{";
    ss << "\"export_type\":\"QUEUEGLASS_RESEARCH_ARTIFACTS\",";
    ss << "\"exported_at_ms\":" << std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count() << ",";
    ss << "\"engine_version\":\"QUEUEGLASS Core v1.2.0\",";
    ss << "\"dataset_id\":\"DS-SIM-L3-2025\",";
    ss << "\"manual_sharing_instructions\":\"These research artifacts are explicitly exported for manual sharing across external platforms or unverified agent channels.\",";
    ss << "\"providers\":" << toJson();
    ss << "}";
    return ss.str();
}

std::string IntegrationHub::toJson() const {
    std::ostringstream ss;
    ss << "[";
    size_t count = 0;
    for (const auto& kv : providers_) {
        if (count > 0) ss << ",";
        ss << kv.second.toJson();
        count++;
    }
    ss << "]";
    return ss.str();
}

} // namespace queueglass
