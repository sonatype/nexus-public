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

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.HashingHandler;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl.PbeCipherImpl;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PbeCipherImplTest
{

  private CryptoHelper cryptoHelper;

  private SecretEncryptionKey secretKey;

  private HashingHandler hashingHandler;

  @BeforeEach
  void setup() {
    cryptoHelper = new TestCryptoHelper();
    secretKey = new SecretEncryptionKey("my-key-id", "password123");
    hashingHandler = new HashingHandlerImpl(cryptoHelper, "PBKDF2WithHmacSHA1", "salt".getBytes(), 1024, 128);
  }

  @Test
  void test_encrypt_a_value_and_decrypt_with_the_storedSecret() {
    PbeCipherImpl cipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, true, null);
    byte[] plaintext = "Sensitive_data".getBytes();

    EncryptedSecret encrypted = cipher.encrypt(plaintext);
    assertNotNull(encrypted);
    assertEquals("PBKDF2WithHmacSHA1", encrypted.getAlgorithm());

    // Decrypt with stored secret
    PbeCipherImpl decryptCipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, encrypted, true, null);
    byte[] decrypted = decryptCipher.decrypt();

    assertArrayEquals(plaintext, decrypted);
  }

  @Test
  void test_is_default_cipher() throws CipherException {
    PbeCipherImpl cipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, true, null);
    assertTrue(cipher.isDefaultCipher());

    PbeCipherImpl cipher2 = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, false, null);
    assertFalse(cipher2.isDefaultCipher());
  }

  @Test
  void testEncryptDecryptWithEmptyString() {
    PbeCipherImpl cipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, true, null);
    String plaintext = "";

    EncryptedSecret encrypted = cipher.encrypt(plaintext.getBytes());
    assertNotNull(encrypted);

    PbeCipherImpl decryptCipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, encrypted, true, null);
    byte[] decrypted = decryptCipher.decrypt();

    assertArrayEquals(plaintext.getBytes(), decrypted);
  }

  @Test
  void testEncryptDecryptWithSpecialCharacters() {
    PbeCipherImpl cipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, true, null);
    String plaintext = "!@#$%^&*()_+";

    EncryptedSecret encrypted = cipher.encrypt(plaintext.getBytes());
    assertNotNull(encrypted);

    PbeCipherImpl decryptCipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, encrypted, true, null);
    byte[] decrypted = decryptCipher.decrypt();

    assertArrayEquals(plaintext.getBytes(), decrypted);
  }

  @Test
  void testEncryptDecryptWithLargeInput() {
    PbeCipherImpl cipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, true, null);
    String largeInput = new String(new char[10000]).replace("\0", "x");

    EncryptedSecret encrypted = cipher.encrypt(largeInput.getBytes());
    assertNotNull(encrypted);

    PbeCipherImpl decryptCipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, encrypted, true, null);
    byte[] decrypted = decryptCipher.decrypt();

    assertArrayEquals(largeInput.getBytes(), decrypted);
  }

  @Test
  void testEncryptWithNullInputThrowsException() {
    PbeCipherImpl cipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, true, null);

    assertThrows(NullPointerException.class, () -> cipher.encrypt(null));
  }

  @Test
  void testEncryptDecryptWithAwsS3SecretKeyFormat() {
    PbeCipherImpl cipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, true, null);
    String awsSecretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY=";

    EncryptedSecret encrypted = cipher.encrypt(awsSecretKey.getBytes());
    assertNotNull(encrypted);

    PbeCipherImpl decryptCipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, encrypted, true, null);
    byte[] decrypted = decryptCipher.decrypt();

    assertArrayEquals(awsSecretKey.getBytes(), decrypted);
  }

  @Test
  void testEncryptDecryptWithBase64PaddingCharacters() {
    PbeCipherImpl cipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, null, true, null);

    String[] testSecrets = {
        "mySecret=Key123=",
        "mySecret+Key+123",
        "mySecret/Key/123",
        "mySecret=/+Key=/+123=/+",
        "wJalrXUtnFEMI/K7MDENG+bPxRfiCY=EXAMPLEKEY"
    };

    for (String secret : testSecrets) {
      EncryptedSecret encrypted = cipher.encrypt(secret.getBytes());
      assertNotNull(encrypted, "Encryption failed for: " + secret);

      PbeCipherImpl decryptCipher = new PbeCipherImpl(cryptoHelper, hashingHandler, secretKey, encrypted, true, null);
      byte[] decrypted = decryptCipher.decrypt();

      assertArrayEquals(secret.getBytes(), decrypted, "Round-trip failed for: " + secret);
    }
  }
}
