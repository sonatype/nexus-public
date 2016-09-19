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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.annotation.concurrent.ThreadSafe;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.sonatype.nexus.crypto.CryptoHelper;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.bouncycastle.util.encoders.Base64Encoder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Cipher implementation compatible with encryption used by plexus-cipher versions [1.6,].
 * 
 * @since 3.0
 */
@ThreadSafe
public class PasswordCipher
{
  private static final int SPICE_SIZE = 16;

  private static final int SALT_SIZE = 8;

  private static final int CHUNK_SIZE = 16;

  private static final String DIGEST_ALG = "SHA-256";

  private static final String KEY_ALG = "AES";

  private static final String CIPHER_ALG = "AES/CBC/PKCS5Padding";

  private final CryptoHelper cryptoHelper;

  private final Base64Encoder base64Encoder;

  private final SecureRandom secureRandom;

  public PasswordCipher(final CryptoHelper cryptoHelper) {
    this.cryptoHelper = checkNotNull(cryptoHelper);
    this.base64Encoder = new Base64Encoder();
    this.secureRandom = cryptoHelper.createSecureRandom();
    this.secureRandom.setSeed(System.nanoTime());
  }

  public byte[] encrypt(final byte[] payload, final String passPhrase) {
    checkNotNull(payload);
    checkNotNull(passPhrase);
    try {
      byte[] salt = new byte[SALT_SIZE];
      secureRandom.nextBytes(salt);
      Cipher cipher = createCipher(passPhrase, salt, Cipher.ENCRYPT_MODE);
      byte[] encryptedBytes = cipher.doFinal(payload);
      int len = encryptedBytes.length;
      byte padLen = (byte) (CHUNK_SIZE - (SALT_SIZE + len + 1) % CHUNK_SIZE);
      int totalLen = SALT_SIZE + len + padLen + 1;
      byte[] allEncryptedBytes = new byte[totalLen];
      secureRandom.nextBytes(allEncryptedBytes);
      System.arraycopy(salt, 0, allEncryptedBytes, 0, SALT_SIZE);
      allEncryptedBytes[SALT_SIZE] = padLen;
      System.arraycopy(encryptedBytes, 0, allEncryptedBytes, SALT_SIZE + 1, len);
      ByteArrayOutputStream bout = new ByteArrayOutputStream(allEncryptedBytes.length * 2);
      base64Encoder.encode(allEncryptedBytes, 0, allEncryptedBytes.length, bout);
      return bout.toByteArray();
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public byte[] decrypt(final byte[] payload, final String passPhrase) {
    checkNotNull(payload);
    checkNotNull(passPhrase);
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      base64Encoder.decode(payload, 0, payload.length, baos);
      byte[] allEncryptedBytes = baos.toByteArray();
      int totalLen = allEncryptedBytes.length;
      byte[] salt = new byte[SALT_SIZE];
      System.arraycopy(allEncryptedBytes, 0, salt, 0, SALT_SIZE);
      byte padLen = allEncryptedBytes[SALT_SIZE];
      byte[] encryptedBytes = new byte[totalLen - SALT_SIZE - 1 - padLen];
      System.arraycopy(allEncryptedBytes, SALT_SIZE + 1, encryptedBytes, 0, encryptedBytes.length);
      Cipher cipher = createCipher(passPhrase, salt, Cipher.DECRYPT_MODE);
      return cipher.doFinal(encryptedBytes);
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Invalid payload (base64 problem)", e);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  // ==

  private Cipher createCipher(final String passPhrase, byte[] salt, final int mode) throws Exception {
    final MessageDigest digester = cryptoHelper.createDigest(DIGEST_ALG); // construction of this is cheap
    byte[] keyAndIv = new byte[SPICE_SIZE * 2];
    if (salt == null || salt.length == 0) {
      // Unsalted! Bad idea!
      salt = null;
    }
    byte[] result;
    int currentPos = 0;
    while (currentPos < keyAndIv.length) {
      digester.update(passPhrase.getBytes(Charsets.UTF_8));
      if (salt != null) {
        // First 8 bytes of salt ONLY! That wasn't obvious to me
        // when using AES encrypted private keys in "Traditional
        // SSLeay Format".
        //
        // Example:
        // DEK-Info: AES-128-CBC,8DA91D5A71988E3D4431D9C2C009F249
        //
        // Only the first 8 bytes are salt, but the whole thing is
        // re-used again later as the IV. MUCH gnashing of teeth!
        digester.update(salt, 0, 8);
      }
      result = digester.digest();
      int stillNeed = keyAndIv.length - currentPos;
      // Digest gave us more than we need. Let's truncate it.
      if (result.length > stillNeed) {
        byte[] b = new byte[stillNeed];
        System.arraycopy(result, 0, b, 0, b.length);
        result = b;
      }
      System.arraycopy(result, 0, keyAndIv, currentPos, result.length);
      currentPos += result.length;
      if (currentPos < keyAndIv.length) {
        // Next round starts with a hash of the hash.
        digester.reset();
        digester.update(result);
      }
    }
    byte[] key = new byte[SPICE_SIZE];
    byte[] iv = new byte[SPICE_SIZE];
    System.arraycopy(keyAndIv, 0, key, 0, key.length);
    System.arraycopy(keyAndIv, key.length, iv, 0, iv.length);
    Cipher cipher = Cipher.getInstance(CIPHER_ALG);
    cipher.init(mode, new SecretKeySpec(key, KEY_ALG), new IvParameterSpec(iv));
    return cipher;
  }
}