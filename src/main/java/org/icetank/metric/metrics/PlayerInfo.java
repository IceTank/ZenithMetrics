package org.icetank.metric.metrics;


import com.zenith.Proxy;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;

import java.util.Objects;

import static com.zenith.Globals.CACHE;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public class PlayerInfo implements Registerable {
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("zenith_player_health")
                .help("Player health over time")
                .callback(callback -> callback.call(Objects.requireNonNullElse(CACHE.getPlayerCache().getThePlayer().getHealth(), 0f)))
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_player_food_level")
                .help("Player food level over time")
                .callback(callback -> callback.call(CACHE.getPlayerCache().getThePlayer().getFood()))
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_player_experience_level")
                .help("Player experience level over time")
                .callback(callback -> callback.call(CACHE.getPlayerCache().getThePlayer().getLevel()))
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_player_is_alive")
                .help("Player alive status over time")
                .callback(callback -> callback.call(CACHE.getPlayerCache().getThePlayer().isAlive() ? 1 : 0))
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_player_is_connected")
                .help("Player connection status over time")
                .callback(callback -> {
                    Proxy proxy = Proxy.getInstance();
                    callback.call(proxy.isConnected() ? 1 : 0);
                })
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_player_saturation")
                .help("Player saturation over time")
                .callback(callback -> callback.call(CACHE.getPlayerCache().getThePlayer().getSaturation()))
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_player_experience")
                .help("Player experience over time")
                .callback(callback -> callback.call(CACHE.getPlayerCache().getThePlayer().getExperience()))
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_player_total_experience")
                .help("Player total experience over time")
                .callback(callback -> callback.call(CACHE.getPlayerCache().getThePlayer().getTotalExperience()))
                .register(registry);
    }
}
