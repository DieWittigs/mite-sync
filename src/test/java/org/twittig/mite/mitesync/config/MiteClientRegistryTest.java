package org.twittig.mite.mitesync.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.seventytwo.oss.mite.MiteClient;
import org.junit.jupiter.api.Test;

class MiteClientRegistryTest {

  private final MiteClient source = mock(MiteClient.class);
  private final MiteClient target = mock(MiteClient.class);
  private final MiteClientRegistry registry = new MiteClientRegistry(source, target);

  @Test
  void get_source_returnsSourceClient() {
    assertThat(registry.get("source")).isSameAs(source);
  }

  @Test
  void get_target_returnsTargetClient() {
    assertThat(registry.get("target")).isSameAs(target);
  }

  @Test
  void get_unknownInstance_throws() {
    assertThatThrownBy(() -> registry.get("other"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("other");
  }
}
