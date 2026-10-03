package org.zalava.modules.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.zalava.api.testing.ModuleContractKit;

class TelegramChannelTest {
  @Test
  void builtArtifactDeclaresTheHostRuntimeCompatibilityRatherThanSdkVersion() throws Exception {
    try (var jar = new java.util.jar.JarFile(System.getProperty("module.artifact"));
        var metadata = jar.getInputStream(jar.getJarEntry("module-metadata.yaml"))) {
      assertThat(new String(metadata.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
          .contains("zalavaRuntime: \">=1.0.0 <2.0.0\"")
          .doesNotContain("zalavaRuntime: \">=0.1.0-alpha.");
    }
  }

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
}
