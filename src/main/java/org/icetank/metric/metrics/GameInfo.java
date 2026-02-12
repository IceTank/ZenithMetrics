package org.icetank.metric.metrics;


import com.zenith.Proxy;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;

import java.time.Duration;
import java.time.Instant;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.TPS;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public class GameInfo implements Registerable {
    private static final Instant START_TIME  = Instant.now();

    public static Gauge reportedTPS;
    public static Gauge reportedPing;
    public static Gauge reportedPlayerCount;
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("zenith_server_tps")
                .help("Current calculated region TPS")
                .callback(callback -> callback.call(Proxy.getInstance().isInQueue() ? 0 : TPS.getTPSValue()))
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_server_ping")
                .help("Current calculated average server ping")
                .callback(callback -> callback.call(Proxy.getInstance().getClient().getPing()))
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_server_player_count")
                .help("Current number of connected players")
                .callback(callback -> callback.call(CACHE.getTabListCache().getEntries().size()))
                .register(registry);
        reportedTPS = Gauge.builder()
                .name("zenith_server_reported_tps")
                .help("Server reported TPS value")
                .register(registry);
        reportedPing = Gauge.builder()
                .name("zenith_server_reported_ping")
                .help("Server reported ping value")
                .register(registry);
        reportedPlayerCount = Gauge.builder()
                .name("zenith_server_reported_player_count")
                .help("Server reported player count")
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_total_uptime")
                .help("Total uptime of the proxy in seconds")
                .callback(callback -> {
                    Duration uptimeDuration = Duration.between(START_TIME, Instant.now());
                    callback.call(uptimeDuration.getSeconds());
                })
                .register(registry);
    }

    public static void onDisconnect() {
        reportedTPS.set(0);
        reportedPing.set(0);
        reportedPlayerCount.set(0);
    }
}
