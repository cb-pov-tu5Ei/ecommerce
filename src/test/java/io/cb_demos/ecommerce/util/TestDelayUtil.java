package io.cb_demos.ecommerce.util;

/**
 * Utility class for adding artificial delays to tests for CI/CD demo purposes.
 * This makes test execution more visible in CI pipelines.
 *
 * To disable delays, set system property: -Dtest.delays.enabled=false
 */
public class TestDelayUtil {

    private static final boolean DELAYS_ENABLED =
        Boolean.parseBoolean(System.getProperty("test.delays.enabled", "true"));

    /**
     * Add a small delay (500ms) - for unit tests
     */
    public static void smallDelay() {
        delay(500);
    }

    /**
     * Add a medium delay (1000ms) - for controller/service tests
     */
    public static void mediumDelay() {
        delay(1000);
    }

    /**
     * Add a large delay (2000ms) - for integration tests
     */
    public static void largeDelay() {
        delay(2000);
    }

    /**
     * Add an extra large delay (5000ms) - for key integration tests
     */
    public static void extraLargeDelay() {
        delay(5000);
    }

    /**
     * Add a massive delay (10000ms) - for comprehensive integration tests
     */
    public static void massiveDelay() {
        delay(10000);
    }

    /**
     * Add a custom delay
     * @param milliseconds delay in milliseconds
     */
    public static void delay(long milliseconds) {
        if (!DELAYS_ENABLED) {
            return;
        }

        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
