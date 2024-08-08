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
package org.sonatype.nexus.crypto.secrets.internal;

import java.util.Optional;
import java.util.Random;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.crypto.LegacyCipherFactory;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.LegacyCipherFactoryImpl;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretData;
import org.sonatype.nexus.crypto.secrets.SecretsStore;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SecretsServiceImplTest
    extends TestSupport
{
  @Mock
  private SecretsStore secretsStore;

  @Mock
  private EncryptionKeySource encryptionKeySource;

  @Mock
  private DatabaseCheck databaseCheck;

  @Captor
  private ArgumentCaptor<String> encryptedValue;

  private final LegacyCipherFactory cipherFactory = new LegacyCipherFactoryImpl(new CryptoHelperImpl());

  private final PbeCipherFactory pbeCipherFactory = new PbeCipherFactoryImpl(new CryptoHelperImpl());

  private SecretsServiceImpl underTest;

  private final Random random = new Random();

  @Before
  public void setup() throws Exception {
    underTest =
        new SecretsServiceImpl(cipherFactory, pbeCipherFactory, secretsStore, encryptionKeySource, databaseCheck);
  }

  @Test
  public void testLegacyEncryptDecrypt() {
    when(databaseCheck.isAtLeast(anyString())).thenReturn(false);

    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encrypt("testing", secret, null);

    verify(secretsStore, never()).create(anyString(), anyString(), anyString(), anyString());
    verifyNoInteractions(secretsStore, encryptionKeySource);
    assertThat(encrypted.decrypt(), is(secret));
  }

  @Test
  public void testFromLegacy() {
    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encrypt("testing", secret, null);

    // Simulate reading an old value
    Secret fromEncrypted = underTest.from(encrypted.getId());

    verify(secretsStore, never()).read(anyInt());
    verify(encryptionKeySource, never()).getKey(anyString());
    assertThat(fromEncrypted.decrypt(), is(secret));
  }

  @Test
  public void testLegacyEncryptDecryptWithDefaultEncryptionKey() {
    int fakeId = random.nextInt();
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(fakeId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encrypt("testing", secret, null);

    //validate legacy secret was stored
    verify(secretsStore).create(eq("testing"), eq(null), encryptedValue.capture(), eq(null));
    assertThat(encrypted.getId(), is(String.format("_%d", fakeId)));

    //set up decryption flow
    when(secretsStore.read(fakeId)).thenReturn(Optional.of(getMockSecretData(fakeId, null, encryptedValue.getValue())));
    assertThat(encrypted.decrypt(), is(secret));
  }

  @Test
  public void testEncryptDecryptWithActiveKey() {
    int fakeId = random.nextInt();
    SecretEncryptionKey mockSecretKey = getMockSecretKey("test", "test-key-secret");
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(fakeId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.of(mockSecretKey));

    char[] secret = "phc-secret".toCharArray();

    Secret encrypted = underTest.encrypt("phc-testing", secret, "test-userid");

    verify(secretsStore).create(eq("phc-testing"), eq("test"), encryptedValue.capture(), eq("test-userid"));

    assertThat(encrypted.getId(), is(String.format("_%d", fakeId)));
    assertIsPhcSecret(encryptedValue.getValue());

    //set up decryption fow
    when(encryptionKeySource.getKey("test")).thenReturn(Optional.of(mockSecretKey));
    when(secretsStore.read(fakeId)).thenReturn(
        Optional.of(getMockSecretData(fakeId, "test", encryptedValue.getValue())));

    assertThat(encrypted.decrypt(), is(secret));
  }

  @Test
  public void testDecryptFailsWhenCustomKeyNotFound() {
    int fakeId = random.nextInt();
    SecretEncryptionKey mockSecretKey = getMockSecretKey("fake-key", "fake-key-secret");
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(fakeId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.of(mockSecretKey));

    char[] secret = "expected-failure".toCharArray();

    Secret encrypted = underTest.encrypt("phc-testing", secret, "test-userid");

    verify(secretsStore).create(eq("phc-testing"), eq("fake-key"), encryptedValue.capture(), eq("test-userid"));

    //set up decryption failure
    when(secretsStore.read(fakeId)).thenReturn(
        Optional.of(getMockSecretData(fakeId, "fake-key", encryptedValue.getValue())));
    when(encryptionKeySource.getKey("fake-key")).thenReturn(Optional.empty());

    CipherException expected = assertThrows(CipherException.class, encrypted::decrypt);
    assertThat(expected.getMessage(), is("unable to find secret key with id 'fake-key'."));
  }

  @Test
  public void testDecryptFailsIfRecordIsNotFound() {
    int fakeId = random.nextInt();
    SecretEncryptionKey mockSecretKey = getMockSecretKey("test-key", "test-key-secret");
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(fakeId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.of(mockSecretKey));

    char[] secret = "failure".toCharArray();

    Secret encrypted = underTest.encrypt("testing failure", secret, null);

    when(secretsStore.read(anyInt())).thenReturn(Optional.empty());

    CipherException expected = assertThrows(CipherException.class, encrypted::decrypt);
    assertThat(expected.getMessage(), is("Unable find secret for the specified token"));
  }

  @Test
  public void testRemoveWorksAsExpected() {
    int fakeId = random.nextInt();
    Secret secret = underTest.from("_" + fakeId);

    underTest.remove(secret);

    verify(secretsStore).delete(fakeId);
  }

  @Test
  public void testRemoveDoesNothingWithLegacyToken() {
    Secret secret = underTest.from("legacy_token");

    underTest.remove(secret);

    verifyNoInteractions(secretsStore);
  }

  private void assertIsPhcSecret(final String value) {
    try {
      EncryptedSecret encryptedSecret = EncryptedSecret.parse(value);
      assertNotNull(encryptedSecret);

      //none of these three should be null
      assertThat(encryptedSecret.getAlgorithm(), is(notNullValue()));
      assertThat(encryptedSecret.getSalt(), is(notNullValue()));
      assertThat(encryptedSecret.getValue(), is(notNullValue()));

      //initialization vector is present in phcSecret as extra attribute
      assertThat(encryptedSecret.getAttributes().get("iv"), is(notNullValue()));
    }
    catch (IllegalArgumentException e) {
      fail("the argument sent is not a PhcSecret");
    }
  }

  private SecretEncryptionKey getMockSecretKey(final String id, final String key) {
    SecretEncryptionKey secretEncryptionKey = new SecretEncryptionKey();
    secretEncryptionKey.setId(id);
    secretEncryptionKey.setKey(key);

    return secretEncryptionKey;
  }

  private SecretData getMockSecretData(final int id, final String keyId, final String secret) {
    SecretData mockData = new SecretData();
    mockData.setId(id);
    mockData.setKeyId(keyId);
    mockData.setSecret(secret);
    return mockData;
  }
}
