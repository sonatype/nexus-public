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

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.LegacyCipherFactory;
import org.sonatype.nexus.crypto.PhraseService;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.HashingHandlerFactory;
import org.sonatype.nexus.crypto.internal.HashingHandlerFactoryImpl;
import org.sonatype.nexus.crypto.internal.LegacyCipherFactoryImpl;
import org.sonatype.nexus.crypto.internal.MavenCipherImpl;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.maven.MavenCipher;
import org.sonatype.nexus.crypto.secrets.ActiveKeyChangeEvent;
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SecretsServiceImplTest
{
  @Mock
  private SecretsStore secretsStore;

  @Mock
  private EncryptionKeySource encryptionKeySource;

  @Mock
  private DatabaseCheck databaseCheck;

  @Captor
  private ArgumentCaptor<String> encryptedValue;

  private final CryptoHelper cryptoHelper = new CryptoHelperImpl(false);

  private final LegacyCipherFactory cipherFactory = new LegacyCipherFactoryImpl(cryptoHelper);

  private final MavenCipher mavenCipher = new MavenCipherImpl(cryptoHelper);

  private final Random random = new Random();

  private SecretsServiceImpl underTestSha1;

  private SecretsServiceImpl underTestSha256;

  @Before
  public void setup() throws Exception {
    HashingHandlerFactory hashingHandlerFactory = new HashingHandlerFactoryImpl(cryptoHelper);
    PbeCipherFactory sha1Factory =
        new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory, "PBKDF2WithHmacSHA1", null);
    PbeCipherFactory sha256Factory =
        new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory, "PBKDF2WithHmacSHA256", null);

    underTestSha1 = new SecretsServiceImpl(cipherFactory, mavenCipher, PhraseService.LEGACY_PHRASE_SERVICE, sha1Factory,
        secretsStore, encryptionKeySource, databaseCheck, false);
    underTestSha256 =
        new SecretsServiceImpl(cipherFactory, mavenCipher, PhraseService.LEGACY_PHRASE_SERVICE, sha256Factory,
            secretsStore, encryptionKeySource, databaseCheck, false);
  }

  @Test
  public void testLegacyCannotBeUsedWithFips() {
    HashingHandlerFactory hashingHandlerFactory = new HashingHandlerFactoryImpl(cryptoHelper);
    PbeCipherFactory sha1Factory =
        new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory, "PBKDF2WithHmacSHA1", null);
    when(databaseCheck.isAtLeast(anyString())).thenReturn(false);
    IllegalStateException expected = assertThrows(IllegalStateException.class,
        () -> new SecretsServiceImpl(cipherFactory, mavenCipher, PhraseService.LEGACY_PHRASE_SERVICE, sha1Factory,
            secretsStore, encryptionKeySource, databaseCheck, true));
    assertThat(expected.getMessage(), is("FIPS mode requires migration to the new secrets service"));
  }

  @Test
  public void testLegacyMavenEncryptDecrypt() {
    when(databaseCheck.isAtLeast(anyString())).thenReturn(false);

    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTestSha1.encryptMaven("testing", secret, null);
    // validate encrypted value was encrypted using maven cipher
    assertTrue(mavenCipher.isPasswordCipher(encrypted.getId()));

    verifyNoInteractions(secretsStore, encryptionKeySource);
    assertThat(encrypted.decrypt(), is(secret));
  }

  @Test
  public void testFromLegacyMaven() {
    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTestSha1.encryptMaven("testing", secret, null);
    // validate encrypted value was encrypted using maven cipher
    assertTrue(mavenCipher.isPasswordCipher(encrypted.getId()));

    // Simulate reading an old value
    Secret fromEncrypted = underTestSha1.from(encrypted.getId());

    verifyNoInteractions(secretsStore, encryptionKeySource);
    assertThat(fromEncrypted.decrypt(), is(secret));
  }

  @Test
  public void testLegacyPbeEncryptDecrypt() {
    when(databaseCheck.isAtLeast(anyString())).thenReturn(false);

    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTestSha1.encrypt("testing", secret, null);

    verifyNoInteractions(secretsStore, encryptionKeySource);
    assertThat(encrypted.decrypt(), is(secret));
  }

  @Test
  public void testFromLegacyPbe() {
    char[] secret = "my-secret".toCharArray();

    Secret encrypted = underTestSha1.encrypt("testing", secret, null);

    // Simulate reading an old value
    Secret fromEncrypted = underTestSha1.from(encrypted.getId());

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

    Secret encrypted = underTestSha1.encrypt("testing", secret, null);

    // validate legacy secret was stored
    verify(secretsStore).create(eq("testing"), eq(null), encryptedValue.capture(), eq(null));
    assertThat(encrypted.getId(), is(String.format("_%d", fakeId)));

    // set up decryption flow
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

    Secret encrypted = underTestSha1.encrypt("phc-testing", secret, "test-userid");

    verify(secretsStore).create(eq("phc-testing"), eq("test"), encryptedValue.capture(), eq("test-userid"));

    assertThat(encrypted.getId(), is(String.format("_%d", fakeId)));
    assertIsPhcSecret(encryptedValue.getValue());

    // set up decryption fow
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

    Secret encrypted = underTestSha1.encrypt("phc-testing", secret, "test-userid");

    verify(secretsStore).create(eq("phc-testing"), eq("fake-key"), encryptedValue.capture(), eq("test-userid"));

    // set up decryption failure
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

    Secret encrypted = underTestSha1.encrypt("testing failure", secret, null);

    when(secretsStore.read(anyInt())).thenReturn(Optional.empty());

    CipherException expected = assertThrows(CipherException.class, encrypted::decrypt);
    assertThat(expected.getMessage(), is("Unable to find secret for the specified token"));
  }

  @Test
  public void testRemoveWorksAsExpected() {
    int fakeId = random.nextInt();
    Secret secret = underTestSha1.from("_" + fakeId);

    underTestSha1.remove(secret);

    verify(secretsStore).delete(fakeId);
  }

  @Test
  public void testRemoveDoesNothingWithLegacyToken() {
    Secret secret = underTestSha1.from("legacy_token");

    underTestSha1.remove(secret);

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

    underTestSha1.reEncrypt(secretData, newKey);
    verify(secretsStore).update(anyInt(), anyString(), eq(newKey), anyString());
  }

  @Test
  public void testReEncryptRequired() {
    SecretEncryptionKey mockSecretKey = getMockSecretKey("active-key", "test-key-secret");
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.of(mockSecretKey));
    when(secretsStore.existWithDifferentKeyId("active-key")).thenReturn(true);

    assertTrue(underTestSha1.isReEncryptRequired());

    when(secretsStore.existWithDifferentKeyId("active-key")).thenReturn(false);
    assertFalse(underTestSha1.isReEncryptRequired());

    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());
    assertFalse(underTestSha1.isReEncryptRequired());
  }

  @Test
  public void testActiveKeyChangedOnEvent() {
    ActiveKeyChangeEvent event = new ActiveKeyChangeEvent("new-key", "old-key", null);
    underTestSha1.on(event);
    verify(encryptionKeySource).setActiveKey("new-key");
  }

  @Test
  public void testSha1AlgorithmUsedWhenConfigured() {
    int fakeId = random.nextInt();
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(fakeId);

    char[] secret = "test-secret".toCharArray();
    underTestSha1.encrypt("test", secret, null);

    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    EncryptedSecret phc = EncryptedSecret.parse(encryptedValue.getValue());

    assertThat(phc.getAlgorithm(), is("PBKDF2WithHmacSHA1"));
  }

  @Test
  public void testSha256AlgorithmUsedWhenConfigured() {
    int fakeId = random.nextInt();
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(fakeId);

    char[] secret = "test-secret".toCharArray();
    underTestSha256.encrypt("test", secret, null);

    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    EncryptedSecret phc = EncryptedSecret.parse(encryptedValue.getValue());

    assertThat(phc.getAlgorithm(), is("PBKDF2WithHmacSHA256"));
  }

  @Test
  public void testMigrationFromSha1ToSha256() {
    // First encrypt with SHA1
    int fakeId = random.nextInt();
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(fakeId);

    char[] secret = "migrate-me".toCharArray();
    underTestSha1.encrypt("test", secret, null);

    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    String sha1Encrypted = encryptedValue.getValue();

    // Now decrypt with SHA256 configured (should trigger re-encryption)
    when(secretsStore.read(fakeId)).thenReturn(Optional.of(
        getMockSecretData(fakeId, null, sha1Encrypted)));

    // Create a new secret from the stored ID
    Secret secretToDecrypt = underTestSha256.from("_" + fakeId);
    char[] decrypted = secretToDecrypt.decrypt();

    // Verify re-encryption occurred with SHA256
    verify(secretsStore).update(eq(fakeId), anyString(), eq(null),
        argThat(newValue -> {
          EncryptedSecret phc = EncryptedSecret.parse(newValue);
          return phc.getAlgorithm().equals("PBKDF2WithHmacSHA256");
        }));
    assertThat(decrypted, is(secret));
  }

  @Test
  public void testMigrationFromSha256ToSha1() {
    // First encrypt with SHA256
    int fakeId = random.nextInt();
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(fakeId);

    char[] secret = "migrate-me".toCharArray();
    underTestSha256.encrypt("test", secret, null);

    // Capture the SHA256 encrypted value
    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    String sha256Encrypted = encryptedValue.getValue();

    // Now decrypt with SHA1 configured (should trigger re-encryption)
    when(secretsStore.read(fakeId)).thenReturn(Optional.of(
        getMockSecretData(fakeId, null, sha256Encrypted)));

    // Create a new secret from the stored ID
    Secret secretToDecrypt = underTestSha1.from("_" + fakeId);
    char[] decrypted = secretToDecrypt.decrypt();

    // Verify re-encryption occurred with SHA1
    verify(secretsStore).update(eq(fakeId), anyString(), eq(null),
        argThat(newValue -> {
          EncryptedSecret phc = EncryptedSecret.parse(newValue);
          return phc.getAlgorithm().equals("PBKDF2WithHmacSHA1");
        }));
    assertThat(decrypted, is(secret));
  }

  private void assertIsPhcSecret(final String value) {
    try {
      EncryptedSecret encryptedSecret = EncryptedSecret.parse(value);
      assertNotNull(encryptedSecret);

      // none of these three should be null
      assertThat(encryptedSecret.getAlgorithm(), is(notNullValue()));
      assertThat(encryptedSecret.getSalt(), is(notNullValue()));
      assertThat(encryptedSecret.getValue(), is(notNullValue()));

      // initialization vector is present in phcSecret as extra attribute
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
    underTestSha1.encrypt("testing", secret.toCharArray(), null);
    verify(secretsStore, atLeastOnce()).create(eq("testing"), eq(encryptionKey.getId()), encryptedValue.capture(),
        eq(null));
    return encryptedValue.getValue();
  }

  @Test
  public void testExportEncryptedWithModernSecret() {
    // Setup: create a modern secret with ID
    int fakeId = random.nextInt();
    String encryptedValue = "$pbkdf2$v=1$i=10000,l=32$abc123$def456";

    when(secretsStore.read(fakeId)).thenReturn(Optional.of(
        getMockSecretData(fakeId, null, encryptedValue)));

    // Export the encrypted value
    String exported = underTestSha1.exportEncrypted("_" + fakeId);

    // Should return the encrypted PHC string
    assertThat(exported, is(encryptedValue));
    verify(secretsStore).read(fakeId);
  }

  @Test
  public void testExportEncryptedWithLegacySecret() {
    // Legacy secrets don't start with underscore
    String legacyEncrypted = "some-legacy-encrypted-value";

    // Export should return the value as-is without querying the store
    String exported = underTestSha1.exportEncrypted(legacyEncrypted);

    assertThat(exported, is(legacyEncrypted));
    verifyNoInteractions(secretsStore);
  }

  @Test
  public void testExportEncryptedWithNullSecret() {
    String exported = underTestSha1.exportEncrypted(null);
    assertThat(exported, is((String) null));
    verifyNoInteractions(secretsStore);
  }

  @Test
  public void testExportEncryptedWhenSecretNotFound() {
    int fakeId = random.nextInt();
    when(secretsStore.read(fakeId)).thenReturn(Optional.empty());

    String exported = underTestSha1.exportEncrypted("_" + fakeId);

    assertThat(exported, is((String) null));
    verify(secretsStore).read(fakeId);
  }

  @Test
  public void testImportEncryptedWithModernSecret() {
    // Setup: First create a real encrypted value to use for import
    char[] secret = "test-password".toCharArray();
    int originalId = random.nextInt();
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(originalId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    Secret encrypted = underTestSha1.encrypt("test", secret, null);
    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    String realEncryptedValue = encryptedValue.getValue();

    // Now import this encrypted value
    int newId = random.nextInt() + 1000;
    when(secretsStore.create(eq("email"), any(), anyString(), eq("testUser"))).thenReturn(newId);

    // Import the encrypted value
    Secret imported = underTestSha1.importEncrypted("email", realEncryptedValue, "testUser");

    // Should create a new entry in the store and return a new ID
    assertThat(imported.getId(), is("_" + newId));
    // Verify it was re-encrypted (the stored value should be different from original due to new IV/salt)
    verify(secretsStore).create(eq("email"), eq(null), argThat(value -> {
      return !value.equals(realEncryptedValue) && value.startsWith("$");
    }), eq("testUser"));
  }

  @Test
  public void testImportEncryptedWithLegacySecret() {
    // Legacy secrets are just wrapped, not stored
    String legacyEncrypted = "some-legacy-encrypted-value";

    Secret imported = underTestSha1.importEncrypted("email", legacyEncrypted, null);

    // Should return a Secret wrapping the legacy value without storing
    assertThat(imported.getId(), is(legacyEncrypted));
    verifyNoInteractions(secretsStore);
  }

  @Test
  public void testImportEncryptedWithNullValue() {
    Secret imported = underTestSha1.importEncrypted("email", null, null);
    assertThat(imported, is((Secret) null));
    verifyNoInteractions(secretsStore);
  }

  @Test
  public void testImportEncryptedWithActiveKey() {
    // Setup: First create a real encrypted value to use for import
    char[] secret = "test-password".toCharArray();
    int originalId = random.nextInt();
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(originalId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    Secret encrypted = underTestSha1.encrypt("test", secret, null);
    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    String realEncryptedValue = encryptedValue.getValue();

    // Now import with an active key
    int newId = random.nextInt() + 1000;
    SecretEncryptionKey activeKey = getMockSecretKey("active-key", "some-key-value");
    when(secretsStore.create(eq("httpclient"), eq("active-key"), anyString(), any())).thenReturn(newId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.of(activeKey));

    // Import the encrypted value
    Secret imported = underTestSha1.importEncrypted("httpclient", realEncryptedValue, null);

    // Should create a new entry with the active key ID and re-encrypted value
    assertThat(imported.getId(), is("_" + newId));
    verify(secretsStore).create(eq("httpclient"), eq("active-key"), argThat(value -> {
      return !value.equals(realEncryptedValue) && value.startsWith("$");
    }), eq(null));
  }

  @Test
  public void testExportImportRoundTrip() {
    // Test a full round-trip: encrypt -> export -> import -> decrypt
    int originalId = random.nextInt();
    int newId = random.nextInt() + 1000; // Different ID
    char[] secret = "test-password".toCharArray();

    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(eq("test"), any(), anyString(), any())).thenReturn(originalId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    // 1. Encrypt a secret
    Secret encrypted = underTestSha1.encrypt("test", secret, null);
    verify(secretsStore).create(eq("test"), any(), encryptedValue.capture(), any());
    String storedEncryptedValue = encryptedValue.getValue();

    // 2. Export it (simulating export from one system)
    when(secretsStore.read(originalId)).thenReturn(Optional.of(
        getMockSecretData(originalId, null, storedEncryptedValue)));
    String exported = underTestSha1.exportEncrypted(encrypted.getId());
    assertThat(exported, is(storedEncryptedValue));

    // 3. Import it (simulating import to another system)
    // Import will decrypt and re-encrypt, so we need to capture the new encrypted value
    ArgumentCaptor<String> reEncryptedValue = ArgumentCaptor.forClass(String.class);
    when(secretsStore.create(eq("imported"), any(), anyString(), any())).thenReturn(newId);
    Secret imported = underTestSha1.importEncrypted("imported", exported, null);
    assertThat(imported.getId(), is("_" + newId));
    verify(secretsStore).create(eq("imported"), eq(null), reEncryptedValue.capture(), eq(null));

    // 4. Decrypt and verify - use the re-encrypted value, not the original
    when(secretsStore.read(newId)).thenReturn(Optional.of(
        getMockSecretData(newId, null, reEncryptedValue.getValue())));
    char[] decrypted = imported.decrypt();
    assertThat(decrypted, is(secret));
  }

  @Test
  public void testImportEncryptedWithMigrationCipherPassword() throws Exception {
    // Setup: Create an encrypted value using a custom password
    char[] secret = "my-secret-password".toCharArray();
    String customPassword = "migration-cipher-key";

    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    // Create a cipher with the custom password and encrypt the secret
    HashingHandlerFactory hashingHandlerFactory = new HashingHandlerFactoryImpl(cryptoHelper);
    PbeCipherFactory cipherFactory =
        new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory, "PBKDF2WithHmacSHA256", null);
    SecretEncryptionKey customKey = new SecretEncryptionKey(null, customPassword);
    String encryptedValue = cipherFactory.create(customKey).encrypt(toBytes(secret)).toPhcString();

    // Now import it with the migration cipher password
    int newId = random.nextInt();
    when(secretsStore.create(eq("email"), any(), anyString(), eq("testUser"))).thenReturn(newId);

    Secret imported = underTestSha256.importEncrypted("email", encryptedValue, "testUser", customPassword);

    // Verify it created a new entry
    assertThat(imported.getId(), is("_" + newId));
    verify(secretsStore).create(eq("email"), eq(null), anyString(), eq("testUser"));
  }

  @Test
  public void testImportEncryptedWithoutMigrationCipherPassword_usesDefaultKey() throws Exception {
    // Create an encrypted value using the default password
    char[] secret = "test-password".toCharArray();
    int originalId = random.nextInt();

    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(originalId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    Secret encrypted = underTestSha256.encrypt("test", secret, null);
    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    String realEncryptedValue = encryptedValue.getValue();

    // Import without migration cipher password (null)
    int newId = random.nextInt() + 1000;
    when(secretsStore.create(eq("email"), any(), anyString(), eq("testUser"))).thenReturn(newId);

    Secret imported = underTestSha256.importEncrypted("email", realEncryptedValue, "testUser", null);

    // Should successfully import using default key
    assertThat(imported.getId(), is("_" + newId));
    verify(secretsStore).create(eq("email"), eq(null), anyString(), eq("testUser"));
  }

  @Test
  public void testImportEncryptedWithEmptyMigrationCipherPassword_usesDefaultKey() throws Exception {
    // Create an encrypted value using the default password
    char[] secret = "test-password".toCharArray();
    int originalId = random.nextInt();

    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(originalId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    Secret encrypted = underTestSha256.encrypt("test", secret, null);
    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    String realEncryptedValue = encryptedValue.getValue();

    // Import with empty migration cipher password
    int newId = random.nextInt() + 1000;
    when(secretsStore.create(eq("email"), any(), anyString(), eq("testUser"))).thenReturn(newId);

    Secret imported = underTestSha256.importEncrypted("email", realEncryptedValue, "testUser", "");

    // Should successfully import using default key
    assertThat(imported.getId(), is("_" + newId));
    verify(secretsStore).create(eq("email"), eq(null), anyString(), eq("testUser"));
  }

  @Test
  public void testImportEncryptedWithWrongMigrationCipherPassword_throwsException() throws Exception {
    // Create an encrypted value using a custom password
    char[] secret = "my-secret".toCharArray();
    String correctPassword = "correct-password";
    String wrongPassword = "wrong-password";

    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    // Encrypt with correct password
    HashingHandlerFactory hashingHandlerFactory = new HashingHandlerFactoryImpl(cryptoHelper);
    PbeCipherFactory cipherFactory =
        new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory, "PBKDF2WithHmacSHA256", null);
    SecretEncryptionKey correctKey = new SecretEncryptionKey(null, correctPassword);
    String encryptedValue = cipherFactory.create(correctKey).encrypt(toBytes(secret)).toPhcString();

    // Try to import with wrong password
    assertThrows(CipherException.class,
        () -> underTestSha256.importEncrypted("email", encryptedValue, "testUser", wrongPassword));
  }

  @Test
  public void testImportEncryptedDelegatesWithoutCipherPassword() throws Exception {
    // Test that the 3-parameter method delegates to the 4-parameter method with null
    char[] secret = "test-password".toCharArray();
    int originalId = random.nextInt();

    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(originalId);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());

    Secret encrypted = underTestSha256.encrypt("test", secret, null);
    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    String realEncryptedValue = encryptedValue.getValue();

    // Import using the 3-parameter method
    int newId = random.nextInt() + 1000;
    when(secretsStore.create(eq("email"), any(), anyString(), eq(null))).thenReturn(newId);

    Secret imported = underTestSha256.importEncrypted("email", realEncryptedValue, null);

    // Should successfully import
    assertThat(imported.getId(), is("_" + newId));
    verify(secretsStore).create(eq("email"), eq(null), anyString(), eq(null));
  }

  @Test
  public void testExportEncryptedWithPassword() {
    setupDatabaseMocks();

    // Create and store a secret with mocked reading
    Object[] result = createAndMockSecret("mypassword");
    Secret secret = (Secret) result[0];

    // Export with custom password
    String exported = underTestSha256.exportEncryptedWithPassword(secret.getId(), "custom-password-123");

    // Verify format starts with algorithm identifier
    assertThat(exported, startsWith("$PBKDF2"));

    // Verify can be imported with same password
    int newId = random.nextInt() + 1000;
    when(secretsStore.create(eq("test"), any(), anyString(), eq(null))).thenReturn(newId);
    Secret imported = underTestSha256.importEncrypted("test", exported, null, "custom-password-123");

    // Capture and mock reading the imported secret
    verify(secretsStore, atLeastOnce()).create(eq("test"), any(), encryptedValue.capture(), eq(null));
    mockSecretRead(newId, encryptedValue.getValue());

    assertThat(new String(imported.decrypt()), is("mypassword"));
  }

  @Test
  public void testExportEncryptedWithPassword_nullSecretId_throwsException() {
    assertThrows(NullPointerException.class,
        () -> underTestSha256.exportEncryptedWithPassword(null, "password"));
  }

  @Test
  public void testExportEncryptedWithPassword_emptyPassword_throwsException() {
    setupDatabaseMocks();

    Object[] result = createAndMockSecret("mypassword");
    Secret secret = (Secret) result[0];

    assertThrows(CipherException.class,
        () -> underTestSha256.exportEncryptedWithPassword(secret.getId(), ""));
  }

  @Test
  public void testExportEncryptedWithPassword_secretNotFound_throwsException() {
    when(secretsStore.read(999999)).thenReturn(Optional.empty());
    assertThrows(CipherException.class,
        () -> underTestSha256.exportEncryptedWithPassword("_999999", "password"));
  }

  @Test
  public void testEncryptPlaintextWithPassword() {
    setupDatabaseMocks();

    // Encrypt plaintext with custom password
    String encrypted = underTestSha256.encryptPlaintextWithPassword("my-bearer-token", "custom-password-123");

    // Verify format starts with algorithm identifier
    assertThat(encrypted, startsWith("$PBKDF2"));

    // Verify can be decrypted via import
    int newId = random.nextInt();
    when(secretsStore.create(eq("test"), any(), anyString(), eq(null))).thenReturn(newId);
    Secret imported = underTestSha256.importEncrypted("test", encrypted, null, "custom-password-123");

    // Capture and mock reading the imported secret
    verify(secretsStore).create(eq("test"), any(), encryptedValue.capture(), eq(null));
    mockSecretRead(newId, encryptedValue.getValue());

    assertThat(new String(imported.decrypt()), is("my-bearer-token"));
  }

  @Test
  public void testEncryptPlaintextWithPassword_nullPlaintext_throwsException() {
    assertThrows(NullPointerException.class,
        () -> underTestSha256.encryptPlaintextWithPassword(null, "password"));
  }

  @Test
  public void testEncryptPlaintextWithPassword_emptyPassword_throwsException() {
    assertThrows(CipherException.class,
        () -> underTestSha256.encryptPlaintextWithPassword("plaintext", ""));
  }

  @Test
  public void testExportImportRoundTrip_withCustomPassword() {
    setupDatabaseMocks();

    // Create original secret with mocked reading
    Object[] result = createAndMockSecret("original-secret");
    Secret original = (Secret) result[0];

    // Export with custom password
    String exported = underTestSha256.exportEncryptedWithPassword(original.getId(), "migration-pwd");

    // Import with same password
    int newId = random.nextInt() + 1000;
    when(secretsStore.create(eq("test"), any(), anyString(), eq(null))).thenReturn(newId);
    Secret imported = underTestSha256.importEncrypted("test", exported, null, "migration-pwd");

    // Capture and mock reading the imported secret
    verify(secretsStore, atLeastOnce()).create(eq("test"), any(), encryptedValue.capture(), eq(null));
    mockSecretRead(newId, encryptedValue.getValue());

    assertThat(new String(imported.decrypt()), is("original-secret"));
    assertThat(imported.getId(), not(is(original.getId()))); // New ID after import
  }

  /**
   * Helper method to convert char[] to byte[] for encryption.
   */
  private static byte[] toBytes(final char[] chars) {
    ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
    byte[] bytes = new byte[byteBuffer.limit()];
    byteBuffer.get(bytes);
    return bytes;
  }

  /**
   * Sets up common mocks for database migration tests.
   */
  private void setupDatabaseMocks() {
    when(databaseCheck.isAtLeast(anyString())).thenReturn(true);
    when(encryptionKeySource.getActiveKey()).thenReturn(Optional.empty());
  }

  /**
   * Mocks SecretData for reading from the secrets store.
   *
   * @param secretId the ID of the secret
   * @param encryptedValue the encrypted value to store
   */
  private void mockSecretRead(final int secretId, final String encryptedValue) {
    SecretData secretData = new SecretData();
    secretData.setId(secretId);
    secretData.setSecret(encryptedValue);
    secretData.setKeyId(null);
    when(secretsStore.read(secretId)).thenReturn(Optional.of(secretData));
  }

  /**
   * Creates a secret and sets up mocks for reading it back.
   * Returns both the created secret and the secret ID for further testing.
   *
   * @param plaintext the plaintext to encrypt
   * @return array with [Secret, secretId] for use in tests
   */
  private Object[] createAndMockSecret(final String plaintext) {
    int secretId = random.nextInt();
    when(secretsStore.create(anyString(), any(), anyString(), any())).thenReturn(secretId);

    Secret secret = underTestSha256.encrypt("test", plaintext.toCharArray(), null);

    // Capture and mock the stored value for reading
    verify(secretsStore).create(anyString(), any(), encryptedValue.capture(), any());
    mockSecretRead(secretId, encryptedValue.getValue());

    return new Object[]{secret, secretId};
  }
}
