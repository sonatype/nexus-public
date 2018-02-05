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
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

import com.google.common.hash.Hashing;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple utility methods when dealing with certificates.
 * <p/>
 * In order to use the methods decodePEMFormattedCertificate or serializeCertificateInPEM, the BouncyCastleProvider
 * must be registered. <BR/><BR/>
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

  public static X509Certificate generateCertificate(final PublicKey publicKey,
                                                    final PrivateKey privateKey,
                                                    final String algorithm,
                                                    final int validDays,
                                                    final String commonName,
                                                    final String orgUnit,
                                                    final String organization,
                                                    final String locality,
                                                    final String state,
                                                    final String country)
      throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, CertificateEncodingException
  {
    X509V3CertificateGenerator certificateGenerator = new X509V3CertificateGenerator();
    Vector<ASN1ObjectIdentifier> order = new Vector<>();
    Hashtable<ASN1ObjectIdentifier, String> attributeMap = new Hashtable<>();

    if (commonName != null) {
      attributeMap.put(X509Name.CN, commonName);
      order.add(X509Name.CN);
    }

    if (orgUnit != null) {
      attributeMap.put(X509Name.OU, orgUnit);
      order.add(X509Name.OU);
    }

    if (organization != null) {
      attributeMap.put(X509Name.O, organization);
      order.add(X509Name.O);
    }

    if (locality != null) {
      attributeMap.put(X509Name.L, locality);
      order.add(X509Name.L);
    }

    if (state != null) {
      attributeMap.put(X509Name.ST, state);
      order.add(X509Name.ST);
    }

    if (country != null) {
      attributeMap.put(X509Name.C, country);
      order.add(X509Name.C);
    }

    X509Name issuerDN = new X509Name(order, attributeMap);

    // validity
    long now = System.currentTimeMillis();
    long expire = now + (long) validDays * 24 * 60 * 60 * 1000;

    certificateGenerator.setNotBefore(new Date(now));
    certificateGenerator.setNotAfter(new Date(expire));
    certificateGenerator.setIssuerDN(issuerDN);
    certificateGenerator.setSubjectDN(issuerDN);
    certificateGenerator.setPublicKey(publicKey);
    certificateGenerator.setSignatureAlgorithm(algorithm);
    certificateGenerator.setSerialNumber(BigInteger.valueOf(now));

    // make certificate
    return certificateGenerator.generate(privateKey);
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
   *          thrown if the PEM formatted string cannot be parsed into a Certificate.
   */
  public static Certificate decodePEMFormattedCertificate(final String pemFormattedCertificate)
      throws CertificateException
  {
    log.trace("Parsing PEM formatted certificate string:\n{}", pemFormattedCertificate);

    // make sure we have something to parse
    if (pemFormattedCertificate != null) {
      StringReader stringReader = new StringReader(pemFormattedCertificate);
      PEMParser pemReader = new PEMParser(stringReader);
      try {
        Object object = pemReader.readObject();
        log.trace("Object found while paring PEM formatted string: {}", object);

        if (object instanceof X509CertificateHolder) {
          X509CertificateHolder holder = (X509CertificateHolder)object;
          JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
          return converter.getCertificate(holder);
        }
      }
      catch (IOException e) {
        throw new CertificateParsingException(
            "Failed to parse valid certificate from expected PEM formatted certificate:\n"
                + pemFormattedCertificate, e);
      }
    }

    // cert was not a valid object
    throw new CertificateParsingException(
        "Failed to parse valid certificate from expected PEM formatted certificate:\n" + pemFormattedCertificate);
  }

  /**
   * Calculates the SHA1 of a Certificate.
   */
  public static String calculateSha1(final Certificate certificate) throws CertificateEncodingException {
    checkNotNull(certificate);
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
