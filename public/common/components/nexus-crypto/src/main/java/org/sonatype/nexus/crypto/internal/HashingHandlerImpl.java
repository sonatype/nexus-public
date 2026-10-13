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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.HashingHandler;

import com.google.common.collect.ImmutableMap;
import org.springframework.lang.Nullable;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.crypto.internal.EncryptionHelper.KEY_ITERATION_PHC;
import static org.sonatype.nexus.crypto.internal.EncryptionHelper.KEY_LEN_PHC;
import static org.sonatype.nexus.crypto.internal.EncryptionHelper.fromBase64;
import static org.sonatype.nexus.crypto.internal.EncryptionHelper.toBase64;

public class HashingHandlerImpl
    implements HashingHandler
{
  private static final int SALT_SIZE = 16;

  private final SecretKeyFactory keyFactory;

  private final CryptoHelper cryptoHelper;

  private final String algorithm;

  private final byte[] salt;

  private final int keyIterations;

  private final int keyLength;

  public HashingHandlerImpl(
      final CryptoHelper cryptoHelper,
      final String nexusPasswordAlgorithm,
      @Nullable final byte[] salt,
      final int keyIterations,
      final int keyLength) throws CipherException
  {
    this.cryptoHelper = cryptoHelper;
    try {
      this.keyFactory = cryptoHelper.createSecretKeyFactory(nexusPasswordAlgorithm);
    }
    catch (NoSuchAlgorithmException e) {
      throw new CipherException(e.getMessage(), e);
    }
    this.algorithm = nexusPasswordAlgorithm;
    this.salt = salt == null ? generateRandomBytes(SALT_SIZE) : salt;
    this.keyIterations = keyIterations;
    this.keyLength = keyLength;
  }

  public EncryptedSecret hash(final char[] valueToHash) throws CipherException {
    try {
      byte[] encodedHash = generateHash(valueToHash, salt);

      return new EncryptedSecret(algorithm, null, toBase64(salt), toBase64(encodedHash),
          ImmutableMap.of(KEY_ITERATION_PHC, String.valueOf(this.keyIterations),
              KEY_LEN_PHC, String.valueOf(this.keyLength)));
    }
    catch (InvalidKeySpecException e) {
      throw new CipherException(e.getMessage(), e);
    }
  }

  public boolean verify(
      final char[] input,
      final String expectedValue) throws CipherException, InvalidKeySpecException
  {
    requireNonNull(input);
    requireNonNull(expectedValue);

    EncryptedSecret parsedExpectedHash = EncryptedSecret.parse(expectedValue);
    byte[] inputHash = this.generateHash(input, fromBase64(parsedExpectedHash.getSalt()));
    byte[] expectedHash = fromBase64(parsedExpectedHash.getValue());

    return MessageDigest.isEqual(inputHash, expectedHash);
  }

  private byte[] generateHash(final char[] input, final byte[] salt) throws InvalidKeySpecException {
    PBEKeySpec spec = new PBEKeySpec(input, salt, this.keyIterations, this.keyLength);
    try {
      SecretKey secretKey = keyFactory.generateSecret(spec);
      return secretKey.getEncoded();
    }
    finally {
      spec.clearPassword();
    }
  }

  private byte[] generateRandomBytes(final int size) {
    SecureRandom localRandom = cryptoHelper.createSecureRandom();
    byte[] bytes = new byte[size];
    localRandom.nextBytes(bytes);
    return bytes;
  }
}
