package org.zalava.modules.telegram;

import java.util.List;
import org.zalava.channels.ChannelEvent;

final class TelegramOutboundMapper {
  private TelegramOutboundMapper() {}

  static TelegramOutboundMessage map(ChannelEvent event) {
    String chatId = event.destination().deliveryHandle();
    return switch (event) {
      case ChannelEvent.Text text -> new TelegramOutboundMessage(chatId, text.text(), List.of());
      case ChannelEvent.Progress progress ->
          new TelegramOutboundMessage(chatId, progress.message(), List.of());
      case ChannelEvent.Result result ->
          new TelegramOutboundMessage(chatId, result.summary(), List.of());
      case ChannelEvent.Error error ->
          new TelegramOutboundMessage(chatId, error.message(), List.of());
      case ChannelEvent.ApprovalPrompt prompt ->
          new TelegramOutboundMessage(
              chatId,
              prompt.approval().prompt(),
              List.of(
                  new TelegramActionButton("Approve", prompt.approval().approveActionId()),
                  new TelegramActionButton("Deny", prompt.approval().denyActionId())));
    };
  }
}
