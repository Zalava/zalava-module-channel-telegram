package org.zalava.modules.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.zalava.api.extensions.channels.ApprovalOperation;
import org.zalava.api.extensions.channels.ApprovalRequest;
import org.zalava.api.extensions.channels.ChannelDestination;
import org.zalava.api.extensions.channels.ChannelEvent;
import org.zalava.api.extensions.channels.ChannelPrivacy;
import org.zalava.api.testing.ModuleContractKit;

class TelegramChannelTest {
  @Test
  void loadsTheBuiltArtifactAndDeclaresTelegramChannel() throws Exception {
    try (ModuleContractKit kit =
        ModuleContractKit.load(
            Path.of(System.getProperty("module.artifact")),
            List.of(),
            TelegramZalavaModule.MODULE_ID,
            System.getProperty("module.version"))) {
      assertThat(kit.module().channels()).hasSize(1);
      assertThat(kit.module().channels().getFirst().descriptor().channelId()).isEqualTo("telegram");
      assertThat(kit.module().channels().getFirst().descriptor().capabilities().approvalPrompts())
          .isTrue();
      assertThat(kit.module().configuration().jsonSchema()).containsKey("properties");
    }
  }

  @Test
  void mapsOnlyPrivateNumericIdentityTextAndOpaqueCallbacks() {
    var text =
        TelegramInboundMapper.text(
            new TelegramInboundUpdate("99", "123456", "123456", true, " hello ", null));
    assertThat(text)
        .hasValueSatisfying(
            value -> {
              assertThat(value.identity().subject()).isEqualTo("123456");
              assertThat(value.destination().privacy()).isEqualTo(ChannelPrivacy.PRIVATE);
              assertThat(value.input())
                  .isEqualTo(new org.zalava.api.extensions.channels.ChannelInput.Text("hello"));
            });
    assertThat(
            TelegramInboundMapper.text(
                new TelegramInboundUpdate("100", "123456", "-100", false, "hello", null)))
        .isEmpty();
    assertThat(
            TelegramInboundMapper.callback(
                new TelegramInboundUpdate(
                    "101", "123456", "123456", true, null, "core-action-opaque")))
        .hasValueSatisfying(
            value ->
                assertThat(value.input())
                    .isEqualTo(
                        new org.zalava.api.extensions.channels.ChannelInput.Action(
                            "core-action-opaque", java.util.Map.of())));
  }

  @Test
  void rendersApprovalAsOpaqueButtonsAndRejectsOtherChannelDelivery() {
    var gateway = new RecordingGateway();
    var channel = new TelegramChannel(gateway);
    var destination = new ChannelDestination("telegram", "123456", ChannelPrivacy.PRIVATE);
    channel.deliver(
        new ChannelEvent.ApprovalPrompt(
            destination,
            new ApprovalRequest(
                "request",
                "actor",
                new ApprovalOperation("tool", "target", java.util.Map.of()),
                "Run tool?",
                "opaque-yes",
                "opaque-no"),
            "correlation"));
    assertThat(gateway.messages)
        .singleElement()
        .satisfies(
            message -> {
              assertThat(message.buttons())
                  .extracting(TelegramActionButton::actionId)
                  .containsExactly("opaque-yes", "opaque-no");
            });
    assertThatThrownBy(
            () ->
                channel.deliver(
                    new ChannelEvent.Text(
                        new ChannelDestination("other", "x", ChannelPrivacy.PRIVATE),
                        "no",
                        "correlation")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static final class RecordingGateway implements TelegramDeliveryGateway {
    private final List<TelegramOutboundMessage> messages = new ArrayList<>();

    @Override
    public void send(TelegramOutboundMessage message) {
      messages.add(message);
    }
  }
}
