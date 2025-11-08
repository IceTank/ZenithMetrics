package org.icetank.metric.metrics;


import com.zenith.Proxy;
import com.zenith.cache.data.entity.EntityPlayer;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;

import static com.zenith.Globals.CACHE;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public class PlayerInfo implements Registerable {
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("player")
                .help("Current status")
                .labelNames("type")
                .callback(callback -> {
                    EntityPlayer player = CACHE.getPlayerCache().getThePlayer();
                    Float health = player.getHealth();
                    callback.call(health != null ? health : 0, "health");

                    callback.call(player.getSaturation(), "saturation");
                    callback.call(player.getFood(), "food_level");
                    callback.call(player.getExperience(), "experience");
                    callback.call(player.getLevel(), "experience_level");
                    callback.call(player.getTotalExperience(), "total_experience");
                    callback.call(player.isAlive() ? 1 : 0, "is_alive");

                    Proxy proxy = Proxy.getInstance();
                    callback.call(proxy.isConnected() ? 1 : 0, "is_connected");
                })
                .register(registry);
    }
}
