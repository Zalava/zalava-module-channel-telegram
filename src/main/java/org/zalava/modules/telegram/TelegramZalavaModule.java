package org.zalava.modules.telegram;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.zalava.ModuleConfigurationDescriptor;
import org.zalava.ModuleDescriptor;
import org.zalava.ProviderFactory;
import org.zalava.ZalavaModule;
import org.zalava.channels.ZalavaChannel;

/** Module entry point. Core owns identity linking, authorization and conversation state. */
public final class TelegramZalavaModule implements ZalavaModule {
  public static final String MODULE_ID = "zalava-module-channel-telegram";
  private final TelegramChannel channel;

  public TelegramZalavaModule() {
    this(new TelegramChannel());
  }

  TelegramZalavaModule(TelegramChannel channel) {
    this.channel = channel;
  }

  @Override
  public ModuleDescriptor descriptor() {
    return new ModuleDescriptor(
        MODULE_ID, version(), "Telegram channel", "Telegram semantic channel adapter");
  }

  @Override
  public List<ProviderFactory> providerFactories() {
    return List.of();
  }

  @Override
  public ModuleConfigurationDescriptor configuration() {
    return new ModuleConfigurationDescriptor(
        Map.of(
            "type",
            "object",
            "additionalProperties",
            false,
            "properties",
            Map.of("botTokenRef", Map.of("type", "string", "x-secret-reference", true))));
  }

  @Override
  public List<ZalavaChannel> channels() {
    return List.of(channel);
  }

  static String version() {
    Properties properties = new Properties();
    try (InputStream input = TelegramZalavaModule.class.getResourceAsStream("/module.properties")) {
      if (input == null) throw new IllegalStateException("Missing module version metadata");
      properties.load(input);
      return properties.getProperty("module.version");
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read module version metadata", exception);
    }
  }
}
