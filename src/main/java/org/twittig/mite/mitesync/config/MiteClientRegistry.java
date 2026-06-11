package org.twittig.mite.mitesync.config;

import io.seventytwo.oss.mite.MiteClient;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Resolves a {@link MiteClient} by instance key. Profiles reference the two configured Mite
 * instances ("source" / "target") by name instead of being wired to a fixed bean.
 */
@Component
public class MiteClientRegistry {

  private final Map<String, MiteClient> clients;

  public MiteClientRegistry(MiteClient sourceMiteClient, MiteClient targetMiteClient) {
    this.clients = Map.of("source", sourceMiteClient, "target", targetMiteClient);
  }

  public MiteClient get(String instanceKey) {
    MiteClient client = clients.get(instanceKey);
    if (client == null) {
      throw new IllegalArgumentException(
          "Unknown Mite instance '" + instanceKey + "' (expected one of " + clients.keySet() + ")");
    }
    return client;
  }
}
