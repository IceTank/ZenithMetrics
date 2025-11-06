package org.example;

import java.util.HashMap;
import java.util.Map;

/**
 * Example configuration POJO.
 *
 * Configurations are saved and loaded to JSON files
 *
 * All fields should be public and mutable.
 *
 * Fields to static inner classes generate nested JSON objects.
 */
public class MetricsConfig {
    public boolean publishMetrics = true;

    public MetricsServer metricsServer = new MetricsServer();
    public static class MetricsServer {
        public String host = "0.0.0.0";
        public int port = 0;
    }

    public ServiceDiscovery serviceDiscovery = new ServiceDiscovery();
    public static class ServiceDiscovery {
        public String serviceId = "";
        public String serviceName = "zenith-proxy";
        public String host = "http://localhost";
        public int port = 9092;
        public String targetHost = "localhost";
        public Map<String, String> labels = new HashMap<>();
    }
}
