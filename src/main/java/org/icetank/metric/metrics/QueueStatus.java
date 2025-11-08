package org.icetank.metric.metrics;


import com.zenith.Proxy;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;

import java.time.Duration;
import java.time.Instant;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public class QueueStatus implements Registerable {
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("queue_position")
                .help("Current position in queue")
                .callback(callback -> callback.call(Proxy.getInstance().getQueuePosition()))
                .register(registry);
        GaugeWithCallback.builder()
                .name("queue_status")
                .help("Current queue status (1 = in queue, 0 = not in queue)")
                .callback(callback -> callback.call(Proxy.getInstance().isInQueue() ? 1 : 0))
                .register(registry);
        GaugeWithCallback.builder()
                .name("online_duration")
                .help("Duration of current online session in seconds")
                .callback(callback -> {
                    if (Proxy.getInstance().isConnected()) {
                        Instant onlineSince = Proxy.getInstance().getConnectTime();
                        if (onlineSince != null) {
                            Duration onlineDuration = Duration.between(onlineSince, Instant.now());
                            callback.call(onlineDuration.getSeconds());
                        } else {
                            callback.call(0);
                        }
                    } else {
                        callback.call(0);
                    }
                })
                .register(registry);
    }
}
