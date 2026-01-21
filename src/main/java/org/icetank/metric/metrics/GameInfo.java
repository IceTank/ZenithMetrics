package org.icetank.metric.metrics;


import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.TPS;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public class GameInfo implements Registerable {
    public static Gauge reportedTPS;
    public static Gauge reportedPing;
    public static Gauge reportedPlayerCount;
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("zenith_server_tps")
                .help("Current region TPS")
                .callback(callback -> callback.call(TPS.getTPSValue()))
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
    }
}
