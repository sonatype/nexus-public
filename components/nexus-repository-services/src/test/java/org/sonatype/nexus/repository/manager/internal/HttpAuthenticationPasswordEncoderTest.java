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
package org.sonatype.nexus.repository.manager.internal;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.security.UserIdHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.httpclient.config.AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION;

public class HttpAuthenticationPasswordEncoderTest
    extends TestSupport
{
  public static final String PASSWORD = "password";

  @Mock
  private SecretsService secretService;

  @Mock
  private Secret aSecret;

  private HttpAuthenticationPasswordEncoder underTest;

  private MockedStatic<UserIdHelper> userIdHelperMock;

  @Before
  public void setUp() throws Exception {
    userIdHelperMock = mockStatic(UserIdHelper.class);
    userIdHelperMock.when(UserIdHelper::get).thenReturn("userId");

    underTest = new HttpAuthenticationPasswordEncoder(secretService);
  }

  @After
  public void tearDown() {
    userIdHelperMock.close();
  }

  @Test
  public void shouldEncodePasswordSecret() {
    final Map<String, Map<String, Object>> attributes = getAttributes("password", PASSWORD);
    mockEncrypt(PASSWORD);

    underTest.encodeHttpAuthPassword(attributes);

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, PASSWORD.toCharArray(),
            UserIdHelper.get());
  }


  @Test
  public void shouldEncodeWhenPasswordAreDifferent() {
    mockEncrypt(PASSWORD);

    underTest.encodeHttpAuthPassword(getAttributes("password", null), getAttributes("password", PASSWORD));

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, PASSWORD.toCharArray(),
            UserIdHelper.get());

    String newPassword = "NEW_PASSWORD";
    mockEncrypt(newPassword);

    underTest.encodeHttpAuthPassword(getAttributes("password", PASSWORD), getAttributes("password", newPassword));

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, newPassword.toCharArray(),
            UserIdHelper.get());
  }

  @Test
  public void shouldNotEncodePasswordWhenUnchanged() {
    mockEncrypt(PASSWORD);

    underTest.encodeHttpAuthPassword(getAttributes("password", PASSWORD), getAttributes("password", PASSWORD));

    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, PASSWORD.toCharArray(),
            UserIdHelper.get());
  }

  @Test
  public void shouldRemovePasswordSecretWhenNewPasswordNotPresent() {
    mockEncrypt(PASSWORD);
    when(secretService.from(PASSWORD)).thenReturn(aSecret);

    underTest.removeSecret(getAttributes("password", PASSWORD), getAttributes("password", null));

    verify(secretService).remove(aSecret);

    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, PASSWORD.toCharArray(),
            UserIdHelper.get());
  }

  @Test
  public void shouldRemovePasswordSecretWhenDifferent() {
    String newPassword = "NEW_PASSWORD";
    when(secretService.from(PASSWORD)).thenReturn(aSecret);

    underTest.removeSecret(getAttributes("password", PASSWORD), getAttributes("password", newPassword));

    verify(secretService)
        .remove(aSecret);
  }
  @Test
  public void shouldNotRemovePasswordSecretWhenNoPreviousSecret() {
    mockEncrypt(PASSWORD);

    underTest.removeSecret(getAttributes("password", null), getAttributes("password", PASSWORD));

    verify(secretService, never())
        .remove(aSecret);
  }

  @Test
  public void shouldRemovePasswordSecret() {
    final Map<String, Map<String, Object>> attributes = getAttributes("password", PASSWORD);
    when(secretService.from(PASSWORD)).thenReturn(aSecret);

    underTest.removeSecret(attributes);

    verify(secretService)
        .remove(aSecret);
  }

  private void mockEncrypt(final String password) {
    when(secretService.encryptMaven(AUTHENTICATION_CONFIGURATION, password.toCharArray(),
        UserIdHelper.get())).thenReturn(aSecret);
  }

  private static Map<String, Map<String, Object>> getAttributes(
      final String authSecretKey,
      final String authSecretValue)
  {
    Map<String, Object> authentication = new HashMap<>();
    authentication.put(authSecretKey, authSecretValue);

    Map<String, Object> httpClient = new HashMap<>();
    httpClient.put("authentication", authentication);

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("httpclient", httpClient);

    return attributes;
  }
}
