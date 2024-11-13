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
package org.sonatype.nexus.internal.httpclient

import org.sonatype.goodies.common.Time
import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.crypto.secrets.Secret
import org.sonatype.nexus.crypto.secrets.SecretDeserializer
import org.sonatype.nexus.crypto.secrets.SecretsFactory
import org.sonatype.nexus.datastore.mybatis.OverrideIgnoreTypeIntrospector
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.common.collect.ImmutableList
import groovy.transform.ToString
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Mockito.when

/**
 * Tests for {@link AuthenticationConfigurationDeserializer}.
 */
class AuthenticationConfigurationDeserializerTest
    extends TestSupport
{
  private ObjectMapper objectMapper

  @Mock
  private Secret secret

  @Mock
  private SecretsFactory secretsFactory


  @Before
  void setUp() {
    objectMapper = new ObjectMapper()
        .setAnnotationIntrospector(new OverrideIgnoreTypeIntrospector(ImmutableList.of(Secret.class)))
        .registerModule(
        new SimpleModule().addSerializer(
            Time.class,
            new SecondsSerializer()
        ).addDeserializer(
            Time.class,
            new SecondsDeserializer()
        ).addSerializer(
            AuthenticationConfiguration.class,
            new AuthenticationConfigurationSerializer()
        ).addDeserializer(
            AuthenticationConfiguration.class,
            new AuthenticationConfigurationDeserializer()
        ).addDeserializer(Secret.class, new SecretDeserializer(secretsFactory))
    )
  }

  @ToString
  static class AuthContainer
  {
    AuthenticationConfiguration auth
  }

  @Test
  void 'read username'() {
    when(secret.getId()).thenReturn('admin123')
    when(secretsFactory.from('admin123')).thenReturn(secret)

    def example = new AuthContainer(auth:
        new UsernameAuthenticationConfiguration(username: 'admin', password: secret)
    )

    def json = objectMapper.writeValueAsString(example)
    log json

    assert json.contains('username')
    assert json.contains('admin')
    assert json.contains('password')

    def obj = objectMapper.readValue(json, AuthContainer.class)
    log obj

    assert obj != null
    assert obj.auth != null
    assert obj.auth instanceof UsernameAuthenticationConfiguration
    assert obj.auth.type == UsernameAuthenticationConfiguration.TYPE

    UsernameAuthenticationConfiguration target = obj.auth as UsernameAuthenticationConfiguration
    assert target.username == 'admin'
    assert target.password == secret
  }

  @Test(expected = JsonMappingException.class)
  void 'invalid type'() {
    def json = '{"auth":{"type":"invalid"}}'
    objectMapper.readValue(json, AuthContainer.class)
  }
}
