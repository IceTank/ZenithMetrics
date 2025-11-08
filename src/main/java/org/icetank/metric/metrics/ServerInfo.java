package org.icetank.metric.metrics;


import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.TPS;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public class ServerInfo implements Registerable {
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("server_tps")
                .help("Current region TPS")
                .callback(callback -> {
                    callback.call(TPS.getTPSValue());
                })
                .register(registry);
        GaugeWithCallback.builder()
                .name("server_player_count")
                .help("Current number of connected players")
                .callback(callback -> {
                    callback.call(CACHE.getTabListCache().getEntries().size());
                })
                .register(registry);
    }
}
