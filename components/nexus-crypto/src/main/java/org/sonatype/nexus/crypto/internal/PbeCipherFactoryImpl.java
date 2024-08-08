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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import com.fasterxml.jackson.core.Base64Variants;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.bouncycastle.util.encoders.Hex;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation for  {@link PbeCipherFactory} . provides a simple cipher supporting PHC string format
 */
@Named
@Singleton
public class PbeCipherFactoryImpl
    implements PbeCipherFactory
{
  private final CryptoHelper cryptoHelper;

  @Inject
  public PbeCipherFactoryImpl(final CryptoHelper cryptoHelper) {
    this.cryptoHelper = checkNotNull(cryptoHelper);
  }

  @Override
  public PbeCipher create(final SecretEncryptionKey secretEncryptionKey) throws CipherException {
    checkNotNull(secretEncryptionKey);
    return new PbeCipherImpl(cryptoHelper, secretEncryptionKey);
  }

  private static String toBase64(final byte[] value) {
    return Base64Variants.getDefaultVariant().encode(value);
  }

  private static byte[] fromBase64(final String encoded) {
    return Base64Variants.getDefaultVariant().decode(encoded);
  }

  /**
   * Default {@link PbeCipher} implementation , uses AES_128 with random IV/Salt
   */
  class PbeCipherImpl
      implements PbeCipher
  {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA1";

    private static final String KEY_ALGORITHM = "AES";

    private static final int KEY_ITERATIONS = 1024;

    private static final int KEY_LENGTH = 128;

    private final SecureRandom random;

    private final CryptoHelper cryptoHelper;

    private final SecretEncryptionKey secretEncryptionKey;

    private final SecretKeyFactory keyFactory;

    public PbeCipherImpl(final CryptoHelper cryptoHelper, final SecretEncryptionKey secretEncryptionKey)
        throws CipherException
    {
      this.random = cryptoHelper.createSecureRandom();
      this.cryptoHelper = cryptoHelper;
      this.secretEncryptionKey = secretEncryptionKey;

      try {
        this.keyFactory = cryptoHelper.createSecretKeyFactory(KEY_FACTORY_ALGORITHM);
      }
      catch (NoSuchAlgorithmException e) {
        throw new CipherException(e.getMessage(), e);
      }
    }

    private byte[] randomBytes(final int size) {
      byte[] bytes = new byte[size];

      random.nextBytes(bytes);
      return bytes;
    }

    @Override
    public EncryptedSecret encrypt(final byte[] bytes) throws CipherException {
      try {
        // define random initialization vector & salt with 16 bytes long (128 bits)
        byte[] iv = randomBytes(16);
        byte[] salt = randomBytes(16);

        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv); //NOSONAR
        KeySpec spec = new PBEKeySpec(secretEncryptionKey.getKey().toCharArray(), salt, KEY_ITERATIONS, KEY_LENGTH);
        SecretKey tmp = keyFactory.generateSecret(spec);
        SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);

        byte[] encrypted = transform(Cipher.ENCRYPT_MODE, secretKey, paramSpec, bytes);

        return new EncryptedSecret(ALGORITHM, null, toBase64(salt), toBase64(encrypted),
            ImmutableMap.of("iv", Hex.toHexString(iv),
                "key_iteration", String.valueOf(KEY_ITERATIONS),
                "key_len", String.valueOf(KEY_LENGTH)));
      }
      catch (InvalidKeySpecException e) {
        throw new CipherException(e.getMessage(), e);
      }
    }

    @Override
    public byte[] decrypt(final EncryptedSecret secret) throws CipherException {
      try {
        byte[] iv = Hex.decode(secret.getAttributes().get("iv"));
        byte[] salt = fromBase64(secret.getSalt());
        byte[] encrypted = fromBase64(secret.getValue());
        int iterations = Integer.parseInt(secret.getAttributes().get("key_iteration"));
        int length = Integer.parseInt(secret.getAttributes().get("key_len"));

        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv); //NOSONAR
        KeySpec spec = new PBEKeySpec(secretEncryptionKey.getKey().toCharArray(), salt, iterations, length);
        SecretKey tmp = keyFactory.generateSecret(spec);
        SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);

        return transform(Cipher.DECRYPT_MODE, secretKey, paramSpec, encrypted);
      }
      catch (InvalidKeySpecException e) {
        throw new CipherException(e.getMessage(), e);
      }
    }

    private byte[] transform(
        final int mode,
        final SecretKey secretKey,
        final AlgorithmParameterSpec paramSpec,
        final byte[] bytes) throws CipherException
    {
      try {
        Cipher cipher = cryptoHelper.createCipher(ALGORITHM);
        cipher.init(mode, secretKey, paramSpec);
        return cipher.doFinal(bytes, 0, bytes.length);
      }
      catch (Exception e) {
        Throwables.throwIfUnchecked(e);
        throw new CipherException(e.getMessage(), e);
      }
    }
  }
}
