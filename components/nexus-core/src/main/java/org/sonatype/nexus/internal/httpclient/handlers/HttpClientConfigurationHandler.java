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
package org.sonatype.nexus.internal.httpclient.handlers;

import java.util.function.Supplier;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretDeserializer;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.datastore.mybatis.AbstractJsonTypeHandler;
import org.sonatype.nexus.datastore.mybatis.OverrideIgnoreTypeIntrospector;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.internal.httpclient.AuthenticationConfigurationDeserializer;
import org.sonatype.nexus.internal.httpclient.AuthenticationConfigurationSerializer;
import org.sonatype.nexus.internal.httpclient.SecondsDeserializer;
import org.sonatype.nexus.internal.httpclient.SecondsSerializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import org.apache.ibatis.type.TypeHandler;

/**
 * MyBatis {@link TypeHandler} that sets up an {@link com.fasterxml.jackson.databind.ObjectMapper} for
 * correct encryption-at-rest of {@link org.sonatype.nexus.httpclient.config.HttpClientConfiguration}s.
 *
 * @since 3.21
 */
public abstract class HttpClientConfigurationHandler<T>
    extends AbstractJsonTypeHandler<T>
{
  // this guarantees the constructor and buildObjectMapper work on the same mapper, regardless which runs first
  private static final ThreadLocal<ObjectMapper> constructingMapper = ThreadLocal.withInitial(ObjectMapper::new);

  protected HttpClientConfigurationHandler(final SecretsFactory secretsFactory) {
    constructingMapper.get().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setAnnotationIntrospector(new OverrideIgnoreTypeIntrospector(ImmutableList.of(Secret.class)))
      // register custom serializers and deserializers
      // - goodies Time is our internal Time representation
      // - AuthenticationConfiguration needs a tiny bit of logic for resolving the proper impl and encryption
      .registerModule(
          new SimpleModule()
              .addSerializer(
                  Time.class,
                  new SecondsSerializer()
              )
              .addDeserializer(
                  Time.class,
                  new SecondsDeserializer()
              )
              .addSerializer(
                  AuthenticationConfiguration.class,
                  new AuthenticationConfigurationSerializer()
              )
              .addDeserializer(
                  AuthenticationConfiguration.class,
                  new AuthenticationConfigurationDeserializer()
              )
              .addDeserializer(Secret.class, new SecretDeserializer(secretsFactory))

      );
  }

  @Override
  protected ObjectMapper buildObjectMapper(final Supplier<ObjectMapper> mapperFactory) {
    // use our custom mapper which re-uses existing serialization/deserialization helpers
    return constructingMapper.get();
  }
}
