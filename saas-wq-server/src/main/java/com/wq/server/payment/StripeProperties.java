package com.wq.server.payment;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stripe integration settings bound from the {@code wq.stripe} prefix.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "wq.stripe")
public class StripeProperties {

    /**
     * Stripe secret API key. Empty disables checkout.
     */
    private String secretKey;

    /**
     * Webhook signing secret issued by the Stripe dashboard or CLI.
     */
    private String webhookSecret;

    /**
     * Browser redirect after a successful checkout.
     */
    private String successUrl;

    /**
     * Browser redirect when the user abandons the checkout.
     */
    private String cancelUrl;

    /**
     * ISO 4217 currency code used for inline prices.
     */
    private String currency = "usd";

    /**
     * Sellable plans keyed by plan code.
     */
    private Map<String, Plan> plans = new LinkedHashMap<>();

    /**
     * Price definition of a plan.
     */
    @Getter
    @Setter
    public static class Plan {

        /**
         * Display name sent to Stripe.
         */
        private String name;

        /**
         * Amount in the smallest currency unit.
         */
        private Long amountCents;

        /**
         * Billing interval, month or year.
         */
        private String interval = "month";

        /**
         * Optional pre-created Stripe price identifier, used instead of an inline price.
         */
        private String stripePriceId;
    }
}
