package org.icetank.module;

import com.zenith.module.api.Module;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.icetank.MetricsPlugin;
import org.icetank.api.ServiceAnnouncer;
import org.icetank.metric.Metrics;

import java.util.Map;
import java.util.concurrent.*;

public class MetricsModule extends Module {
    HTTPServer server = null;
    ServiceAnnouncer announcer = new ServiceAnnouncer();
    private ScheduledExecutorService scheduler = null;
    private final long METRICS_INTERVAL_SECONDS = 30;

    @Override
    public boolean enabledSetting() {
        return MetricsPlugin.PLUGIN_CONFIG.enabled;
    }

    @Override
    public void onEnable() {
        JvmMetrics.builder().register();
        Metrics.builder().register();

        try {
            if (server != null) {
                MetricsPlugin.LOG.warn("Metrics server is already running.");
                return;
            }
            server = HTTPServer.builder()
                    .port(0)
                    .buildAndStart();

            String serviceId = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.serviceId;
            String serviceName = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.serviceName;
            String host = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.host;
            int port = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.port;
            Map<String, String> labels = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.labels;

            if (serviceId.isEmpty()) {
                MetricsPlugin.LOG.error("Service ID is empty, configure with zenith instance name");
                throw new RuntimeException("Service ID is empty");
            }
            if (server.getPort() <= 0) {
                MetricsPlugin.LOG.error("Metrics server failed to start, cannot announce serviceName");
                throw new RuntimeException("Metrics server failed to start");
            }
            String target = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.targetHost + ":" + server.getPort();

            ServiceAnnouncer.ServiceInfo info = new ServiceAnnouncer.ServiceInfo(serviceId, serviceName, target, labels, 60);
            CompletableFuture.runAsync(() -> {
                try {
                    announcer.registerService(host, port, info);
                } catch (Exception ex) {
                    MetricsPlugin.LOG.error("Failed to register serviceName. Disabling.", ex);
                    disable();
                    return;
                }
                if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
                    scheduler = Executors.newSingleThreadScheduledExecutor();
                }
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        announcer.sendHeartbeat(host, port, info);
                    } catch (Exception ex) {
                        MetricsPlugin.LOG.error("Failed to send heartbeat for serviceName {}: {}", serviceId, ex.getMessage());
                    }
                }, METRICS_INTERVAL_SECONDS, METRICS_INTERVAL_SECONDS, TimeUnit.SECONDS);
            });
        } catch (Exception e) {
            shutdown();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    private void shutdown() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
