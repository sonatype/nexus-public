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
package org.sonatype.nexus.orient;

import java.security.spec.AlgorithmParameterSpec;
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

import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.compression.OCompression;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of OrientDB's {@code OCompression} interface to provide password-based-encryption of stored records.
 * 
 * @since 3.0
 */
@Singleton
@Named(PbeCompression.NAME)
public class PbeCompression
    implements OCompression
{
  public static final String NAME = "pbe";

  private static final String CPREFIX = "${nexus.orient." + NAME;

  private final CryptoHelper cryptoHelper;

  private final AlgorithmParameterSpec paramSpec;

  private final SecretKey secretKey;

  @Inject
  public PbeCompression(final CryptoHelper cryptoHelper,
                        final @Named(CPREFIX + ".password:-changeme}") String password,
                        final @Named(CPREFIX + ".salt:-changeme}") String salt,
                        final @Named(CPREFIX + ".iv:-0123456789ABCDEF}") String iv)
      throws Exception
  {
    this.cryptoHelper = checkNotNull(cryptoHelper);

    checkNotNull(iv);
    this.paramSpec = new IvParameterSpec(iv.getBytes());

    checkNotNull(password);
    checkNotNull(salt);
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 1024, 128);
    SecretKeyFactory factory = cryptoHelper.createSecretKeyFactory("PBKDF2WithHmacSHA1");
    SecretKey tmp = factory.generateSecret(spec);
    this.secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
  }

  @Override
  public byte[] compress(final byte[] bytes) {
    return compress(bytes, 0, bytes.length);
  }

  @Override
  public byte[] uncompress(final byte[] bytes) {
    return uncompress(bytes, 0, bytes.length);
  }

  @Override
  public byte[] compress(final byte[] bytes, final int i, final int i2) {
    return transform(Cipher.ENCRYPT_MODE, bytes, i, i2);
  }

  @Override
  public byte[] uncompress(final byte[] bytes, final int i, final int i2) {
    return transform(Cipher.DECRYPT_MODE, bytes, i, i2);
  }

  private byte[] transform(final int mode, final byte[] bytes, final int i, final int i2) {
    try {
      Cipher cipher = cryptoHelper.createCipher("AES/CBC/PKCS5Padding");
      cipher.init(mode, secretKey, paramSpec);
      return cipher.doFinal(bytes, i, i2);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String name() {
    return NAME;
  }
}
