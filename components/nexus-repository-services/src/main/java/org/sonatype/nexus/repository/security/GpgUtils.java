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
package org.sonatype.nexus.repository.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.io.ByteStreams;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.bouncycastle.bcpg.HashAlgorithmTags.SHA256;
import static org.bouncycastle.openpgp.PGPSignature.BINARY_DOCUMENT;
import static org.bouncycastle.openpgp.PGPSignature.CANONICAL_TEXT_DOCUMENT;

/**
 * GnuPG utils to sign data.
 *
 * @since 3.30
 */
public class GpgUtils
{
  private static final Logger LOG = LoggerFactory.getLogger(GpgUtils.class);

  private static final char[] EMPTY_PASSPHRASE = new char[0];

  private static final String BC_PROVIDER = "BC";

  public static class SigningConfig
  {
    private String keypair;

    private String passphrase;

    public String getKeypair() {
      return keypair;
    }

    public void setKeypair(final String keypair) {
      this.keypair = keypair;
    }

    public String getPassphrase() {
      return passphrase;
    }

    public void setPassphrase(final String passphrase) {
      this.passphrase = passphrase;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SigningConfig that = (SigningConfig) o;
      return Objects.equals(keypair, that.keypair) &&
          Objects.equals(passphrase, that.passphrase);
    }

    @Override
    public int hashCode() {
      return Objects.hash(keypair, passphrase);
    }
  }

  private GpgUtils() {
  }

  /**
   * Check is configured GPG key pair.
   *
   * @param secretKey  the GPG secret/private key.
   * @param passphrase the password for the secret key or {@code null}.
   * @return whether GPG key is configured and accessible.
   */
  public static boolean isConfigured(final String secretKey, @Nullable final String passphrase) {
    if (StringUtils.isBlank(secretKey)) {
      return false;
    }
    try {
      PBESecretKeyDecryptor bc = getSecretKeyDecryptor(passphrase);
      PGPSecretKey pgpSecretKey = readSecretKey(secretKey);
      PGPPrivateKey pgpPrivateKey = pgpSecretKey.extractPrivateKey(bc);
      return Objects.nonNull(pgpPrivateKey);
    }
    catch (PGPException | IOException e) {
      LOG.warn("GPG key configured, but unexpected error occurred while trying to use it. " +
          "Please check your keypair and passphrase.", e);
    }
    return false;
  }

  /**
   * Sign input data with GPG.
   *
   * @param secretKey  the GPG secret/private key.
   * @param passphrase the password for the secret key or {@code null}.
   * @return ASCII-armored GPG signature for a given input
   */
  public static byte[] signExternal(final InputStream input, final String secretKey, @Nullable final String passphrase)
      throws IOException
  {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      PGPSignatureGenerator sigGenerator = initPgpSignatureGenerator(secretKey, passphrase, BINARY_DOCUMENT);
      try (ArmoredOutputStream aOut = new ArmoredOutputStream(buffer)) {
        BCPGOutputStream bOut = new BCPGOutputStream(aOut);
        sigGenerator.update(ByteStreams.toByteArray(input));
        sigGenerator.generate().encode(bOut);
      }
    }
    catch (PGPException e) {
      throw new RuntimeException(e); //NOSONAR
    }

    return buffer.toByteArray();
  }

  /**
   * Sign input data with GPG.
   *
   * @param secretKey  the GPG secret/private key.
   * @param passphrase the password for the secret key.
   * @return ASCII-armored GPG signature for a given input.
   */
  public static byte[] signInline(final String input, final String secretKey, final String passphrase)
      throws IOException
  {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      PGPSecretKey signKey = readSecretKey(secretKey);
      PGPSignatureGenerator sigGenerator = initPgpSignatureGenerator(secretKey, passphrase, CANONICAL_TEXT_DOCUMENT);

      Iterator<String> userIds = signKey.getUserIDs();
      if (userIds.hasNext()) {
        PGPSignatureSubpacketGenerator sigSubpacketGenerator = new PGPSignatureSubpacketGenerator();
        sigSubpacketGenerator.setSignerUserID(false, userIds.next());
        sigGenerator.setHashedSubpackets(sigSubpacketGenerator.generate());
      }

      String[] lines = input.split("\r?\n");
      try (ArmoredOutputStream aOut = new ArmoredOutputStream(buffer)) {
        aOut.beginClearText(SHA256);

        boolean firstLine = true;
        for (String line : lines) {
          String normalizedLine = line.replaceAll("\\s*$", "");
          String sigLine = (firstLine ? "" : "\r\n") + normalizedLine;
          sigGenerator.update(sigLine.getBytes(UTF_8));
          aOut.write((normalizedLine + "\n").getBytes(UTF_8));
          firstLine = false;
        }
        aOut.endClearText();

        sigGenerator.generate().encode(new BCPGOutputStream(aOut));
      }
    }
    catch (PGPException e) {
      throw new RuntimeException(e); //NOSONAR
    }
    return buffer.toByteArray();
  }

  /**
   * Get public key from the secret one.
   *
   * @param secretKey the the GPG secret/private key.
   * @return the {@link PGPPublicKey} object.
   */
  public static PGPPublicKey getPublicKey(final String secretKey) throws IOException {
    PGPSecretKey signKey = readSecretKey(secretKey);
    return signKey.getPublicKey();
  }

  /**
   * Get the signing key from the secret key.
   *
   * @param secretKey the GPG secret/private key.
   * @return the {@link PGPSecretKey} object.
   */
  private static PGPSecretKey readSecretKey(final String secretKey) throws IOException {
    try (InputStream decoderStream = PGPUtil.getDecoderStream(
        new ByteArrayInputStream(secretKey.getBytes(UTF_8)))) {
      PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
          decoderStream, new JcaKeyFingerprintCalculator());

      Iterator<PGPSecretKeyRing> keyRings = pgpSec.getKeyRings();
      while (keyRings.hasNext()) {
        PGPSecretKeyRing keyRing = keyRings.next();
        Iterator<PGPSecretKey> keys = keyRing.getSecretKeys();
        while (keys.hasNext()) {
          PGPSecretKey key = keys.next();
          if (key.isSigningKey()) {
            return key;
          }
        }
      }
    }
    catch (PGPException e) {
      throw new RuntimeException(e); //NOSONAR
    }

    throw new IllegalStateException("Can't find signing key in key ring.");
  }

  private static PGPSignatureGenerator initPgpSignatureGenerator(
      final String secretKey,
      final String passphrase,
      final int pgpSignature)
      throws IOException, PGPException
  {
    PBESecretKeyDecryptor keyDecryptor = getSecretKeyDecryptor(passphrase);
    PGPSecretKey signKey = readSecretKey(secretKey);
    PGPPrivateKey privateKey = signKey.extractPrivateKey(keyDecryptor);
    PGPSignatureGenerator sigGenerator = new PGPSignatureGenerator(
        new JcaPGPContentSignerBuilder(signKey.getPublicKey().getAlgorithm(), SHA256)
            .setProvider(BC_PROVIDER));
    sigGenerator.init(pgpSignature, privateKey);
    return sigGenerator;
  }

  private static PBESecretKeyDecryptor getSecretKeyDecryptor(final String passphrase) throws PGPException {
    char[] pass = StringUtils.isNotBlank(passphrase) ? passphrase.toCharArray() : EMPTY_PASSPHRASE;
    return new JcePBESecretKeyDecryptorBuilder()
        .setProvider(BC_PROVIDER)
        .build(pass);
  }
}
