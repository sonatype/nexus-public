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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * RSA utility methods for signing and key management.
 */
public class RsaUtils
{
  private static final Pattern PRIVATE_KEY_PEM_PATTERN = Pattern.compile(
      "-----BEGIN (?:RSA PRIVATE KEY|PRIVATE KEY|ENCRYPTED PRIVATE KEY)-----.*?-----END (?:RSA PRIVATE KEY|PRIVATE KEY|ENCRYPTED PRIVATE KEY)-----",
      Pattern.DOTALL);

  private RsaUtils() {
  }

  /**
   * Generates a stable short key name from the PEM key.
   *
   * @param pem the PEM-encoded private key
   * @return a short key name (8 hex chars after "key-")
   */
  public static String generateKeyName(final String pem) {
    String cleanPem;
    try {
      cleanPem = extractPrivateKeyPem(pem)
          .replaceAll("-----BEGIN [^-]+-----", "")
          .replaceAll("-----END [^-]+-----", "")
          .replaceAll("\\s", "");
    }
    catch (IOException e) {
      throw new IllegalArgumentException("No private key PEM block found", e);
    }

    byte[] keyBytes = cleanPem.getBytes(StandardCharsets.UTF_8);
    byte[] hash;
    try {
      hash = MessageDigest.getInstance("SHA-256").digest(keyBytes);
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 digest is not available", e);
    }

    StringBuilder builder = new StringBuilder();
    for (int i = hash.length - 4; i < hash.length; i++) {
      builder.append(String.format("%02x", hash[i]));
    }
    return "key-" + builder;
  }

  /**
   * Signs content with SHA1withRSA.
   *
   * SHA1withRSA computes SHA1 internally then RSA-signs the DigestInfo structure,
   * matching {@code openssl dgst -sha1 -sign key.pem}.
   *
   * @param content the content bytes to sign
   * @param privateKeyPem the PEM-encoded private key
   * @param passphrase the private key passphrase
   * @return the RSA signature bytes
   */
  public static byte[] sign(
      final byte[] content,
      final String privateKeyPem,
      final String passphrase) throws IOException
  {
    PrivateKey privateKey = parsePrivateKey(privateKeyPem, passphrase);

    try {
      Signature signature = Signature.getInstance("SHA1withRSA");
      signature.initSign(privateKey);
      signature.update(content);
      return signature.sign();
    }
    catch (GeneralSecurityException e) {
      throw new IOException("Failed to sign content", e);
    }
  }

  /**
   * Gets the RSA public key derived from the configured private key.
   *
   * @param privateKeyPem the PEM-encoded private key
   * @param passphrase the private key passphrase
   * @return the public key in PEM format
   */
  public static byte[] getPublicKey(final String privateKeyPem, final String passphrase) throws IOException {
    PrivateKey privateKey = parsePrivateKey(privateKeyPem, passphrase);
    PublicKey publicKey = derivePublicKey(privateKey);

    StringWriter sw = new StringWriter();
    try (PemWriter writer = new PemWriter(sw)) {
      writer.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
    }

    return sw.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Derives the RSA public key from a CRT-format private key.
   */
  private static PublicKey derivePublicKey(final PrivateKey privateKey) throws IOException {
    if (!(privateKey instanceof RSAPrivateCrtKey)) {
      throw new IOException("Cannot derive public key: private key is not RSA CRT format");
    }

    RSAPrivateCrtKey rsaKey = (RSAPrivateCrtKey) privateKey;
    try {
      RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(rsaKey.getModulus(), rsaKey.getPublicExponent());
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(publicKeySpec);
    }
    catch (GeneralSecurityException e) {
      throw new IOException("Failed to derive public key from private key", e);
    }
  }

  /**
   * Parses an RSA private key from PEM, optionally decrypting with a passphrase.
   *
   * Supports PKCS#1 ({@code BEGIN RSA PRIVATE KEY}), PKCS#8 ({@code BEGIN PRIVATE KEY}),
   * and encrypted PKCS#8 ({@code BEGIN ENCRYPTED PRIVATE KEY}). Any text outside the PEM block
   * is ignored so UI validation prefixes do not corrupt the key material.
   */
  private static PrivateKey parsePrivateKey(final String pem, final String passphrase) throws IOException {
    try {
      String pemBlock = extractPrivateKeyPem(pem);
      char[] password = passphrase != null ? passphrase.toCharArray() : new char[0];
      JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

      try (PEMParser pemParser = new PEMParser(new StringReader(pemBlock))) {
        Object parsed = pemParser.readObject();
        if (parsed == null) {
          throw new IOException("No private key PEM block found");
        }

        if (parsed instanceof PEMEncryptedKeyPair encryptedKeyPair) {
          PEMDecryptorProvider decryptor = new JcePEMDecryptorProviderBuilder().build(password);
          return keyConverter.getPrivateKey(encryptedKeyPair.decryptKeyPair(decryptor).getPrivateKeyInfo());
        }
        if (parsed instanceof PEMKeyPair keyPair) {
          return keyConverter.getPrivateKey(keyPair.getPrivateKeyInfo());
        }
        if (parsed instanceof PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo) {
          InputDecryptorProvider decryptor =
              new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password);
          PrivateKeyInfo privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptor);
          return keyConverter.getPrivateKey(privateKeyInfo);
        }
        if (parsed instanceof PrivateKeyInfo privateKeyInfo) {
          return keyConverter.getPrivateKey(privateKeyInfo);
        }

        throw new IOException("Unsupported private key format: " + parsed.getClass().getName());
      }
    }
    catch (OperatorCreationException | PKCSException e) {
      throw new IOException("Failed to decrypt RSA private key", e);
    }
    catch (Exception e) {
      throw new IOException("Failed to parse RSA private key", e);
    }
  }

  private static String extractPrivateKeyPem(final String pem) throws IOException {
    Matcher matcher = PRIVATE_KEY_PEM_PATTERN.matcher(pem);
    if (!matcher.find()) {
      throw new IOException("No private key PEM block found");
    }
    return matcher.group();
  }
}
