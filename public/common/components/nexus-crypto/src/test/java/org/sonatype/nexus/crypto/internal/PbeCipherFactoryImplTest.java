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
package org.sonatype.nexus.crypto.internal;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.HashingHandler;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl.PbeCipherImpl;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import com.fasterxml.jackson.core.Base64Variants;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.sonatype.nexus.crypto.internal.EncryptionHelper.fromBase64;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PbeCipherFactoryImplTest
{
  private SecretEncryptionKey encryptionKey;

  private PbeCipherFactoryImpl factory;

  @BeforeEach
  void setUp() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    encryptionKey = mock(SecretEncryptionKey.class);
    when(encryptionKey.getKey()).thenReturn("test-secret-key");
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    when(hashingHandlerFactory.create("PBKDF2WithSHA1")).thenReturn(mock(HashingHandler.class));

    factory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory, "PBKDF2WithSHA1", null);
  }

  @Test
  void testCreate_DefaultAlgorithm_Pbkdf2Sha1() {
    PbeCipher cipher = factory.create(encryptionKey);
    assertNotNull(cipher);
    assertInstanceOf(PbeCipherImpl.class, cipher);
    assertInstanceOf(cipher.getClass(), cipher);
    assertTrue(cipher.isDefaultCipher());
  }

  @Test
  void testCreate_when_Sha256Algorithm_input_creates_Pbkdf2Sha256() {
    String encoded =
        "$pbkdf2-sha256$iv=a6f7e545dc07ae8b0c5ff522d58b5994,key_iteration=10000,key_len=256$9+gAr77ZJtvlpDZm7Av1bg==$Z7yqZ3ok5JyAFYjoJ9lo+p2G1GZFUX9kqnDEJFHeErg=";

    PbeCipher cipher = factory.create(encryptionKey, encoded);
    assertNotNull(cipher);
    assertFalse(cipher.isDefaultCipher());
  }

  @Test
  void testCreate_UnsupportedAlgorithm_ShouldThrow() {
    String encoded = "$unsupportedabcdef0123456789$16salt";
    assertThrows(IllegalArgumentException.class, () -> factory.create(encryptionKey, encoded));
  }

  @Test
  void testCreate_WithExplicitIterations_SHA1() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    when(hashingHandlerFactory.create("PBKDF2WithHmacSHA1", "test-salt".getBytes(), 5000))
        .thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA1", null);

    PbeCipher cipher = customFactory.create(encryptionKey, "test-salt", "test-iv", 5000);
    assertNotNull(cipher);
    assertTrue(cipher.isDefaultCipher());
  }

  @Test
  void testCreate_WithExplicitIterations_SHA256() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    when(hashingHandlerFactory.create("PBKDF2WithHmacSHA256", "test-salt".getBytes(), 15000))
        .thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA256", null);

    PbeCipher cipher = customFactory.create(encryptionKey, "test-salt", "test-iv", 15000);
    assertNotNull(cipher);
    assertTrue(cipher.isDefaultCipher());
  }

  @Test
  void testCreate_WithNullIterations_UsesConfiguredIterations() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    // When iterations is null, should use configuredSecretsIterations (12000)
    when(hashingHandlerFactory.create("PBKDF2WithHmacSHA256", "test-salt".getBytes(), 12000))
        .thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA256", 12000);

    PbeCipher cipher = customFactory.create(encryptionKey, "test-salt", "test-iv", null);
    assertNotNull(cipher);
    assertTrue(cipher.isDefaultCipher());
  }

  @Test
  void testCreate_WithBothNullIterations_PassesNullToHashingHandler() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    // When both are null, should pass null to HashingHandlerFactory (will use defaults)
    when(hashingHandlerFactory.create("PBKDF2WithHmacSHA256", "test-salt".getBytes(), null))
        .thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA256", null);

    PbeCipher cipher = customFactory.create(encryptionKey, "test-salt", "test-iv", null);
    assertNotNull(cipher);
    assertTrue(cipher.isDefaultCipher());
  }

  @Test
  void testCreate_FromPhcString_ExtractsIterationsCorrectly() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    // PHC string contains key_iteration=8000
    when(hashingHandlerFactory.create(anyString(), any(), eq(8000))).thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA1", null);

    String phcString =
        "$PBKDF2WithHmacSHA256$iv=a6f7e545dc07ae8b0c5ff522d58b5994,key_iteration=8000,key_len=256$9+gAr77ZJtvlpDZm7Av1bg==$Z7yqZ3ok5JyAFYjoJ9lo+p2G1GZFUX9kqnDEJFHeErg=";
    PbeCipher cipher = customFactory.create(encryptionKey, phcString);
    assertNotNull(cipher);
    // Should NOT be default cipher because algorithm differs (SHA256 vs SHA1)
    assertFalse(cipher.isDefaultCipher());
  }

  @Test
  void testIsDefaultCipher_WhenAlgorithmMatches_SHA1() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    when(hashingHandlerFactory.create(anyString(), any(), any())).thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA1", null);

    String phcStringSHA1 = "$PBKDF2WithHmacSHA1$iv=a6f7e545dc07ae8b,key_iteration=1024,key_len=128$c2FsdA==$dmFsdWU=";
    PbeCipher cipher = customFactory.create(encryptionKey, phcStringSHA1);
    assertTrue(cipher.isDefaultCipher());
  }

  @Test
  void testIsDefaultCipher_WhenAlgorithmMatches_SHA256() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    when(hashingHandlerFactory.create(anyString(), any(), any())).thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA256", null);

    String phcStringSHA256 =
        "$PBKDF2WithHmacSHA256$iv=a6f7e545dc07ae8b,key_iteration=10000,key_len=256$c2FsdA==$dmFsdWU=";
    PbeCipher cipher = customFactory.create(encryptionKey, phcStringSHA256);
    assertTrue(cipher.isDefaultCipher());
  }

  @Test
  void testIsDefaultCipher_WhenAlgorithmDiffers() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    when(hashingHandlerFactory.create(anyString(), any(), any())).thenReturn(mockHandler);

    // Factory configured with SHA1
    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA1", null);

    // PHC string contains SHA256
    String phcStringSHA256 =
        "$PBKDF2WithHmacSHA256$iv=a6f7e545dc07ae8b,key_iteration=10000,key_len=256$c2FsdA==$dmFsdWU=";
    PbeCipher cipher = customFactory.create(encryptionKey, phcStringSHA256);
    assertFalse(cipher.isDefaultCipher());
  }

  @Test
  void testIsDefaultCipher_WithDirectCreation_AlwaysTrue() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    when(hashingHandlerFactory.create(anyString(), any(), any())).thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA256", null);

    // Direct creation (not from PHC string) always uses default cipher flag
    PbeCipher cipher1 = customFactory.create(encryptionKey);
    assertTrue(cipher1.isDefaultCipher());

    PbeCipher cipher2 = customFactory.create(encryptionKey, "salt", "iv", 5000);
    assertTrue(cipher2.isDefaultCipher());
  }

  @Test
  void testCreate_IterationsPriorityOrder_ExplicitOverConfigured() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    // Explicit iterations (7000) should take priority over configured (12000)
    when(hashingHandlerFactory.create("PBKDF2WithHmacSHA256", "test-salt".getBytes(), 7000))
        .thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA256", 12000);

    PbeCipher cipher = customFactory.create(encryptionKey, "test-salt", "test-iv", 7000);
    assertNotNull(cipher);
  }

  @Test
  void testCreate_IterationsPriorityOrder_ConfiguredOverNull() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    // When explicit is null, should use configured (12000)
    when(hashingHandlerFactory.create("PBKDF2WithHmacSHA256", "test-salt".getBytes(), 12000))
        .thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA256", 12000);

    PbeCipher cipher = customFactory.create(encryptionKey, "test-salt", "test-iv", null);
    assertNotNull(cipher);
  }

  @Test
  void testCreate_FromPhcString_IterationsInAttributeTakesPrecedence() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    HashingHandler mockHandler = mock(HashingHandler.class);
    // PHC string has key_iteration=9500, configured is 12000
    // PHC iterations should take precedence
    when(hashingHandlerFactory.create(anyString(), any(), eq(9500))).thenReturn(mockHandler);

    PbeCipherFactoryImpl customFactory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory,
        "PBKDF2WithHmacSHA256", 12000);

    String phcString = "$PBKDF2WithHmacSHA256$iv=a6f7e545dc07ae8b,key_iteration=9500,key_len=256$c2FsdA==$dmFsdWU=";
    PbeCipher cipher = customFactory.create(encryptionKey, phcString);
    assertNotNull(cipher);
  }

  // --- HMAC integration tests ---

  /**
   * Builds a factory backed by real JDK crypto (no BouncyCastle dependency) with a fixed 16-byte
   * AES key returned by the mock HashingHandler, so encrypt/decrypt can be exercised end-to-end.
   */
  private PbeCipherFactoryImpl createRealCryptoFactory(HashingHandler fixedHandler) throws Exception {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    when(cryptoHelper.createCipher("AES/CBC/PKCS5Padding"))
        .thenAnswer(inv -> Cipher.getInstance("AES/CBC/PKCS5Padding"));
    // getProvider() returns null → Mac.getInstance(alg, null) throws → falls back to default JCA
    when(cryptoHelper.getProvider()).thenReturn(null);

    HashingHandlerFactory hf = mock(HashingHandlerFactory.class);
    when(hf.create(anyString(), any(), any())).thenReturn(fixedHandler);

    return new PbeCipherFactoryImpl(cryptoHelper, hf, "PBKDF2WithHmacSHA256", null);
  }

  private HashingHandler createFixedKeyHandler() {
    byte[] fixedKey = new byte[16];
    Arrays.fill(fixedKey, (byte) 0x4b);
    String keyBase64 = Base64Variants.getDefaultVariant().encode(fixedKey);
    String saltBase64 = Base64Variants.getDefaultVariant().encode("test-salt-16b".getBytes());

    EncryptedSecret hashResult = new EncryptedSecret(
        "PBKDF2WithHmacSHA256", null, saltBase64, keyBase64,
        ImmutableMap.of("key_iteration", "10000", "key_len", "128"));

    HashingHandler handler = mock(HashingHandler.class);
    when(handler.hash(any())).thenReturn(hashResult);
    return handler;
  }

  @Test
  void testEncryptThenDecrypt_WithHmac_RoundTrips() throws Exception {
    HashingHandler fixedHandler = createFixedKeyHandler();
    PbeCipherFactoryImpl f = createRealCryptoFactory(fixedHandler);

    SecretEncryptionKey testKey = mock(SecretEncryptionKey.class);
    when(testKey.getKey()).thenReturn("test-master-key");

    byte[] plaintext = "hello-rate-limit".getBytes();
    EncryptedSecret encrypted = f.create(testKey).encrypt(plaintext);

    assertNotNull(encrypted.getAttributes().get("hmac"), "HMAC attribute must be present after encryption");

    byte[] result = f.create(testKey, encrypted.toPhcString()).decrypt();
    assertArrayEquals(plaintext, result);
  }

  @Test
  void testDecrypt_WithoutHmac_BackwardCompatibility() throws Exception {
    HashingHandler fixedHandler = createFixedKeyHandler();
    PbeCipherFactoryImpl f = createRealCryptoFactory(fixedHandler);

    SecretEncryptionKey testKey = mock(SecretEncryptionKey.class);
    when(testKey.getKey()).thenReturn("test-master-key");

    byte[] plaintext = "legacy-secret".getBytes();
    EncryptedSecret encrypted = f.create(testKey).encrypt(plaintext);

    // Strip HMAC to simulate a secret encrypted by the pre-HMAC code
    Map<String, String> attrsWithoutHmac = new LinkedHashMap<>(encrypted.getAttributes());
    attrsWithoutHmac.remove("hmac");
    EncryptedSecret legacySecret = new EncryptedSecret(
        encrypted.getAlgorithm(), encrypted.getVersion(),
        encrypted.getSalt(), encrypted.getValue(), attrsWithoutHmac);

    byte[] result = f.create(testKey, legacySecret.toPhcString()).decrypt();
    assertArrayEquals(plaintext, result);
  }

  /**
   * Verifies that {@code decrypt(byte[])} — the overload that accepts raw ciphertext bytes — can
   * round-trip plaintext encrypted by {@code encrypt()}, even though it does NOT verify the HMAC
   * attribute. This confirms the "caller is responsible for integrity" contract documented in the
   * method Javadoc: callers who manage their own byte buffers (e.g. legacy MyBatis type handlers)
   * can still decrypt ciphertext produced by the HMAC-aware {@code encrypt()} method, provided
   * they reconstruct a cipher with the matching key and IV before calling this overload.
   */
  @Test
  void testDecrypt_ByteArrayOverload_RoundTrips_NoHmacVerification() throws Exception {
    HashingHandler fixedHandler = createFixedKeyHandler();
    PbeCipherFactoryImpl f = createRealCryptoFactory(fixedHandler);

    SecretEncryptionKey testKey = mock(SecretEncryptionKey.class);
    when(testKey.getKey()).thenReturn("test-master-key");

    byte[] plaintext = "raw-decrypt-test".getBytes();

    // Use a fixed, known IV so the same value can be used for both encrypt and decrypt ciphers.
    // factory.create(key, salt, iv, iterations) stores iv as iv.getBytes()
    String knownIv = "0123456789abcdef"; // exactly 16 bytes when UTF-8 encoded
    PbeCipher encCipher = f.create(testKey, "test-salt", knownIv, null);
    EncryptedSecret encrypted = encCipher.encrypt(plaintext);

    // Extract the raw AES-CBC ciphertext (the 'value' field of the PHC envelope)
    byte[] rawCiphertext = fromBase64(encrypted.getValue());

    // Re-create a cipher with the identical key/salt/IV — decrypt(byte[]) skips HMAC check
    PbeCipher decCipher = f.create(testKey, "test-salt", knownIv, null);
    byte[] result = decCipher.decrypt(rawCiphertext);

    assertArrayEquals(plaintext, result, "decrypt(byte[]) must round-trip plaintext from encrypt()");
  }

  @Test
  void testDecrypt_WithTamperedHmac_ThrowsCipherException() throws Exception {
    HashingHandler fixedHandler = createFixedKeyHandler();
    PbeCipherFactoryImpl f = createRealCryptoFactory(fixedHandler);

    SecretEncryptionKey testKey = mock(SecretEncryptionKey.class);
    when(testKey.getKey()).thenReturn("test-master-key");

    byte[] plaintext = "secret-data".getBytes();
    EncryptedSecret encrypted = f.create(testKey).encrypt(plaintext);

    // Replace HMAC with an incorrect value
    Map<String, String> tamperedAttrs = new LinkedHashMap<>(encrypted.getAttributes());
    tamperedAttrs.put("hmac", "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
    EncryptedSecret tampered = new EncryptedSecret(
        encrypted.getAlgorithm(), encrypted.getVersion(),
        encrypted.getSalt(), encrypted.getValue(), tamperedAttrs);

    PbeCipher decCipher = f.create(testKey, tampered.toPhcString());
    assertThrows(CipherException.class, decCipher::decrypt);
  }

  @Test
  void testDecrypt_WithTamperedIv_ThrowsCipherException() throws Exception {
    HashingHandler fixedHandler = createFixedKeyHandler();
    PbeCipherFactoryImpl f = createRealCryptoFactory(fixedHandler);

    SecretEncryptionKey testKey = mock(SecretEncryptionKey.class);
    when(testKey.getKey()).thenReturn("test-master-key");

    byte[] plaintext = "secret-data".getBytes();
    EncryptedSecret encrypted = f.create(testKey).encrypt(plaintext);

    // Replace IV with a different value while keeping HMAC and ciphertext intact
    Map<String, String> tamperedAttrs = new LinkedHashMap<>(encrypted.getAttributes());
    tamperedAttrs.put("iv", "00000000000000000000000000000000"); // 16 zero bytes in hex
    EncryptedSecret tampered = new EncryptedSecret(
        encrypted.getAlgorithm(), encrypted.getVersion(),
        encrypted.getSalt(), encrypted.getValue(), tamperedAttrs);

    PbeCipher decCipher = f.create(testKey, tampered.toPhcString());
    assertThrows(CipherException.class, decCipher::decrypt,
        "IV tampering must be detected by HMAC verification");
  }
}
