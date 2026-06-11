package org.twittig.mite.mitesync;

import io.seventytwo.oss.mite.MiteClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.twittig.mite.mitesync.config.DailyReportProperties;

@Configuration
@EnableConfigurationProperties(DailyReportProperties.class)
public class MiteSyncConfig {

  @Value("${mite-sync.source.host}")
  private String sourceMiteHost;

  @Value("${mite-sync.source.api-key}")
  private String sourceMiteApiKey;

  @Value("${mite-sync.target.host}")
  private String targetMiteHost;

  @Value("${mite-sync.target.api-key}")
  private String targetMiteApiKey;

  @Bean
  public MiteClient sourceMiteClient() {
    return new MiteClient(sourceMiteHost, sourceMiteApiKey);
  }

  @Bean
  public MiteClient targetMiteClient() {
    return new MiteClient(targetMiteHost, targetMiteApiKey);
  }
}
