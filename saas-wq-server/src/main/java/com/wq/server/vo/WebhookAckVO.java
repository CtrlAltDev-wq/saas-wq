package com.wq.server.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Acknowledgement returned to Stripe after a webhook delivery.
 *
 * @param received  always true, the event was handled
 * @param duplicate true when the event id had already been processed
 */
@Schema(description = "Webhook acknowledgement")
public record WebhookAckVO(boolean received, boolean duplicate) {
}
