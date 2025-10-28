package co.personal.ynabsyncher.infrastructure.client;

import co.personal.ynabsyncher.spi.client.YnabApiClient;
import org.junit.jupiter.api.Disabled;

/**
 * Contract tests for YnabApiClientImpl are disabled in favor of WireMock integration tests.
 * 
 * The comprehensive WireMock tests in YnabApiClientWireMockTest provide better validation of:
 * - HTTP integration behavior with realistic YNAB API responses
 * - Error handling with proper HTTP status codes
 * - JSON mapping and parsing validation
 * - All contract compliance requirements
 * 
 * Creating meaningful mocks for this complex HTTP client would require duplicating
 * most of the implementation logic, providing little additional value beyond the
 * existing WireMock integration tests.
 */
@Disabled("Contract tests replaced by comprehensive WireMock integration tests")
class YnabApiClientImplContractTest extends YnabApiClientContractTest {

    @Override
    protected YnabApiClient createYnabApiClient() {
        throw new UnsupportedOperationException(
                "Contract tests disabled - use YnabApiClientWireMockTest for comprehensive validation");
    }
}