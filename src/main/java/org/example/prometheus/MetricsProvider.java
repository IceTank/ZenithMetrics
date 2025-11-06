package org.example.prometheus;

public interface MetricsProvider {
    PrometheusMetric getMetric();

    static MetricsProviderBuilder builder() {
        return new MetricsProviderBuilder();
    }
    class MetricsProviderBuilder {
        private PrometheusMetric metric;

        public MetricsProviderBuilder metric(PrometheusMetric metric) {
            this.metric = metric;
            return this;
        }

        public MetricsProvider build() {
            return new MetricsProvider() {
                @Override
                public PrometheusMetric getMetric() {
                    return metric;
                }
            };
        }
    }
}
