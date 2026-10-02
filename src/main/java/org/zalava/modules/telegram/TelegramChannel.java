package org.zalava.modules.telegram;

import java.util.Objects;
import org.zalava.channels.ChannelCapabilities;
import org.zalava.channels.ChannelDescriptor;
import org.zalava.channels.ChannelEvent;
import org.zalava.channels.ChannelInteractionReceiver;
import org.zalava.channels.ChannelTransportContext;
import org.zalava.channels.IncomingInteraction;
import org.zalava.channels.ZalavaChannel;

/** Protocol boundary: maps Telegram updates/events without owning Core policy or state. */
public final class TelegramChannel implements ZalavaChannel {
  public static final String CHANNEL_ID = "telegram";
  private static final ChannelDescriptor DESCRIPTOR =
      new ChannelDescriptor(
          CHANNEL_ID,
          "Telegram",
          new ChannelCapabilities(true, false, true, false, true, true, true, true));
  private volatile ChannelInteractionReceiver receiver = ignored -> {};
  private volatile TelegramDeliveryGateway gateway;

  public TelegramChannel() {
    this(new UnsupportedTelegramDeliveryGateway());
  }

  TelegramChannel(TelegramDeliveryGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway, "gateway");
  }

  /** Connects the explicitly configured SDK transport; replacing a live transport is rejected. */
  synchronized void connect(TelegramDeliveryGateway gateway) {
    Objects.requireNonNull(gateway, "gateway");
    if (!(this.gateway instanceof UnsupportedTelegramDeliveryGateway)) {
      throw new IllegalStateException("Telegram transport is already configured");
    }
    this.gateway = gateway;
  }

  @Override
  public ChannelDescriptor descriptor() {
    return DESCRIPTOR;
  }

  @Override
  public void bind(ChannelInteractionReceiver receiver) {
    this.receiver = Objects.requireNonNull(receiver, "receiver");
  }

  @Override
  public void start(ChannelTransportContext context) {
    Object reference = context.configuration().get("botTokenRef");
    if (!(reference instanceof String tokenReference) || tokenReference.isBlank()) {
      throw new IllegalArgumentException("Telegram botTokenRef configuration is required");
    }
    char[] secret =
        context
            .secrets()
            .resolve(tokenReference)
            .orElseThrow(
                () -> new IllegalStateException("Telegram bot token secret is unavailable"));
    try {
      TelegramSdkTransport.start(new String(secret), this);
    } finally {
      java.util.Arrays.fill(secret, '\0');
    }
  }

  /**
   * Called by the SDK adapter after strict transport mapping; rejected updates never reach Core.
   */
  void receive(IncomingInteraction interaction) {
    receiver.receive(interaction);
  }

  @Override
  public void deliver(ChannelEvent event) {
    Objects.requireNonNull(event, "event");
    if (!CHANNEL_ID.equals(event.destination().channelId())) {
      throw new IllegalArgumentException("Telegram channel cannot deliver another channel's event");
    }
    gateway.send(TelegramOutboundMapper.map(event));
  }

  @Override
  public void close() {
    gateway.close();
  }
}
