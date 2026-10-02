package org.zalava.modules.telegram;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.zalava.channels.*;

class TelegramBoundaryTest {
  @Test
  void rejectsIncompleteInboundUpdatesAndInvalidOutboundMessages() {
    for (String bad : Arrays.asList(null, " ")) {
      for (int field = 0; field < 4; field++) {
        String[] values = {"1", "2", "3", "text"};
        values[field] = bad;
        assertThat(
                TelegramInboundMapper.text(
                    new TelegramInboundUpdate(
                        values[0], values[1], values[2], true, values[3], null)))
            .isEmpty();
        assertThat(
                TelegramInboundMapper.callback(
                    new TelegramInboundUpdate(
                        values[0], values[1], values[2], true, null, values[3])))
            .isEmpty();
      }
      assertThatThrownBy(() -> new TelegramOutboundMessage(bad, "text", null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new TelegramOutboundMessage("chat", bad, null))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(
            TelegramInboundMapper.text(
                new TelegramInboundUpdate("1", "2", "3", false, "text", null)))
        .isEmpty();
    assertThat(
            TelegramInboundMapper.callback(
                new TelegramInboundUpdate("1", "2", "3", false, null, "action")))
        .isEmpty();
    assertThat(new TelegramOutboundMessage("chat", "text", null).buttons()).isEmpty();
    assertThatThrownBy(
            () ->
                new UnsupportedTelegramDeliveryGateway()
                    .send(new TelegramOutboundMessage("chat", "text", List.of())))
        .isInstanceOf(IllegalStateException.class);
    new UnsupportedTelegramDeliveryGateway().close();
  }

  @Test
  void requiresScopedSecretsAndWipesTheResolvedToken() throws Exception {
    var channel = new TelegramChannel();
    for (Object value : List.of(" ", 42))
      assertThatThrownBy(
              () -> channel.start(new ChannelTransportContext(Map.of("botTokenRef", value), null)))
          .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> channel.start(ChannelTransportContext.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> channel.start(new ChannelTransportContext(Map.of("botTokenRef", "ref"), null)))
        .isInstanceOf(IllegalStateException.class);
    char[] token = "secret".toCharArray();
    try (var clients = mockConstruction(OkHttpTelegramClient.class);
        var apps = mockConstruction(TelegramBotsLongPollingApplication.class)) {
      channel.start(
          new ChannelTransportContext(Map.of("botTokenRef", "ref"), name -> Optional.of(token)));
      assertThat(token).containsOnly('\0');
      channel.close();
      verify(apps.constructed().getFirst()).close();
    }
    assertThatThrownBy(() -> channel.bind(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> channel.connect(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> channel.deliver(null)).isInstanceOf(NullPointerException.class);
    channel.receive(null);
  }

  @Test
  void translatesSdkUpdatesAndDeliveryWithoutNetwork() throws Exception {
    AtomicReference<LongPollingSingleThreadUpdateConsumer> receiver = new AtomicReference<>();
    List<IncomingInteraction> received = new ArrayList<>();
    var channel = new TelegramChannel();
    channel.bind(received::add);
    try (var clients = mockConstruction(OkHttpTelegramClient.class);
        var apps =
            mockConstruction(
                TelegramBotsLongPollingApplication.class,
                (app, ctx) ->
                    doAnswer(
                            call -> {
                              receiver.set(call.getArgument(1));
                              return null;
                            })
                        .when(app)
                        .registerBot(
                            anyString(), any(LongPollingSingleThreadUpdateConsumer.class)))) {
      var transport = TelegramSdkTransport.start("token", channel);
      transport.send(new TelegramOutboundMessage("3", "text", List.of()));
      transport.send(
          new TelegramOutboundMessage(
              "3", "approve", List.of(new TelegramActionButton("Approve", "action"))));
      verify(clients.constructed().getFirst(), times(2)).execute(any(SendMessage.class));
      doThrow(new IllegalStateException("failed"))
          .when(clients.constructed().getFirst())
          .execute(any(SendMessage.class));
      assertThatThrownBy(() -> transport.send(new TelegramOutboundMessage("3", "text", List.of())))
          .isInstanceOf(IllegalStateException.class);
      Update update = mock(Update.class);
      receiver.get().consume((Update) null);
      receiver.get().consume(update);
      when(update.getUpdateId()).thenReturn(1);
      receiver.get().consume(update);
      when(update.hasMessage()).thenReturn(true);
      Message message = mock(Message.class);
      when(update.getMessage()).thenReturn(message);
      receiver.get().consume(update);
      when(message.hasText()).thenReturn(true);
      receiver.get().consume(update);
      User user = mock(User.class);
      when(message.getFrom()).thenReturn(user);
      receiver.get().consume(update);
      when(user.getId()).thenReturn(2L);
      when(message.getChatId()).thenReturn(3L);
      when(message.isUserMessage()).thenReturn(true);
      when(message.getText()).thenReturn("hello");
      receiver.get().consume(update);
      when(update.hasMessage()).thenReturn(false);
      when(update.hasCallbackQuery()).thenReturn(true);
      CallbackQuery callback = mock(CallbackQuery.class);
      when(update.getCallbackQuery()).thenReturn(callback);
      receiver.get().consume(update);
      when(callback.getFrom()).thenReturn(user);
      receiver.get().consume(update);
      when(callback.getMessage()).thenReturn(message);
      when(callback.getData()).thenReturn("approve");
      receiver.get().consume(update);
      assertThat(received).hasSize(2);
      transport.close();
      assertThatThrownBy(() -> channel.connect(mock(TelegramDeliveryGateway.class)))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void rejectsBadStartupAndPreservesStartupFailureDuringCleanup() throws Exception {
    for (String token : Arrays.asList(null, " "))
      assertThatThrownBy(() -> TelegramSdkTransport.start(token, new TelegramChannel()))
          .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> TelegramSdkTransport.start("token", null))
        .isInstanceOf(NullPointerException.class);
    try (var clients = mockConstruction(OkHttpTelegramClient.class);
        var apps =
            mockConstruction(
                TelegramBotsLongPollingApplication.class,
                (app, ctx) -> {
                  doThrow(new IllegalStateException("start failure"))
                      .when(app)
                      .registerBot(anyString(), any(LongPollingSingleThreadUpdateConsumer.class));
                  doThrow(new IllegalStateException("close failure")).when(app).close();
                })) {
      assertThatThrownBy(() -> TelegramSdkTransport.start("token", new TelegramChannel()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("start");
      verify(apps.constructed().getFirst()).close();
    }
  }
}
