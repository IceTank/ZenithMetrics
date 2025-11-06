package org.example.metrics;

import com.zenith.Proxy;
import org.example.prometheus.MetricsProvider;
import org.example.prometheus.PrometheusMetric;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Metrics {
    public static List<MetricsProvider> getMetricsProviders() {
        return List.of(MetricsProvider.builder()
                        .metric(PrometheusMetric.builder()
                                .helpText(() -> "# HELP queue_position Current position in queue")
                                .typeText(() -> "# TYPE queue_position gauge")
                                .valueLines(() -> List.of(String.valueOf(Proxy.getInstance().getQueuePosition())))
                                .build())
                        .build(),
                MetricsProvider.builder()
                        .metric(PrometheusMetric.builder()
                                .helpText(() -> "# HELP queue_status Current queue status (1 = in queue, 0 = not in queue)")
                                .typeText(() -> "# TYPE queue_status gauge")
                                .valueLines(() -> List.of(String.valueOf(Proxy.getInstance().isInQueue() ? 1 : 0)))
                                .build())
                        .build(),
                MetricsProvider.builder()
                        .metric(PrometheusMetric.builder()
                                .helpText(() -> "# HELP online_duration Duration of current online session in seconds")
                                .typeText(() -> "# TYPE online_duration gauge")
                                .valueLines(() -> {
                                    Instant instant = Proxy.getInstance().getConnectTime();
                                    if (instant == null) {
                                        return List.of("0");
                                    }
                                    long seconds = Duration.between(instant, Instant.now()).getSeconds();
                                    return List.of(String.valueOf(seconds));
                                })
                                .build())
                        .build());
    }
}
