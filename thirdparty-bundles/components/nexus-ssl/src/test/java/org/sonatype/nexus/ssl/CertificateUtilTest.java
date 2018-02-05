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

import java.security.Security;
import java.security.cert.Certificate;

import org.sonatype.goodies.testsupport.TestSupport;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link CertificateUtil}.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class CertificateUtilTest
    extends TestSupport
{
  private static final String NL = System.lineSeparator();

  // Need to use platform NL here for compatibility
  private final String CERT_IN_PEM =
      "-----BEGIN CERTIFICATE-----" + NL
          + "MIIByzCCAXUCBgE0OsUqMjANBgkqhkiG9w0BAQUFADBtMRYwFAYDVQQDEw10byBi" + NL
          + "ZSBjaGFuZ2VkMQ8wDQYDVQQLEwZjaGFuZ2UxDzANBgNVBAoTBmNoYW5nZTEPMA0G" + NL
          + "A1UEBxMGY2hhbmdlMQ8wDQYDVQQIEwZjaGFuZ2UxDzANBgNVBAYTBmNoYW5nZTAg" + NL
          + "Fw0xMTEyMTQwNDEyMDdaGA8yMTExMTEyMDA0MTIwN1owbTEWMBQGA1UEAxMNdG8g" + NL
          + "YmUgY2hhbmdlZDEPMA0GA1UECxMGY2hhbmdlMQ8wDQYDVQQKEwZjaGFuZ2UxDzAN" + NL
          + "BgNVBAcTBmNoYW5nZTEPMA0GA1UECBMGY2hhbmdlMQ8wDQYDVQQGEwZjaGFuZ2Uw" + NL
          + "XDANBgkqhkiG9w0BAQEFAANLADBIAkEAtyZDEbRZ9snDlCQbKerKAGGMHXIWF1t2" + NL
          + "6SBEAuC6krlujo5vMQsE/0Qp0jePjf9IKj8dR5RcXDKNi4mITY/Y4wIDAQABMA0G" + NL
          + "CSqGSIb3DQEBBQUAA0EAjX5DHXWkFxVWuvymp/2VUkcs8/PV1URpjpnVRL22GbXU" + NL
          + "UTlNxF8vcC+LMpLCaAk3OLezSwYkpptRFK/x3EWq7g==" + NL
          + "-----END CERTIFICATE-----";

  private static final String SHA_CERT_FINGERPRINT = "64:C4:44:A9:02:F7:F0:02:16:AA:C3:43:0B:BF:ED:44:C8:81:87:CD";

  @Before
  public void registerBouncyCastle() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  /**
   * Tests a Certificate can be decoded then serialized and end up with the same result.
   */
  @Test
  public void testMarshalPEMFormat() throws Exception {
    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM);
    assertThat(CertificateUtil.serializeCertificateInPEM(certificate).trim(), equalTo(CERT_IN_PEM));
  }

  /**
   * Tests calculating a fingerprint for a Certificate.
   */
  @Test
  public void testCalculateFingerPrint() throws Exception {
    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM);
    String actualFingerPrint = CertificateUtil.calculateFingerprint(certificate);
    assertThat(actualFingerPrint, equalTo(SHA_CERT_FINGERPRINT));
  }
}
