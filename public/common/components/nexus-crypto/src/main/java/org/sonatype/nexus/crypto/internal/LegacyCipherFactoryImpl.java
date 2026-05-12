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
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.LegacyCipherFactory;
import org.sonatype.nexus.crypto.internal.error.CipherException;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link LegacyCipherFactory} interface to provide password-based-encryption.
 *
 * @since 3.0
 * @deprecated
 */
@Component
@Singleton
@Deprecated
public class LegacyCipherFactoryImpl
    implements LegacyCipherFactory
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final CryptoHelper cryptoHelper;

  @Inject
  public LegacyCipherFactoryImpl(final CryptoHelper cryptoHelper) {
    this.cryptoHelper = checkNotNull(cryptoHelper);
  }

  @Override
  public PbeCipher create(final String password, final String salt, final String iv) throws CipherException {
    checkNotNull(password);
    checkNotNull(salt);
    checkNotNull(iv);
    return new PbeCipherImpl(cryptoHelper, password, salt, iv);
  }

  private static class PbeCipherImpl
      implements PbeCipher
  {
    private final CryptoHelper cryptoHelper;

    private final AlgorithmParameterSpec paramSpec;

    private final SecretKey secretKey;

    public PbeCipherImpl(
        final CryptoHelper cryptoHelper,
        final String password,
        final String salt,
        final String iv) throws CipherException
    {
      this.cryptoHelper = cryptoHelper;

      try {
        this.paramSpec = new IvParameterSpec(iv.getBytes()); // NOSONAR
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 1024, 128);
        SecretKeyFactory factory = cryptoHelper.createSecretKeyFactory("PBKDF2WithHmacSHA1");
        SecretKey tmp = factory.generateSecret(spec);
        this.secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
      }
      catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new CipherException(e.getMessage(), e);
      }
    }

    @Override
    public byte[] encrypt(final byte[] bytes) {
      return transform(Cipher.ENCRYPT_MODE, bytes);
    }

    @Override
    public byte[] decrypt(final byte[] bytes) {
      return transform(Cipher.DECRYPT_MODE, bytes);
    }

    private byte[] transform(final int mode, final byte[] bytes) {
      try {
        Cipher cipher = cryptoHelper.createCipher("AES/CBC/PKCS5Padding");
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
