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

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretDeserializer;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.datastore.mybatis.OverrideIgnoreTypeIntrospector;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.kv.KeyValueStore;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthenticationConfigurationDeserializer}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AuthenticationConfigurationDeserializerTest
{
  private static final Logger log = LoggerFactory.getLogger(AuthenticationConfigurationDeserializerTest.class);

  private ObjectMapper objectMapper;

  @Mock
  private Secret secret;

  @Mock
  private SecretsFactory secretsFactory;

  @Mock
  private KeyValueStore keyValueStore;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper()
        .setAnnotationIntrospector(new OverrideIgnoreTypeIntrospector(ImmutableList.of(Secret.class)))
        .registerModule(
            new SimpleModule().addSerializer(
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
                .addDeserializer(Secret.class, new SecretDeserializer(secretsFactory)));
  }

  public static class AuthContainer
  {
    public AuthenticationConfiguration auth;
  }

  @Test
  public void readUsername() throws Exception {
    when(secret.getId()).thenReturn("admin123");
    when(secretsFactory.from("admin123")).thenReturn(secret);

    AuthContainer example = new AuthContainer();
    UsernameAuthenticationConfiguration original = new UsernameAuthenticationConfiguration();
    original.setUsername("admin");
    original.setPassword(secret);
    example.auth = original;

    String json = objectMapper.writeValueAsString(example);
    log.info(json);

    assertTrue(json.contains("username"));
    assertTrue(json.contains("admin"));
    assertTrue(json.contains("password"));

    AuthContainer obj = objectMapper.readValue(json, AuthContainer.class);
    log.info("{}", obj);

    assertNotNull(obj);
    assertNotNull(obj.auth);
    assertTrue(obj.auth instanceof UsernameAuthenticationConfiguration);
    assertTrue(obj.auth.getType().equals(UsernameAuthenticationConfiguration.TYPE));

    UsernameAuthenticationConfiguration target = (UsernameAuthenticationConfiguration) obj.auth;
    assertTrue(target.getUsername().equals("admin"));
    assertTrue(target.getPassword().equals(secret));
  }

  @Test(expected = JsonMappingException.class)
  public void invalidType() throws Exception {
    String json = "{\"auth\":{\"type\":\"invalid\"}}";
    objectMapper.readValue(json, AuthContainer.class);
  }
}
