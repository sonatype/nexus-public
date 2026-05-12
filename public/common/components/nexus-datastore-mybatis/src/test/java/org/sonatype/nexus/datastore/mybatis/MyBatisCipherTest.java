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
package org.sonatype.nexus.datastore.mybatis;

import java.util.Optional;

import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.FixedEncryption;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeySource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MyBatisCipherTest
{
  private static final String PASSWORD = "test-password";

  private static final String SALT = "test-salt";

  private static final String IV = "test-iv-1234567";

  private static final byte[] PLAIN_TEXT = "secret-data".getBytes();

  private static final byte[] ENCRYPTED_BYTES = "encrypted-data".getBytes();

  @Mock
  private PbeCipherFactory pbeCipherFactory;

  @Mock
  private EncryptionKeySource encryptionKeySource;

  @Mock
  private PbeCipher mockPbeCipher;

  private MyBatisCipher underTest;

  @Before
  public void setup() {
    when(encryptionKeySource.getFixedEncryption()).thenReturn(Optional.empty());
    when(pbeCipherFactory.create(any(), eq(SALT), eq(IV), eq(null))).thenReturn(mockPbeCipher);

    underTest = new MyBatisCipher(PASSWORD, SALT, IV, pbeCipherFactory, encryptionKeySource);
  }

  @Test
  public void testEncrypt() {
    EncryptedSecret encryptedSecret = EncryptedSecret.parse(
        "$PBKDF2WithHmacSHA256$iv=abc,key_iteration=10000,key_len=256$c2FsdA==$ZW5jcnlwdGVkLWRhdGE=");
    when(mockPbeCipher.encrypt(PLAIN_TEXT)).thenReturn(encryptedSecret);

    byte[] result = underTest.encrypt(PLAIN_TEXT);

    assertNotNull(result);
    assertArrayEquals(ENCRYPTED_BYTES, result);
    verify(mockPbeCipher).encrypt(PLAIN_TEXT);
  }

  @Test
  public void testDecrypt() {
    when(mockPbeCipher.decrypt(ENCRYPTED_BYTES)).thenReturn(PLAIN_TEXT);

    byte[] result = underTest.decrypt(ENCRYPTED_BYTES);

    assertNotNull(result);
    assertArrayEquals(PLAIN_TEXT, result);
    verify(mockPbeCipher).decrypt(ENCRYPTED_BYTES);
  }

  @Test
  public void testConstructor_WithNoFixedEncryption_UsesDefaults() {
    // Create a fresh instance separate from setup
    when(encryptionKeySource.getFixedEncryption()).thenReturn(Optional.empty());

    // Reset the mock to clear previous interactions from setup
    org.mockito.Mockito.reset(pbeCipherFactory);
    when(pbeCipherFactory.create(any(), eq(SALT), eq(IV), eq(null))).thenReturn(mockPbeCipher);

    MyBatisCipher cipher = new MyBatisCipher(PASSWORD, SALT, IV, pbeCipherFactory, encryptionKeySource);

    assertNotNull(cipher);
    ArgumentCaptor<SecretEncryptionKey> keyCaptor = ArgumentCaptor.forClass(SecretEncryptionKey.class);
    verify(pbeCipherFactory).create(keyCaptor.capture(), eq(SALT), eq(IV), eq(null));
    assertThat(keyCaptor.getValue().getKey(), is(equalTo(PASSWORD)));
  }

  @Test
  public void testConstructor_WithFixedEncryption_UsesConfiguredKeyAndSaltIv() {
    SecretEncryptionKey customKey = new SecretEncryptionKey("custom-key-id", "custom-password");
    FixedEncryption fixedEncryption =
        new FixedEncryption("custom-key-id", "custom-salt", "custom-iv", Integer.valueOf(15000));

    when(encryptionKeySource.getFixedEncryption()).thenReturn(Optional.of(fixedEncryption));
    when(encryptionKeySource.getKey("custom-key-id")).thenReturn(Optional.of(customKey));
    when(pbeCipherFactory.create(customKey, "custom-salt", "custom-iv", null)).thenReturn(mockPbeCipher);

    MyBatisCipher cipher = new MyBatisCipher(PASSWORD, SALT, IV, pbeCipherFactory, encryptionKeySource);

    assertNotNull(cipher);
    verify(pbeCipherFactory).create(customKey, "custom-salt", "custom-iv", null);
  }

  @Test
  public void testConstructor_WithFixedEncryptionPartialConfig_UsesDefaults() {
    FixedEncryption fixedEncryption = new FixedEncryption("custom-key-id", null, null, null);
    SecretEncryptionKey customKey = new SecretEncryptionKey("custom-key-id", "custom-password");

    when(encryptionKeySource.getFixedEncryption()).thenReturn(Optional.of(fixedEncryption));
    when(encryptionKeySource.getKey("custom-key-id")).thenReturn(Optional.of(customKey));
    when(pbeCipherFactory.create(customKey, SALT, IV, null)).thenReturn(mockPbeCipher);

    MyBatisCipher cipher = new MyBatisCipher(PASSWORD, SALT, IV, pbeCipherFactory, encryptionKeySource);

    assertNotNull(cipher);
    verify(pbeCipherFactory).create(customKey, SALT, IV, null);
  }

  @Test
  public void testConstructor_WithCustomSaltAndIv() {
    FixedEncryption fixedEncryption = new FixedEncryption(null, "json-salt", "json-iv-123456", Integer.valueOf(99999));

    when(encryptionKeySource.getFixedEncryption()).thenReturn(Optional.of(fixedEncryption));
    when(pbeCipherFactory.create(any(), eq("json-salt"), eq("json-iv-123456"), eq(null))).thenReturn(mockPbeCipher);

    MyBatisCipher cipher = new MyBatisCipher(PASSWORD, SALT, IV, pbeCipherFactory, encryptionKeySource);

    assertNotNull(cipher);
    ArgumentCaptor<SecretEncryptionKey> keyCaptor = ArgumentCaptor.forClass(SecretEncryptionKey.class);
    verify(pbeCipherFactory).create(keyCaptor.capture(), eq("json-salt"), eq("json-iv-123456"), eq(null));
    assertThat(keyCaptor.getValue().getKey(), is(equalTo(PASSWORD)));
  }

  @Test
  public void testEncryptDecrypt_RoundTrip() {
    // Test that encrypt -> decrypt returns original data
    EncryptedSecret encryptedSecret = EncryptedSecret.parse(
        "$PBKDF2WithHmacSHA256$iv=abc,key_iteration=10000,key_len=256$c2FsdA==$ZW5jcnlwdGVkLWRhdGE=");
    when(mockPbeCipher.encrypt(PLAIN_TEXT)).thenReturn(encryptedSecret);
    when(mockPbeCipher.decrypt(ENCRYPTED_BYTES)).thenReturn(PLAIN_TEXT);

    byte[] encrypted = underTest.encrypt(PLAIN_TEXT);
    byte[] decrypted = underTest.decrypt(encrypted);

    assertArrayEquals(PLAIN_TEXT, decrypted);
  }
}
