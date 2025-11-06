package org.example.prometheus;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public interface PrometheusMetric {
    @Nullable String getHelpText();
    @Nullable String getTypeText();
    List<String> getValueLines();

    static MetricsBuilder builder() {
        return new MetricsBuilder();
    }
    class MetricsBuilder {
        private Supplier<String> helpTextSupplier = () -> "";
        private Supplier<String> typeTextSupplier = () -> "";
        private Supplier<List<String>> valueLinesSupplier;

        public MetricsBuilder helpText(Supplier<String> helpTextSupplier) {
            this.helpTextSupplier = helpTextSupplier;
            return this;
        }
        public MetricsBuilder typeText(Supplier<String> typeTextSupplier) {
            this.typeTextSupplier = typeTextSupplier;
            return this;
        }
        public MetricsBuilder valueLines(Supplier<List<String>> valueLinesSupplier) {
            this.valueLinesSupplier = valueLinesSupplier;
            return this;
        }

        public PrometheusMetric build() {
            return new PrometheusMetric() {
                @Override
                public @Nullable String getHelpText() {
                    return helpTextSupplier.get();
                }

                @Override
                public @Nullable String getTypeText() {
                    return typeTextSupplier.get();
                }

                @Override
                public List<String> getValueLines() {
                    return valueLinesSupplier.get();
                }
            };
        }
    }
}
