#pragma once

#include <string>
#include <vector>
#include <unordered_map>
#include <cstdint>

namespace queueglass {

enum class IntegrationType {
    EXTERNAL_AGENT_CONNECTOR,
    MODEL_PROVIDER_ADAPTER,
    INTERNAL_RESEARCH_PLUGIN
};

inline std::string integrationTypeToString(IntegrationType type) {
    switch (type) {
        case IntegrationType::EXTERNAL_AGENT_CONNECTOR: return "EXTERNAL_AGENT_CONNECTOR";
        case IntegrationType::MODEL_PROVIDER_ADAPTER: return "MODEL_PROVIDER_ADAPTER";
        case IntegrationType::INTERNAL_RESEARCH_PLUGIN: return "INTERNAL_RESEARCH_PLUGIN";
    }
    return "UNKNOWN";
}

enum class ConnectionState {
    NOT_CONFIGURED,
    REQUIRES_AUTHORIZATION,
    CONNECTED,
    DEGRADED,
    REVOKED,
    UNSUPPORTED,
    UNVERIFIED
};

inline std::string connectionStateToString(ConnectionState state) {
    switch (state) {
        case ConnectionState::NOT_CONFIGURED: return "Not configured";
        case ConnectionState::REQUIRES_AUTHORIZATION: return "Requires authorization";
        case ConnectionState::CONNECTED: return "Connected";
        case ConnectionState::DEGRADED: return "Degraded";
        case ConnectionState::REVOKED: return "Revoked";
        case ConnectionState::UNSUPPORTED: return "Unsupported";
        case ConnectionState::UNVERIFIED: return "Unverified";
    }
    return "Unverified";
}

struct ProviderRegistryEntry {
    std::string id;
    std::string name;
    IntegrationType integration_type{IntegrationType::EXTERNAL_AGENT_CONNECTOR};

    // Verification Record Fields (Section 15)
    std::string exact_product_identity;
    std::string official_documentation_url;
    std::string date_verified;
    std::string connection_method;
    std::string authentication_method;
    std::vector<std::string> capabilities;
    std::string compatibility_test_results;

    ConnectionState connection_state{ConnectionState::UNVERIFIED};
    bool is_verified{false};
    std::string unverified_explanation;
    uint64_t last_checked_timestamp_ms{0};
    double latency_ms{0.0};

    std::string toJson() const;
};

class IntegrationHub {
public:
    IntegrationHub();

    std::vector<ProviderRegistryEntry> getAllProviders() const;
    ProviderRegistryEntry getProvider(const std::string& id) const;

    bool testProviderConnection(const std::string& id);
    bool revokeProvider(const std::string& id);

    std::string exportResearchArtifactsJson() const;
    std::string toJson() const;

private:
    std::unordered_map<std::string, ProviderRegistryEntry> providers_;
    void initializeProviders();
};

} // namespace queueglass
