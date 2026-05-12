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
import java.util.Optional;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.security.UserIdHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.httpclient.config.AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION;
import static org.sonatype.nexus.repository.manager.internal.HttpAuthenticationSecretEncoder.BEARER_TOKEN_MIGRATION_STARTED;

@ExtendWith(MockitoExtension.class)
class HttpAuthenticationSecretEncoderTest
{
  public static final String PASSWORD = "password";

  public static final String BEARER_TOKEN = "bearerToken";

  public static final String BEARER_TOKEN_ID = "bearerTokenId";

  @Mock
  private SecretsService secretService;

  @Mock
  private Secret aSecret;

  @Mock
  private KeyValueStore keyValueStore;

  private HttpAuthenticationSecretEncoder underTest;

  private MockedStatic<UserIdHelper> userIdHelperMock;

  @BeforeEach
  void setUp() {
    userIdHelperMock = mockStatic(UserIdHelper.class);
    userIdHelperMock.when(UserIdHelper::get).thenReturn("userId");

    underTest = new HttpAuthenticationSecretEncoder(secretService, keyValueStore);
  }

  @AfterEach
  public void tearDown() {
    userIdHelperMock.close();
  }

  @Test
  void shouldEncodePasswordSecret() {
    final Map<String, Map<String, Object>> attributes = getAttributes("password", PASSWORD);
    mockEncrypt(PASSWORD);

    underTest.encodeHttpAuthPassword(attributes);

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, PASSWORD.toCharArray(),
            UserIdHelper.get());
  }

  @Test
  void shouldHandleSecretObjectWithoutEncrypting() {
    // Arrange - password value is already a Secret object (from import)
    final Map<String, Map<String, Object>> attributes = getAttributesWithSecret("password", aSecret);
    when(aSecret.getId()).thenReturn("_123");

    // Act
    underTest.encodeHttpAuthPassword(attributes);

    // Assert - should NOT call encryptMaven since it's already a Secret
    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, PASSWORD.toCharArray(),
            UserIdHelper.get());

    // Assert - should have called getId() to extract the Secret ID
    verify(aSecret).getId();
  }

  @Test
  void shouldEncodeWhenPasswordAreDifferent() {
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
  void shouldNotEncodePasswordWhenUnchanged() {
    final String encryptedPassword = "_123";
    mockEncrypt(encryptedPassword);

    underTest.encodeHttpAuthPassword(getAttributes("password", encryptedPassword),
        getAttributes("password", encryptedPassword));

    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, encryptedPassword.toCharArray(),
            UserIdHelper.get());
  }

  @Test
  void shouldRemovePasswordSecretWhenNewPasswordNotPresent() {
    mockEncrypt(PASSWORD);
    when(secretService.from(PASSWORD)).thenReturn(aSecret);

    underTest.removeSecret(getAttributes("password", PASSWORD), getAttributes("password", null));

    verify(secretService).remove(aSecret);

    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, PASSWORD.toCharArray(),
            UserIdHelper.get());
  }

  @Test
  void shouldRemovePasswordSecretWhenDifferent() {
    String newPassword = "NEW_PASSWORD";
    when(secretService.from(PASSWORD)).thenReturn(aSecret);

    underTest.removeSecret(getAttributes("password", PASSWORD), getAttributes("password", newPassword));

    verify(secretService)
        .remove(aSecret);
  }

  @Test
  void shouldNotRemovePasswordSecretWhenNoPreviousSecret() {
    mockEncrypt(PASSWORD);

    underTest.removeSecret(getAttributes("password", null), getAttributes("password", PASSWORD));

    verify(secretService, never())
        .remove(aSecret);
  }

  @Test
  void shouldRemovePasswordSecret() {
    final Map<String, Map<String, Object>> attributes = getAttributes("password", PASSWORD);
    when(secretService.from(PASSWORD)).thenReturn(aSecret);

    underTest.removeSecret(attributes);

    verify(secretService)
        .remove(aSecret);
  }

  @Test
  void shouldMoveBearerTokenIdToBearerTokenPreMigration() {
    final String token = "my-bearer-token";
    final Map<String, Map<String, Object>> attributes = getAttributes(BEARER_TOKEN_ID, token);

    underTest.encodeHttpAuthPassword(attributes);

    Map<String, Object> auth = (Map<String, Object>) attributes.get("httpclient").get("authentication");
    // Pre-migration: bearerTokenId moved to bearerToken (plain text, no encryption)
    assertThat(auth.get(BEARER_TOKEN), is(equalTo(token)));
    assertThat(auth.containsKey(BEARER_TOKEN_ID), is(false));

    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, token.toCharArray(), UserIdHelper.get());
  }

  @Test
  void shouldEncodeBearerTokenSecretPostMigration() {
    when(keyValueStore.getBoolean(BEARER_TOKEN_MIGRATION_STARTED)).thenReturn(Optional.of(true));

    final String token = "my-bearer-token";
    final Map<String, Map<String, Object>> attributes = getAttributes(BEARER_TOKEN_ID, token);
    mockEncrypt(token);

    underTest.encodeHttpAuthPassword(attributes);

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, token.toCharArray(), UserIdHelper.get());
  }

  @Test
  void shouldEncodeWhenBearerTokenAreDifferentPostMigration() {
    when(keyValueStore.getBoolean(BEARER_TOKEN_MIGRATION_STARTED)).thenReturn(Optional.of(true));

    final String token = "my-bearer-token";
    mockEncrypt(token);

    underTest.encodeHttpAuthPassword(getAttributes(BEARER_TOKEN_ID, null), getAttributes(BEARER_TOKEN_ID, token));

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, token.toCharArray(), UserIdHelper.get());

    String newToken = "NEW_BEARER_TOKEN";
    mockEncrypt(newToken);

    underTest.encodeHttpAuthPassword(getAttributes(BEARER_TOKEN_ID, token), getAttributes(BEARER_TOKEN_ID, newToken));

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, newToken.toCharArray(), UserIdHelper.get());
  }

  @Test
  void shouldNotEncodeBearerTokenWhenUnchangedPostMigration() {
    when(keyValueStore.getBoolean(BEARER_TOKEN_MIGRATION_STARTED)).thenReturn(Optional.of(true));

    final String encryptedToken = "_456";
    mockEncrypt(encryptedToken);

    underTest.encodeHttpAuthPassword(getAttributes(BEARER_TOKEN_ID, encryptedToken),
        getAttributes(BEARER_TOKEN_ID, encryptedToken));

    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, encryptedToken.toCharArray(), UserIdHelper.get());
  }

  @Test
  void shouldRemoveBearerTokenIdSecretWhenNewTokenNotPresent() {
    // removeSecret only handles bearerTokenId (encrypted secrets), not bearerToken (plain text)
    final String token = "my-bearer-token";
    mockEncrypt(token);
    when(secretService.from(token)).thenReturn(aSecret);

    underTest.removeSecret(getAttributes(BEARER_TOKEN_ID, token), getAttributes(BEARER_TOKEN_ID, null));

    verify(secretService).remove(aSecret);

    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, token.toCharArray(), UserIdHelper.get());
  }

  @Test
  void shouldRemoveBearerTokenIdSecretWhenDifferent() {
    final String token = "my-bearer-token";
    String newToken = "NEW_BEARER_TOKEN";
    when(secretService.from(token)).thenReturn(aSecret);

    underTest.removeSecret(getAttributes(BEARER_TOKEN_ID, token), getAttributes(BEARER_TOKEN_ID, newToken));

    verify(secretService).remove(aSecret);
  }

  @Test
  void shouldNotRemoveBearerTokenIdSecretWhenNoPreviousSecret() {
    final String token = "my-bearer-token";
    mockEncrypt(token);

    underTest.removeSecret(getAttributes(BEARER_TOKEN_ID, null), getAttributes(BEARER_TOKEN_ID, token));

    verify(secretService, never()).remove(aSecret);
  }

  @Test
  void shouldRemoveBearerTokenIdSecret() {
    when(keyValueStore.getBoolean(BEARER_TOKEN_MIGRATION_STARTED)).thenReturn(Optional.of(true));
    final String token = "my-bearer-token";
    final Map<String, Map<String, Object>> attributes = getAttributes(BEARER_TOKEN_ID, token);
    when(secretService.from(token)).thenReturn(aSecret);

    underTest.removeSecret(attributes);

    verify(secretService).remove(aSecret);
  }

  @Test
  void shouldNotRemoveBearerTokenPlainText() {
    // bearerToken (plain text, pre-migration) should NOT trigger secret removal
    final String token = "my-bearer-token";
    final Map<String, Map<String, Object>> attributes = getAttributes(BEARER_TOKEN, token);

    underTest.removeSecret(attributes);

    verify(secretService, never()).remove(aSecret);
  }

  @Test
  void shouldNotEncryptPasswordWhenAlreadyEncrypted() {
    final String encryptedSecretId = "_123";
    mockEncrypt(encryptedSecretId);

    underTest.encodeHttpAuthPassword(
        getAttributes("password", encryptedSecretId),
        getAttributes("password", encryptedSecretId));

    verify(secretService, never())
        .encryptMaven(AUTHENTICATION_CONFIGURATION, encryptedSecretId.toCharArray(),
            UserIdHelper.get());
  }

  @Test
  void shouldEncryptWhenChangingFromPlainToNewPlain() {
    final String oldPlainPassword = "oldPassword";
    final String newPlainPassword = "newPassword";
    mockEncrypt(newPlainPassword);

    underTest.encodeHttpAuthPassword(
        getAttributes("password", oldPlainPassword),
        getAttributes("password", newPlainPassword));

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, newPlainPassword.toCharArray(),
            UserIdHelper.get());
  }

  @Test
  void shouldEncryptWhenChangingFromEncryptedToNewPlain() {
    final String oldEncryptedPassword = "_123";
    final String newPlainPassword = "newPassword";
    when(aSecret.getId()).thenReturn("_789");
    mockEncrypt(newPlainPassword);

    underTest.encodeHttpAuthPassword(
        getAttributes("password", oldEncryptedPassword),
        getAttributes("password", newPlainPassword));

    verify(secretService)
        .encryptMaven(AUTHENTICATION_CONFIGURATION, newPlainPassword.toCharArray(),
            UserIdHelper.get());
  }

  private void mockEncrypt(final String password) {
    lenient().when(secretService.encryptMaven(AUTHENTICATION_CONFIGURATION, password.toCharArray(),
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

  private static Map<String, Map<String, Object>> getAttributesWithSecret(
      final String authSecretKey,
      final Secret authSecretValue)
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
