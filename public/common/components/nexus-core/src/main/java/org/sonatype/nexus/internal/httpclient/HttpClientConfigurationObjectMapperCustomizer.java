/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.internal.httpclient;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretDeserializer;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.datastore.mybatis.OverrideIgnoreTypeIntrospector;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.repository.config.ConfigurationObjectMapperCustomizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * HTTP-client specific {@link ConfigurationObjectMapperCustomizer} that registers custom deserializer
 * with {@link ObjectMapper}.
 *
 * @see AuthenticationConfigurationDeserializer
 */
@Component
@Singleton
public class HttpClientConfigurationObjectMapperCustomizer
    implements ConfigurationObjectMapperCustomizer
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final SecretsService secretsService;

  private final KeyValueStore keyValueStore;

  @Inject
  public HttpClientConfigurationObjectMapperCustomizer(
      final SecretsService secretsService,
      final KeyValueStore keyValueStore)
  {
    this.secretsService = checkNotNull(secretsService);
    this.keyValueStore = checkNotNull(keyValueStore);
  }

  @Override
  public void customize(final ObjectMapper objectMapper) {
    objectMapper
        .setAnnotationIntrospector(new OverrideIgnoreTypeIntrospector(ImmutableList.of(Secret.class)))
        .registerModule(
            new SimpleModule()
                .addSerializer(
                    Time.class,
                    new SecondsSerializer())
                .addDeserializer(
                    Time.class,
                    new SecondsDeserializer())
                .addSerializer(
                    AuthenticationConfiguration.class,
                    new AuthenticationConfigurationSerializer(keyValueStore))
                .addDeserializer(
                    AuthenticationConfiguration.class,
                    new AuthenticationConfigurationDeserializer())
                .addDeserializer(Secret.class, new SecretDeserializer(secretsService)));
  }
}
