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
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl
import org.sonatype.nexus.crypto.internal.MavenCipherImpl
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration
import org.sonatype.nexus.security.PasswordHelper

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import groovy.transform.ToString
import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link AuthenticationConfigurationDeserializer}.
 */
class AuthenticationConfigurationDeserializerTest
    extends TestSupport
{
  private ObjectMapper objectMapper

  @Before
  void setUp() {
    final PasswordHelper passwordHelper = new PasswordHelper(new MavenCipherImpl(new CryptoHelperImpl()))
    objectMapper = new ObjectMapper().registerModule(
        new SimpleModule().addSerializer(
            Time.class,
            new SecondsSerializer()
        ).addDeserializer(
            Time.class,
            new SecondsDeserializer()
        ).addSerializer(
            AuthenticationConfiguration.class,
            new AuthenticationConfigurationSerializer(passwordHelper)
        ).addDeserializer(
            AuthenticationConfiguration.class,
            new AuthenticationConfigurationDeserializer(passwordHelper)
        )
    )
  }

  @ToString
  static class AuthContainer
  {
    AuthenticationConfiguration auth
  }

  @Test
  void 'read username'() {
    def example = new AuthContainer(auth:
        new UsernameAuthenticationConfiguration(username: 'admin', password: 'admin123')
    )

    def json = objectMapper.writeValueAsString(example)
    log json

    assert json.contains('username')
    assert json.contains('admin')
    assert json.contains('password')
    assert !json.contains('admin123')

    def obj = objectMapper.readValue(json, AuthContainer.class)
    log obj

    assert obj != null
    assert obj.auth != null
    assert obj.auth instanceof UsernameAuthenticationConfiguration
    assert obj.auth.type == UsernameAuthenticationConfiguration.TYPE

    UsernameAuthenticationConfiguration target = obj.auth as UsernameAuthenticationConfiguration
    assert target.username == 'admin'
    assert target.password == 'admin123'
  }

  @Test(expected = JsonMappingException.class)
  void 'invalid type'() {
    def json = '{"auth":{"type":"invalid"}}'
    objectMapper.readValue(json, AuthContainer.class)
  }
}
