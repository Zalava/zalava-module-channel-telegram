package org.zalava.modules.telegram;

import java.util.Optional;
import org.zalava.channels.ChannelDestination;
import org.zalava.channels.ChannelInput;
import org.zalava.channels.ChannelInteractionKind;
import org.zalava.channels.ChannelPrivacy;
import org.zalava.channels.ExternalIdentityReference;
import org.zalava.channels.IncomingInteraction;

/** Pure mapping with a deliberately small trusted input surface for SDK adapters and tests. */
final class TelegramInboundMapper {
  private TelegramInboundMapper() {}

  static Optional<IncomingInteraction> text(TelegramInboundUpdate update) {
    if (!update.isPrivateChat()
        || blank(update.updateId())
        || blank(update.userId())
        || blank(update.chatId())
        || blank(update.text())) return Optional.empty();
    return Optional.of(
        new IncomingInteraction(
            "telegram:update:" + update.updateId(),
            new ExternalIdentityReference(TelegramChannel.CHANNEL_ID, update.userId()),
            new ChannelDestination(
                TelegramChannel.CHANNEL_ID, update.chatId(), ChannelPrivacy.PRIVATE),
            ChannelInteractionKind.CONVERSATION,
            new ChannelInput.Text(update.text()),
            "telegram:" + update.updateId()));
  }

  static Optional<IncomingInteraction> callback(TelegramInboundUpdate update) {
    if (!update.isPrivateChat()
        || blank(update.updateId())
        || blank(update.userId())
        || blank(update.chatId())
        || blank(update.callbackData())) return Optional.empty();
    return Optional.of(
        new IncomingInteraction(
            "telegram:update:" + update.updateId(),
            new ExternalIdentityReference(TelegramChannel.CHANNEL_ID, update.userId()),
            new ChannelDestination(
                TelegramChannel.CHANNEL_ID, update.chatId(), ChannelPrivacy.PRIVATE),
            ChannelInteractionKind.INTERACTIVE_ACTION,
            new ChannelInput.Action(update.callbackData(), java.util.Map.of()),
            "telegram:" + update.updateId()));
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}

record TelegramInboundUpdate(
    String updateId,
    String userId,
    String chatId,
    boolean isPrivateChat,
    String text,
    String callbackData) {}
