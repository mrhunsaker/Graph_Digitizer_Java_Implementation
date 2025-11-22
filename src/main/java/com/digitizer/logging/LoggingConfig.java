package com.digitizer.logging;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Central logging configuration metadata for the Graph Digitizer application.
 * <p>
 * This class exposes constants that mirror settings in {@code log4j2.xml} so
 * other components can reference log file locations without hard-coding
 * duplicate strings. It does <strong>not</strong> mutate Log4j2 configuration at
 * runtime; Log4j2 picks up its XML configuration during initialization.
 * <p>
 * <strong>Async Logging:</strong> To enable high-throughput asynchronous
 * logging, start the JVM with the system property:
 * <pre>
 *   -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
 * </pre>
 * This engages the {@code <AsyncRoot>} logger defined in {@code log4j2.xml}.
 * Ensure the LMAX Disruptor dependency is present (declared in the Maven
 * {@code pom.xml}).
 * <p>
 * <strong>JSON Logs:</strong> Structured events are written to the file
 * referenced by {@link #JSON_LOG_FILE}. Each line is a standalone JSON object
 * suitable for ingestion by log processors (e.g., ELK, Splunk) using a
 * newline-delimited JSON parser.
 */
public final class LoggingConfig {

    /** Name of the primary rolling text log file. */
    public static final String TEXT_LOG_FILE = "logs/graph-digitizer.log";
    /** Name of the JSON structured log file. */
    public static final String JSON_LOG_FILE = "logs/graph-digitizer.json";

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfig.class);

    private LoggingConfig() {
        // utility class
    }

    /**
     * Emits a diagnostic message confirming logging backend activation.
     * Intended for optional startup health checks.
     *
     * @param appVersion the application version string to include in the banner
     */
    public static void logStartupBanner(String appVersion) {
        logger.info("Logging initialized for Graph Digitizer version {} (text log: {}, json log: {})", appVersion, TEXT_LOG_FILE, JSON_LOG_FILE);
    }

    /**
     * Initialize basic MDC (Mapped Diagnostic Context) keys used for correlation.
     * This attaches a session identifier and (optionally) a user id if supplied.
     *
     * @param sessionId unique identifier for this application run (e.g. timestamp or random UUID)
     * @param userId optional user id (null if not applicable)
     */
    public static void initializeMdc(String sessionId, String userId) {
        if (sessionId != null) {
            MDC.put("session", sessionId);
        }
        if (userId != null) {
            MDC.put("user", userId);
        }
        logger.debug("MDC initialized (session={}, user={})", sessionId, userId);
    }

    /**
     * Performs lightweight environment checks and logs findings:
     * <ul>
     *   <li>Java version</li>
     *   <li>Async logger property presence</li>
     *   <li>Log directory existence / creation</li>
     * </ul>
     */
    public static void runEnvironmentChecks() {
        String javaVersion = System.getProperty("java.version", "unknown");
        boolean asyncEnabled = System.getProperty("Log4jContextSelector", "")
                .contains("AsyncLoggerContextSelector");
        logger.info("Environment check: java.version={}, asyncLoggingEnabled={}", javaVersion, asyncEnabled);

        Path logDir = Path.of("logs");
        if (!Files.exists(logDir)) {
            try {
                Files.createDirectories(logDir);
                logger.info("Created missing log directory: {}", logDir.toAbsolutePath());
            } catch (SecurityException se) {
                logger.warn("Security manager blocked log directory creation: {}", se.getMessage());
            } catch (java.io.IOException ioe) {
                logger.warn("I/O error creating log directory {}: {}", logDir.toAbsolutePath(), ioe.getMessage());
            }
        }
        // Record directory canonical path for debugging / ingestion tools.
        try {
            logger.debug("Log directory resolved to: {}", new File("logs").getCanonicalPath());
        } catch (java.io.IOException ioe) {
            logger.debug("Failed to resolve canonical log directory path: {}", ioe.getMessage());
        }
    }

    /**
     * Convenience method to generate a simple session identifier based on current timestamp.
     * @return session id string
     */
    public static String generateSessionId() {
        return Long.toString(Instant.now().toEpochMilli());
    }
}
