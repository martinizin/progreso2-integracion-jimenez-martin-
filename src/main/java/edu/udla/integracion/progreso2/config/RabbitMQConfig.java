package edu.udla.integracion.progreso2.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara la topología de RabbitMQ al arrancar la aplicación (idempotente).
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  PATRÓN POINT-TO-POINT (Facturación)                                │
 * │  billing.exchange (DirectExchange) ──routing key "billing"──►       │
 * │                                          billing.queue              │
 * │  Garantiza que la orden de cobro sea procesada por UN SOLO          │
 * │  consumidor (evita doble facturación).                              │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │  PATRÓN PUBLISH/SUBSCRIBE (Notificaciones + Analítica)              │
 * │  appointments.events (FanoutExchange) ──►  notifications.queue      │
 * │                                       └──►  analytics.queue        │
 * │  El mismo evento llega a TODOS los consumidores suscritos           │
 * │  sin que el productor los conozca.                                  │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@Configuration
public class RabbitMQConfig {

    // ── Constantes ───────────────────────────────────────────────────────────

    /** Point-to-Point: Facturación */
    public static final String BILLING_QUEUE       = "billing.queue";
    public static final String BILLING_EXCHANGE    = "billing.exchange";
    public static final String BILLING_ROUTING_KEY = "billing";

    /** Publish/Subscribe: Eventos de cita confirmada */
    public static final String EVENTS_EXCHANGE      = "appointments.events";
    public static final String NOTIFICATIONS_QUEUE  = "notifications.queue";
    public static final String ANALYTICS_QUEUE      = "analytics.queue";

    // ── Point-to-Point: Facturación ──────────────────────────────────────────

    @Bean
    public Queue billingQueue() {
        return QueueBuilder.durable(BILLING_QUEUE).build();
    }

    @Bean
    public DirectExchange billingExchange() {
        return ExchangeBuilder.directExchange(BILLING_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding billingBinding(Queue billingQueue, DirectExchange billingExchange) {
        return BindingBuilder.bind(billingQueue)
            .to(billingExchange)
            .with(BILLING_ROUTING_KEY);
    }

    // ── Publish/Subscribe: Eventos ───────────────────────────────────────────

    @Bean
    public FanoutExchange appointmentsEventsExchange() {
        return ExchangeBuilder.fanoutExchange(EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    @Bean
    public Binding notificationsBinding(
            Queue notificationsQueue, FanoutExchange appointmentsEventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(appointmentsEventsExchange);
    }

    @Bean
    public Binding analyticsBinding(
            Queue analyticsQueue, FanoutExchange appointmentsEventsExchange) {
        return BindingBuilder.bind(analyticsQueue).to(appointmentsEventsExchange);
    }
}
