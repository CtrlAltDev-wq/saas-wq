package com.wq.server.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.wq.common.api.Result;
import com.wq.common.api.ResultCode;
import com.wq.common.exception.BusinessException;
import com.wq.server.payment.StripeEventDeduplicator;
import com.wq.server.payment.StripeProperties;
import com.wq.server.payment.StripeWebhookHandler;
import com.wq.server.vo.WebhookAckVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint receiving Stripe asynchronous events. Requests are authenticated by the
 * Stripe signature header instead of a JWT, and every event id is claimed in Redis before
 * processing to guarantee idempotency.
 */
@Tag(name = "Billing Webhooks", description = "Stripe asynchronous event receiver")
@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties properties;
    private final StripeEventDeduplicator deduplicator;
    private final StripeWebhookHandler webhookHandler;

    /**
     * Validates the Stripe signature, then applies the event exactly once.
     *
     * @param payload   raw request body
     * @param signature Stripe-Signature header
     * @return acknowledgement, with duplicate set when the event was already processed
     */
    @Operation(summary = "Receive a Stripe webhook event")
    @PostMapping("/webhook")
    public Result<WebhookAckVO> handleWebhook(@RequestBody String payload,
                                              @RequestHeader(value = "Stripe-Signature", required = false)
                                              String signature) {
        if (!StringUtils.hasText(properties.getWebhookSecret())) {
            log.error("Stripe webhook received but no signing secret is configured");
            throw new BusinessException(ResultCode.PAYMENT_NOT_CONFIGURED);
        }
        if (!StringUtils.hasText(signature)) {
            throw new BusinessException(ResultCode.INVALID_WEBHOOK_SIGNATURE);
        }
        Event event = verify(payload, signature);
        if (!deduplicator.register(event.getId())) {
            log.info("Duplicate Stripe event {} ignored", event.getId());
            return Result.success(new WebhookAckVO(true, true));
        }
        try {
            webhookHandler.handle(event);
        } catch (RuntimeException exception) {
            deduplicator.release(event.getId());
            throw exception;
        }
        return Result.success(new WebhookAckVO(true, false));
    }

    /**
     * Verifies the payload signature with the configured webhook secret.
     *
     * @param payload   raw request body
     * @param signature Stripe-Signature header
     * @return verified event
     */
    private Event verify(String payload, String signature) {
        try {
            return Webhook.constructEvent(payload, signature, properties.getWebhookSecret());
        } catch (SignatureVerificationException exception) {
            log.warn("Rejected Stripe webhook with an invalid signature", exception);
            throw new BusinessException(ResultCode.INVALID_WEBHOOK_SIGNATURE);
        }
    }
}
