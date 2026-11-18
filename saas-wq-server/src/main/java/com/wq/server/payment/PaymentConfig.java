package com.wq.server.payment;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Stripe configuration properties of the billing module.
 */
@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class PaymentConfig {
}
