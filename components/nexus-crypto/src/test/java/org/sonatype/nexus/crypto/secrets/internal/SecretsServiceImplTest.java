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
import org.sonatype.nexus.crypto.PhraseService;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.LegacyCipherFactoryImpl;
import org.sonatype.nexus.crypto.internal.MavenCipherImpl;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.maven.MavenCipher;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretData;
import org.sonatype.nexus.crypto.secrets.ActiveKeyChangeEvent;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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

  private final MavenCipher mavenCipher = new MavenCipherImpl(new CryptoHelperImpl());

  private SecretsServiceImpl underTest;

  private final Random random = new Random();

  @Before
  public void setup() throws Exception {
    underTest =
        new SecretsServiceImpl(cipherFactory, mavenCipher, PhraseService.LEGACY_PHRASE_SERVICE, pbeCipherFactory,
            secretsStore, encryptionKeySource, databaseCheck);
  }

  @Test
  public void testLegacyMavenEncryptDecrypt() {
    when(databaseCheck.isAtLeast(anyString())).thenReturn(false);

    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encryptMaven("testing", secret, null);
    //validate encrypted value was encrypted using maven cipher
    assertTrue(mavenCipher.isPasswordCipher(encrypted.getId()));

    verifyNoInteractions(secretsStore, encryptionKeySource);
    assertThat(encrypted.decrypt(), is(secret));
  }

  @Test
  public void testFromLegacyMaven() {
    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encryptMaven("testing", secret, null);
    //validate encrypted value was encrypted using maven cipher
    assertTrue(mavenCipher.isPasswordCipher(encrypted.getId()));

    // Simulate reading an old value
    Secret fromEncrypted = underTest.from(encrypted.getId());

    verifyNoInteractions(secretsStore, encryptionKeySource);
    assertThat(fromEncrypted.decrypt(), is(secret));
  }

  @Test
  public void testLegacyPbeEncryptDecrypt() {
    when(databaseCheck.isAtLeast(anyString())).thenReturn(false);

    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encrypt("testing", secret, null);

    verifyNoInteractions(secretsStore, encryptionKeySource);
    assertThat(encrypted.decrypt(), is(secret));
  }

  @Test
  public void testFromLegacyPbe() {
    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTest.encrypt("testing", secret, null);

    // Simulate reading an old value
    Secret fromEncrypted = underTest.from(encrypted.getId());

    verifyNoInteractions(secretsStore, encryptionKeySource);
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

  @Test
  public void testReEncrypt() {
    String oldKey = "old-key";
    String newKey = "new-key";

    SecretEncryptionKey mockSecretKey = getMockSecretKey("old-key", "test-key-secret");
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.of(mockSecretKey));
    when(encryptionKeySource.getKey("old-key")).thenReturn(Optional.of(mockSecretKey));

    when(secretsStore.read(anyInt())).thenAnswer(invocation -> {
      int id = invocation.getArgument(0);
      return Optional.of(getMockSecretData(id, oldKey, getEncryptedSecret(id, "secret" + id, mockSecretKey)));
    });

    int secretId = random.nextInt();
    SecretData secretData =
        getMockSecretData(secretId, oldKey, getEncryptedSecret(secretId, "secret" + secretId, mockSecretKey));

    underTest.reEncrypt(secretData, newKey);
    verify(secretsStore).update(anyInt(), anyString(), eq(newKey), anyString());
  }

  @Test
  public void testReEncryptRequired() {
    SecretEncryptionKey mockSecretKey = getMockSecretKey("active-key", "test-key-secret");
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.of(mockSecretKey));
    when(secretsStore.existWithDifferentKeyId("active-key")).thenReturn(true);

    assertTrue(underTest.isReEncryptRequired());

    when(secretsStore.existWithDifferentKeyId("active-key")).thenReturn(false);
    assertFalse(underTest.isReEncryptRequired());

    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());
    assertFalse(underTest.isReEncryptRequired());
  }

  @Test
  public void testActiveKeyChangedOnEvent() {
    ActiveKeyChangeEvent event = new ActiveKeyChangeEvent("new-key", "old-key", null);
    underTest.on(event);
    verify(encryptionKeySource).setActiveKey("new-key");
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

  private String getEncryptedSecret(final int secretId, final String secret, final SecretEncryptionKey encryptionKey) {
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(secretId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.of(encryptionKey));
    underTest.encrypt("testing", secret.toCharArray(), null);
    verify(secretsStore, atLeastOnce()).create(eq("testing"), eq(encryptionKey.getId()), encryptedValue.capture(),
        eq(null));
    return encryptedValue.getValue();
  }
}
