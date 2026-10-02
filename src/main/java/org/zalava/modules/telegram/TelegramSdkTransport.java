package org.zalava.modules.telegram;

import java.util.List;
import java.util.Objects;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

/** TelegramBots 7.10 SDK adapter. It is explicitly started with a secret token by the host. */
public final class TelegramSdkTransport implements TelegramDeliveryGateway, AutoCloseable {
  private final OkHttpTelegramClient client;
  private final TelegramBotsLongPollingApplication application;

  private TelegramSdkTransport(String botToken, TelegramChannel channel) {
    this.client = new OkHttpTelegramClient(botToken);
    this.application = new TelegramBotsLongPollingApplication();
    try {
      application.registerBot(
          botToken, (LongPollingSingleThreadUpdateConsumer) update -> receive(update, channel));
    } catch (Exception exception) {
      closeQuietly(application);
      throw new IllegalStateException("Could not start Telegram long polling", exception);
    }
  }

  /**
   * Starts long polling; callers must obtain the token from an approved secret/configuration
   * source.
   */
  public static TelegramSdkTransport start(String botToken, TelegramChannel channel) {
    if (botToken == null || botToken.isBlank())
      throw new IllegalArgumentException("botToken must not be blank");
    TelegramChannel target = Objects.requireNonNull(channel, "channel");
    TelegramSdkTransport transport = new TelegramSdkTransport(botToken, target);
    target.connect(transport);
    return transport;
  }

  @Override
  public void send(TelegramOutboundMessage message) {
    SendMessage request = new SendMessage(message.chatId(), message.text());
    if (!message.buttons().isEmpty()) {
      InlineKeyboardRow row = new InlineKeyboardRow();
      for (TelegramActionButton button : message.buttons()) {
        InlineKeyboardButton sdkButton = new InlineKeyboardButton(button.label());
        sdkButton.setCallbackData(button.actionId());
        row.add(sdkButton);
      }
      request.setReplyMarkup(new InlineKeyboardMarkup(List.of(row)));
    }
    try {
      client.execute(request);
    } catch (Exception exception) {
      throw new IllegalStateException("Telegram delivery failed", exception);
    }
  }

  private static void receive(Update update, TelegramChannel channel) {
    if (update == null || update.getUpdateId() == null) return;
    if (update.hasMessage()) {
      Message message = update.getMessage();
      if (message == null
          || !message.hasText()
          || message.getFrom() == null
          || message.getFrom().getId() == null) return;
      TelegramInboundMapper.text(
              new TelegramInboundUpdate(
                  String.valueOf(update.getUpdateId()),
                  String.valueOf(message.getFrom().getId()),
                  String.valueOf(message.getChatId()),
                  message.isUserMessage(),
                  message.getText(),
                  null))
          .ifPresent(channel::receive);
    } else if (update.hasCallbackQuery()) {
      CallbackQuery callback = update.getCallbackQuery();
      if (callback == null
          || callback.getFrom() == null
          || callback.getFrom().getId() == null
          || callback.getMessage() == null) return;
      TelegramInboundMapper.callback(
              new TelegramInboundUpdate(
                  String.valueOf(update.getUpdateId()),
                  String.valueOf(callback.getFrom().getId()),
                  String.valueOf(callback.getMessage().getChatId()),
                  callback.getMessage().isUserMessage(),
                  null,
                  callback.getData()))
          .ifPresent(channel::receive);
    }
  }

  @Override
  public void close() {
    closeQuietly(application);
  }

  private static void closeQuietly(TelegramBotsLongPollingApplication application) {
    try {
      application.close();
    } catch (Exception ignored) {
      // A failed shutdown must not conceal the startup/delivery failure which caused it.
    }
  }
}
