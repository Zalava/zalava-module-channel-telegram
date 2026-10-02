package org.zalava.modules.telegram;

interface TelegramDeliveryGateway extends AutoCloseable {
  void send(TelegramOutboundMessage message);

  @Override
  default void close() {}
}
