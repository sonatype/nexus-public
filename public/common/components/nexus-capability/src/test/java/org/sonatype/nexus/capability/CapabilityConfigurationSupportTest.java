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
package org.sonatype.nexus.capability;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretData;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.crypto.secrets.SecretsStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link CapabilityConfigurationSupport#decryptSecret}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CapabilityConfigurationSupportTest
{
  @Mock
  private SecretsService secretsService;

  @Mock
  private SecretsStore secretsStore;

  @Mock
  private Secret secret;

  @Mock
  private SecretData secretData;

  private TestCapabilityConfiguration config;

  @Before
  public void setup() {
    config = new TestCapabilityConfiguration();
  }

  @Test
  public void testDecryptSecretWithUnderscorePrefix() {
    String secretId = "_123";
    String decryptedValue = "my-secret";

    when(secretsService.from(secretId)).thenReturn(secret);
    when(secret.decrypt()).thenReturn(decryptedValue.toCharArray());

    String result = config.decryptSecret(secretId, secretsStore, secretsService);

    assertThat(result, is(decryptedValue));
    verify(secretsService).from(secretId);
  }

  @Test
  public void testDecryptSecretWithoutUnderscorePrefix() {
    String secretId = "456";
    String decryptedValue = "another-secret";

    when(secretsService.from(secretId)).thenReturn(secret);
    when(secret.decrypt()).thenReturn(decryptedValue.toCharArray());

    String result = config.decryptSecret(secretId, secretsStore, secretsService);

    assertThat(result, is(decryptedValue));
    verify(secretsService).from(secretId);
  }

  @Test
  public void testDecryptSecretWithUnderscorePrefixMultipleDigits() {
    String secretId = "_9876";
    String decryptedValue = "long-secret-id";

    when(secretsService.from(secretId)).thenReturn(secret);
    when(secret.decrypt()).thenReturn(decryptedValue.toCharArray());

    String result = config.decryptSecret(secretId, secretsStore, secretsService);

    assertThat(result, is(decryptedValue));
    verify(secretsService).from(secretId);
  }

  @Test
  public void testDecryptSecretNotFound() {
    String secretId = "_999";

    when(secretsService.from(secretId)).thenThrow(new RuntimeException("Secret not found"));

    String result = config.decryptSecret(secretId, secretsStore, secretsService);

    assertThat(result, is(nullValue()));
    verify(secretsService).from(secretId);
  }

  @Test
  public void testDecryptSecretWithNullSecretId() {
    String result = config.decryptSecret(null, secretsStore, secretsService);

    assertThat(result, is(nullValue()));
    verify(secretsService, never()).from(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  public void testDecryptSecretWithEmptySecretId() {
    String result = config.decryptSecret("", secretsStore, secretsService);

    assertThat(result, is(""));
    verify(secretsService, never()).from(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  public void testDecryptSecretWithNullSecretsStore() {
    String secretId = "_123";
    String decryptedValue = "my-secret";

    // SecretsStore is not used anymore, so this test just verifies it still works
    when(secretsService.from(secretId)).thenReturn(secret);
    when(secret.decrypt()).thenReturn(decryptedValue.toCharArray());

    String result = config.decryptSecret(secretId, null, secretsService);

    assertThat(result, is(decryptedValue));
  }

  @Test
  public void testDecryptSecretWithNullSecretsService() {
    String secretId = "_123";

    String result = config.decryptSecret(secretId, secretsStore, null);

    assertThat(result, is(secretId));
  }

  @Test
  public void testDecryptSecretWithBothServicesNull() {
    String secretId = "_123";

    String result = config.decryptSecret(secretId, null, null);

    assertThat(result, is(secretId));
  }

  @Test
  public void testDecryptSecretWithInvalidFormat() {
    String secretId = "_abc";

    when(secretsService.from(secretId)).thenThrow(new NumberFormatException("Invalid format"));

    String result = config.decryptSecret(secretId, secretsStore, secretsService);

    // Should return as-is for backwards compatibility
    assertThat(result, is(secretId));
  }

  @Test
  public void testDecryptSecretWithNonNumericValue() {
    String secretId = "not-a-number";

    when(secretsService.from(secretId)).thenThrow(new NumberFormatException("Invalid format"));

    String result = config.decryptSecret(secretId, secretsStore, secretsService);

    // Should return as-is for backwards compatibility
    assertThat(result, is(secretId));
  }

  @Test
  public void testDecryptSecretMultipleCalls() {
    String secretId = "_789";
    String decryptedValue = "repeated-secret";

    when(secretsService.from(secretId)).thenReturn(secret);
    when(secret.decrypt()).thenReturn(decryptedValue.toCharArray());

    // Call multiple times
    String result1 = config.decryptSecret(secretId, secretsStore, secretsService);
    String result2 = config.decryptSecret(secretId, secretsStore, secretsService);

    assertThat(result1, is(decryptedValue));
    assertThat(result2, is(decryptedValue));
  }

  /**
   * Test subclass to expose the protected decryptSecret method for testing.
   */
  private static class TestCapabilityConfiguration
      extends CapabilityConfigurationSupport
  {
    // Exposes protected method for testing
    @Override
    public String decryptSecret(String secretId, SecretsStore secretsStore, SecretsService secretsService) {
      return super.decryptSecret(secretId, secretsStore, secretsService);
    }
  }
}
