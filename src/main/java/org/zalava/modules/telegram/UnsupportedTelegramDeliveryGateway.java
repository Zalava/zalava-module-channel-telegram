package org.zalava.modules.telegram;

/** Safe default until host configuration supplies a token-backed Telegram SDK gateway. */
final class UnsupportedTelegramDeliveryGateway implements TelegramDeliveryGateway {
  @Override
  public void send(TelegramOutboundMessage message) {
    throw new IllegalStateException("Telegram transport is not configured");
  }
}
