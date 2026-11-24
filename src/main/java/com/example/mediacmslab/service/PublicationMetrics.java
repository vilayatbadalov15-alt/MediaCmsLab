package com.example.mediacmslab.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusNamingConvention;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class PublicationMetrics {
    private static final String NAME = "messages_created";
    private final Counter created;

    PublicationMetrics(MeterRegistry registry) {
        created = Counter.builder(NAME).description("Content records committed to PostgreSQL").register(registry);
    }

    @Bean
    static MeterRegistryCustomizer<PrometheusMeterRegistry> publicationCounterName() {
        // The default convention strips the reserved "_created" suffix before adding "_total".
        return registry -> registry.config().namingConvention(new PrometheusNamingConvention() {
            @Override
            public String name(String name, Meter.Type type, String baseUnit) {
                return NAME.equals(name) ? NAME : super.name(name, type, baseUnit);
            }
        });
    }

    @TransactionalEventListener
    void onCreated(ContentService.Created event) {
        created.increment();
    }
}
