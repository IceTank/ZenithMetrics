package org.icetank.module;

import com.zenith.module.api.Module;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.icetank.api.ServiceAnnouncer;
import org.icetank.metric.Metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.icetank.MetricsPlugin.LOG;
import static org.icetank.MetricsPlugin.PLUGIN_CONFIG;

public class MetricsModule extends Module {
    HTTPServer server = null;
    ServiceAnnouncer announcer = new ServiceAnnouncer();
    private ScheduledExecutorService scheduler = null;
    private final long METRICS_INTERVAL_SECONDS = 10;

    @Override
    public boolean enabledSetting() {
        return PLUGIN_CONFIG.enabled;
    }

    @Override
    public void onEnable() {
        JvmMetrics.builder().register();
        Metrics.builder().register();

        try {
            if (server != null) {
                LOG.warn("Metrics server is already running.");
                return;
            }
            server = HTTPServer.builder()
                    .port(0)
                    .buildAndStart();

            String accountName = PLUGIN_CONFIG.serviceDiscovery.accountName;
            String serviceName = "zenith-proxy-" + accountName;
            Map<String, String> labels = new HashMap<>(PLUGIN_CONFIG.serviceDiscovery.labels);
            labels.put("accountName", accountName); // For grafana queries
            labels.put("instance", PLUGIN_CONFIG.serviceDiscovery.targetHost); // Prevent cardinality explosion

            if (accountName.isEmpty()) {
                LOG.error("Service ID is empty, configure with zenith instance name");
                throw new RuntimeException("Service ID is empty");
            }
            if (server.getPort() <= 0) {
                LOG.error("Metrics server failed to start, cannot announce serviceName");
                throw new RuntimeException("Metrics server failed to start");
            }
            String target = PLUGIN_CONFIG.serviceDiscovery.targetHost + ":" + server.getPort();

            ServiceAnnouncer.ServiceInfo info = new ServiceAnnouncer.ServiceInfo(accountName, serviceName, target, labels, 60);
            CompletableFuture.runAsync(() -> {
                try {
                    announcer.registerService(PLUGIN_CONFIG.serviceDiscovery.host,
                            PLUGIN_CONFIG.serviceDiscovery.port, info);
                } catch (Exception ex) {
                    LOG.error("Failed to register serviceName. Disabling.", ex);
                    disable();
                    return;
                }
                if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
                    scheduler = Executors.newSingleThreadScheduledExecutor();
                }
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        announcer.sendHeartbeat(PLUGIN_CONFIG.serviceDiscovery.host,
                                PLUGIN_CONFIG.serviceDiscovery.port, info);
                    } catch (Exception ex) {
                        LOG.error("Failed to send heartbeat for serviceName {}: {}", accountName, ex.getMessage());
                    }
                }, METRICS_INTERVAL_SECONDS, METRICS_INTERVAL_SECONDS, TimeUnit.SECONDS);
            });
        } catch (Exception e) {
            disable();
            LOG.error("Failed to start MetricsModule, disabling module.", e);
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
