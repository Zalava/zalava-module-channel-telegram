package org.zalava.modules.telegram;

import java.util.List;

record TelegramOutboundMessage(String chatId, String text, List<TelegramActionButton> buttons) {
  TelegramOutboundMessage {
    if (chatId == null || chatId.isBlank())
      throw new IllegalArgumentException("chatId must not be blank");
    if (text == null || text.isBlank())
      throw new IllegalArgumentException("text must not be blank");
    buttons = List.copyOf(buttons == null ? List.of() : buttons);
  }
}

record TelegramActionButton(String label, String actionId) {}
