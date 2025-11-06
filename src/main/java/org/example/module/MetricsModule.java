package org.example.module;

import com.zenith.module.api.Module;
import org.example.MetricsPlugin;
import org.example.api.ServiceAnnouncer;
import org.example.api.WebServer;
import org.example.metrics.Metrics;
import org.example.prometheus.MetricsProvider;
import org.example.prometheus.PrometheusMetric;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MetricsModule extends Module {
    WebServer server = new WebServer();
    ServiceAnnouncer announcer = new ServiceAnnouncer();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> metricsTask;
    private final long METRICS_INTERVAL_SECONDS = 30;

    private final List<MetricsProvider> providers = new CopyOnWriteArrayList<>();

    public void registerMetricsProviders(List<MetricsProvider> providers) {
        for (MetricsProvider provider : providers) {
            registerMetricsProvider(provider);
        }
    }
    public void registerMetricsProvider(MetricsProvider provider) {
        if (!providers.contains(provider)) {
            providers.add(provider);
        }
    }

    public void clearMetricsProviders() {
        providers.clear();
    }

    @Override
    public boolean enabledSetting() {
        return MetricsPlugin.PLUGIN_CONFIG.publishMetrics;
    }

    @Override
    public void onEnable() {
        server.start();
        registerMetricsProviders(Metrics.getMetricsProviders());

        String serviceId = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.serviceId;
        String serviceName = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.serviceName;
        String host = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.host;
        int port = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.port;
        Map<String, String> labels = MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.labels;

        if (serviceId.isEmpty()) {
            MetricsPlugin.LOG.error("Service ID is empty, configure with zenith instance name");
            return;
        }
        if (server.getPort() <= 0) {
            MetricsPlugin.LOG.error("Metrics server is not running, cannot announce serviceName");
            return;
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
            metricsTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    announcer.sendHeartbeat(host, port, info);
                } catch (Exception ex) {
                    MetricsPlugin.LOG.error("Failed to send heartbeat for serviceName {}: {}", serviceId, ex.getMessage());
                }
            }, METRICS_INTERVAL_SECONDS, METRICS_INTERVAL_SECONDS, TimeUnit.SECONDS);
        });
    }

    @Override
    public void onDisable() {
        if (metricsTask != null) {
            metricsTask.cancel(true);
        }
        scheduler.shutdown();
        server.stop();
        clearMetricsProviders();

        CompletableFuture.runAsync(() -> {
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        });
    }

    public String getMetricsText() {
        StringBuilder sb = new StringBuilder();
        for (MetricsProvider provider : providers) {
            String helpText = null;
            String typeText = null;
            List<String> valueLines;
            try {
                PrometheusMetric metric = provider.getMetric();
                helpText = metric.getHelpText();
                typeText = metric.getTypeText();
                valueLines = metric.getValueLines();
            } catch (Exception ex) {
                MetricsPlugin.LOG.error("Error getting metric from provider {}: {}", provider, ex.getMessage());
                continue;
            }
            if (helpText != null && !helpText.isEmpty()) {
                sb.append(helpText).append("\n");
            }
            if (typeText != null && !typeText.isEmpty()) {
                sb.append(typeText).append("\n");
            }
            for (String line : valueLines) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
