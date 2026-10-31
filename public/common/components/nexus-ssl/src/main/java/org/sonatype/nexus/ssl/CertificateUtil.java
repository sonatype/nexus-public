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
package org.sonatype.nexus.ssl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.sonatype.nexus.common.text.Strings2;

import com.google.common.base.Throwables;
import com.google.common.hash.Hashing;
import jakarta.annotation.Nullable;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Simple utility methods when dealing with certificates.
 * <p/>
 * In order to use the methods decodePEMFormattedCertificate or serializeCertificateInPEM, the BouncyCastleProvider
 * must be registered. <BR/>
 * <BR/>
 *
 * <pre>
 * <code>
 *    if (Security.getProvider("BC") == null) {
 *        Security.addProvider(new BouncyCastleProvider());
 *    }
 * </code>
 * </pre>
 *
 * @since 3.0
 */
public final class CertificateUtil
{
  private static final Logger log = LoggerFactory.getLogger(CertificateUtil.class);

  private CertificateUtil() {
    // empty
  }

  public static X509Certificate generateCertificate(
      final PublicKey publicKey,
      final PrivateKey privateKey,
      final String algorithm,
      final int validDays,
      final String commonName,
      final String orgUnit,
      final String organization,
      final String locality,
      final String state,
      final String country) throws CertificateEncodingException
  {

    X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);

    if (commonName != null) {
      nameBuilder.addRDN(BCStyle.CN, commonName);
    }
    if (orgUnit != null) {
      nameBuilder.addRDN(BCStyle.OU, orgUnit);
    }
    if (organization != null) {
      nameBuilder.addRDN(BCStyle.O, organization);
    }
    if (locality != null) {
      nameBuilder.addRDN(BCStyle.L, locality);
    }
    if (state != null) {
      nameBuilder.addRDN(BCStyle.ST, state);
    }
    if (country != null) {
      nameBuilder.addRDN(BCStyle.C, country);
    }

    X500Name issuerDN = nameBuilder.build();

    long now = System.currentTimeMillis();
    long expire = now + (long) validDays * 24 * 60 * 60 * 1000;

    Date startDate = new Date(now);
    Date endDate = new Date(expire);
    BigInteger serialNumber = BigInteger.valueOf(now);

    X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
        issuerDN, serialNumber, startDate, endDate, issuerDN, publicKey);

    try {
      ContentSigner signer = new JcaContentSignerBuilder(algorithm).build(privateKey);
      X509CertificateHolder certificateHolder = certificateBuilder.build(signer);
      return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }
    catch (Exception e) {
      throw new CertificateEncodingException("Error generating X.509 certificate", e);
    }
  }

  /**
   * Serialize a certificate into a PEM formatted String.
   *
   * @param certificate the certificate to be serialized.
   * @return the certificate in PEM format
   * @throws IOException thrown if the certificate cannot be converted into the PEM format.
   */
  public static String serializeCertificateInPEM(final Certificate certificate) throws IOException {
    StringWriter buff = new StringWriter();
    try (JcaPEMWriter writer = new JcaPEMWriter(buff)) {
      writer.writeObject(certificate);
    }
    return buff.toString();
  }

  /**
   * Decodes a PEM formatted certificate.
   *
   * @param pemFormattedCertificate text to be decoded as a PEM certificate.
   * @return the Certificate decoded from the input text.
   * @throws CertificateParsingException
   *           thrown if the PEM formatted string cannot be parsed into a Certificate.
   */
  public static Certificate decodePEMFormattedCertificate(
      final String pemFormattedCertificate) throws CertificateException
  {
    log.trace("Parsing PEM formatted certificate string:\n{}", pemFormattedCertificate);

    Object object = decodePEMFormatted(pemFormattedCertificate);
    log.trace("Object found while paring PEM formatted string: {}", object);

    if (object instanceof X509CertificateHolder holder) {
      JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
      return converter.getCertificate(holder);
    }
    else if (object != null) {
      log.debug("Unexpected certificate type {}", object.getClass().getName());
    }

    // cert was not a valid object
    throw new CertificateParsingException(
        "Failed to parse valid certificate from expected PEM formatted certificate:\n" + pemFormattedCertificate);
  }

  /**
   * Creates an unencrypted PEM formatted string. The result should not be returned to users and should only be
   * persisted through the SecretsService.
   *
   * @param privateKey a PEM formatted private key
   * @return the unencrypted PEM formatted string
   *
   * @throws InvalidKeyException if the key is invalid, or cannot be decrypted
   */
  public static String removePemPassphrase(
      final String pemPrivateKey,
      @Nullable final String passphrase) throws InvalidKeyException
  {
    PrivateKey privateKey = decodePEMFormattedPrivateKey(pemPrivateKey, passphrase);

    StringWriter pemWriter = new StringWriter();
    try (JcaPEMWriter writer = new JcaPEMWriter(pemWriter)) {
      writer.writeObject(privateKey);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return pemWriter.toString();
  }

  /**
   * Decodes an unencrypted PEM formatted private key.
   *
   * @param pemFormattedPrivateKey text to be decoded as a PEM private key.
   * @return the PrivateKey decoded from the input text.
   * @throws CertificateParsingException
   *           thrown if the PEM formatted string cannot be parsed into a PrivateKey.
   */
  public static PrivateKey decodePEMFormattedPrivateKey(
      final String pemFormattedPrivateKey) throws InvalidKeyException
  {
    return decodePEMFormattedPrivateKey(pemFormattedPrivateKey, null);
  }

  /**
   * Decodes a PEM formatted private key.
   *
   * @param pemFormattedPrivateKey text to be decoded as a PEM private key.
   * @param passphrase an optional passphrase for the private key
   * @return the PrivateKey decoded from the input text.
   * @throws CertificateParsingException
   *           thrown if the PEM formatted string cannot be parsed into a PrivateKey.
   */
  public static PrivateKey decodePEMFormattedPrivateKey(
      final String pemFormattedPrivateKey,
      @Nullable final String passphrase) throws InvalidKeyException
  {
    try {
      Object object = decodePEMFormatted(pemFormattedPrivateKey);
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
      if (object instanceof PrivateKeyInfo privateKeyInfo) {
        return converter.getPrivateKey(privateKeyInfo);
      }
      else if (object instanceof PEMKeyPair pair) {
        return converter.getPrivateKey(pair.getPrivateKeyInfo());
      }
      else if (object instanceof PKCS8EncryptedPrivateKeyInfo keyInfo) {
        if (Strings2.isBlank(passphrase)) {
          throw new InvalidKeyException("Provided key requires passphrase");
        }
        InputDecryptorProvider pkcs8Prov =
            new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passphrase.toCharArray());

        return converter.getPrivateKey(keyInfo.decryptPrivateKeyInfo(pkcs8Prov));

      }
      else if (object instanceof PEMEncryptedKeyPair encryptedKeyPair) {
        if (Strings2.isBlank(passphrase)) {
          throw new InvalidKeyException("Provided key requires passphrase");
        }
        PEMDecryptorProvider decryptorProvider =
            new JcePEMDecryptorProviderBuilder().build(passphrase.toCharArray());
        PEMKeyPair decryptedKeyPair = encryptedKeyPair.decryptKeyPair(decryptorProvider);
        return converter.getPrivateKey(decryptedKeyPair.getPrivateKeyInfo());
      }
      else if (object != null) {
        log.debug("Unexpected private key type {}", object.getClass().getName());
      }
    }
    catch (Exception e) {
      Throwables.throwIfInstanceOf(e, InvalidKeyException.class);
      throw new InvalidKeyException("Failed to parse valid private key from expected PEM formatted string", e);
    }

    // cert was not a valid object
    throw new InvalidKeyException("Failed to parse valid private key from expected PEM formatted string");
  }

  /**
   * @param pemFormattedPrivateKey
   * @return true if the provided string is a PEM formatted private key, false otherwise.
   */
  public static boolean isPEMFormattedPrivateKey(final String pemFormattedPrivateKey) {

    try {
      Object object = decodePEMFormatted(pemFormattedPrivateKey);

      if (object != null) {
        log.debug("Private key type: {}", object.getClass().getName());
      }

      return object instanceof PrivateKeyInfo || object instanceof PEMKeyPair
          || object instanceof PKCS8EncryptedPrivateKeyInfo
          || object instanceof PEMEncryptedKeyPair;
    }
    catch (Exception e) {
      log.debug("Failed to parse valid private key from expected PEM formatted string", e);
    }
    return false;
  }

  /**
   * Decodes a PEM formatted object.
   *
   * @param pemFormatted text to be decoded as a PEM.
   * @return the Certificate decoded from the input text.
   * @throws CertificateParsingException
   *           thrown if the PEM formatted string cannot be parsed into a Certificate.
   */
  private static Object decodePEMFormatted(final String pemFormatted) throws CertificateException {
    // make sure we have something to parse
    if (pemFormatted == null) {
      return null;
    }
    StringReader stringReader = new StringReader(pemFormatted);

    try (PEMParser pemReader = new PEMParser(stringReader)) {
      return pemReader.readObject();
    }
    catch (Exception e) {
      throw new CertificateParsingException("Failed to parse expected PEM format", e);
    }
  }

  /**
   * Calculates the SHA1 of a Certificate.
   */
  public static String calculateSha1(final Certificate certificate) throws CertificateEncodingException {
    requireNonNull(certificate);
    return Hashing.sha1().hashBytes(certificate.getEncoded()).toString().toUpperCase(Locale.US);
  }

  /**
   * Calculates the SHA1 of a Certificate and formats for readability,
   * such as {@code 64:C4:44:A9:02:F7:F0:02:16:AA:C3:43:0B:BF:ED:44:C8:81:87:CD}.
   */
  public static String calculateFingerprint(final Certificate certificate) throws CertificateEncodingException {
    String sha1Hash = calculateSha1(certificate);
    return encode(sha1Hash, ':', 2);
  }

  public static Map<String, String> getIssuerRdns(final X509Certificate certificate) throws InvalidNameException {
    return getRdns(certificate.getIssuerX500Principal().getName());
  }

  public static Map<String, String> getSubjectRdns(final X509Certificate certificate) throws InvalidNameException {
    return getRdns(certificate.getSubjectX500Principal().getName());
  }

  private static Map<String, String> getRdns(final String dn) throws InvalidNameException {
    Map<String, String> rdns = new HashMap<>();
    LdapName ldapName = new LdapName(dn);
    for (Rdn rdn : ldapName.getRdns()) {
      rdns.put(rdn.getType(), rdn.getValue().toString());
    }
    return rdns;
  }

  private static String encode(final String input, final char separator, final int delay) {
    StringBuilder buff = new StringBuilder();

    int i = 0;
    for (char c : input.toCharArray()) {
      if (i != 0 && i % delay == 0) {
        buff.append(separator);
      }
      buff.append(c);
      i++;
    }

    return buff.toString();
  }
}
