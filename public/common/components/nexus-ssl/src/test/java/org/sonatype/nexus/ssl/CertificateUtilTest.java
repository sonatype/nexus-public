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

import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CertificateUtil}.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class CertificateUtilTest
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

  private final String privateKeyPem = """
      -----BEGIN PRIVATE KEY-----
      MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDj91PKcraeUCHL
      kEHaVfHCiKqLwWDqaAWJ93qZkPd+KXNYUTQhPd4UHSWT0Mps1h9dVXJvm5lHYzwI
      wjJgs3ynbG8XyteYY2K5dSF8hqeydX4B+PxdTAvFuILN2dcoM2CqEQAHEpGwELuS
      5TYxRZ3+3Q/YgURrXeFLEBlRKVMi5hc+VNVa/7lZstWrujVl18BtGiAtJjN4+xUy
      ke6PCJmM9eLgIlVRlJM9Vbctqhxs5zXBSUH5NlDGFlsddrs79nNe7ca48pAAEDoC
      7VZ5a8k6VLorQG+pM3YFep3BdCeDP5egak81BRbqWbxf58NPC/w1ekr6wcCqQFez
      9dnMjUS9AgMBAAECggEAHnzTlTHxx+ruFiAhGn7fghWOf3nZiQfn47t4B0UqwFHx
      VeioSODSDE0YmzPzeopJcA1PLGt6HKDCJOa535Jpk6eOFPfBDSWPjL+EPrfwgg6y
      Dzf6Ik0bDvKd9cNSQdNbNGtpHkkGuG4Ic/0ZGg5WMsFkTt9848IOsSACTscw0ewE
      o+nAE3fZPMSce0pBpl6MZSfczTWK4tUsSthckkbdBKe34+Xa+FahBxXDErgZ10Z5
      cmWVhPJ5f4vXOWa7uznjU2bQ5OYCr516hTj6ZAtmNd//kmQ7TBDGioY8rvrCP+6q
      XRtifyF06lL0fuZ7y/BwDCxPnlYrNigvwIVscuJtAQKBgQD1zd4VaXtOoLrhtYBn
      /62sspMR2aksLUD4rB5uInnuk6f6aA5Vep3l0fGm3BHFxwrkOGxA8vd/w5hEh7aI
      CnOv9Wds3D0CRhchI3gA4FJq8zLHc/+QewRaU49L4cUEYCddUzqvbwlXDKV1TbBr
      8zc8KkgIo+CSyabshxpEGS33vQKBgQDtbArEuiYIZ8KpxPHyAeyhaQPvhFm4fT1v
      DxTOBKMxQbQtoUaoM/koHZvrU9Tp9ip1L8c9QAxJ0gE7KwicHADDxNMX7SQ0jvAL
      ygbutmh2kENbMD0A6CfrgCBPvhjCzcfj2k3YDnqRoQdQTDsh7NOtWtBcPhLeJRSI
      Wuu5F3bRAQKBgQDjCEb8F/VgZmy2VOJJLIrJXBcjAZuTxiighrmEmCtvBMq+KqYy
      pZJ51zQQHmv79SBGMycpxkB7rblSctdRR/Yaq9vfXHytQPfUD+KjTI5TYlWgi6+d
      T0Jx+4P7XoiOxhEVze4CY9iIopGgiyVMT9TQqWznLhYa/k5DYXYNOASRMQKBgQDb
      Ub7w0/RmN1N/eEr4AAcTY+sayuwGnMqvWQMi/O2zhrpmnGCk6L307MUnsh0wsxGt
      hyFJ12yClNhGTA58BvI95ggKRin300Kz4UBeK2p8e+98wSbZO5wE1CsE/tnouic4
      uGTIfTeEZ3hWGNVXFWrE7OslCsJrK124SCUQqjg8AQKBgAPRBV2pZxePixVV+tT3
      jaWyYGO8M5ScBwU1kvnHSa+ygj7LxURmUGolSoNewnXd1VRZ+I0Ev7SVlVc0eKMh
      fDdrhLkH3a3TMiO1blkaPNToog776KOPBepWZJN4ognWVo7EDBGXg+RKwIsNn5dF
      J3JXpWiBtpZPxCtBq4oyyk0R
      -----END PRIVATE KEY-----
      """;

  private final String privateKeyEncrypted = """
      -----BEGIN ENCRYPTED PRIVATE KEY-----
      MIIFNTBfBgkqhkiG9w0BBQ0wUjAxBgkqhkiG9w0BBQwwJAQQUgfF8vyFjLM+aCut
      e3WRNgICCAAwDAYIKoZIhvcNAgkFADAdBglghkgBZQMEAQIEEHhyUuhplmdXhwjE
      30B+YVgEggTQWRSbHNrl6ISCa8LG8yM/Sic4j1S7Cv2yGiKNPU5Kv+/vhyh+eG/S
      0jIOzH1ONOMfwuJQ5Cc9X77nNoPWHhfY/o2B0FahjY371HyJ0gw26nGsuiscg/3E
      d7XDX2zJe+qa6LuaVAlQym0MthZ2uLzeFRBPiFPjckf6LLoSdZaMVXNiJSwVsMmt
      vSzrpOuLsQYasLNu6nCzE8sULBFS/jV4TtdqEeZoQArbMuoiCUKGoK3kL8XpcTvy
      fc9w3I7fw/H2D3TRYkUG4qRuG3xd84jspsI/AOhAsrpMbKHRZtEws7x136Fhnr9r
      +k+YokNYxUHPky6FzdoSTqAnOJCFwirnnGpJVEO2JWHoQtJxa1c3JjNpZvu6m0cF
      3A+YP8v7tTnGidJDdafUs1ymaX1kdngUjDXsBcBY1OPtd8LUL7G55R0oMGzsvUTo
      PF/Nv1GMsIRegVLDyXXrxZ3TB2KqfLQSFRr9roP4Uo8f+8wns4EU/D5EbKFgxYWj
      To4xQs9K9uYeCsu/BN1++/cUQL/TM18oC4IDR8zqi4+w17sQZu+kB2+oTRCttIVN
      bUw4V/TeQpgz87noahmBVSqwlP2XCotAntSKSB2htM98j/SUAMhI/9+ztY7nc7SQ
      zq9A3qISTQaKs2qxG9K7yN65DoM6sq1/Waj7wECaSwzQrMHyquL5ZTshQja5N19z
      MCaVmiiyxvUWemirjTER/1V3aLw1ROGYfWApG3FpAA1106ChHv/Ceumeo2GQ6fNS
      lx6+qjrGQ4Zn8DJNrWWeWDhXBWKr6LUHhf2TuDQ6Ibak47rc5foZdkJFy53Kwb7m
      u2iAg20CWIH95e+a9dc7jFCUWjZLzxGyI9D1dBW+fsggRYaC7e00u2xKlFGpqsnF
      vMclFRk+FVRUt2TSOEoVjpdJ9TKBejFiLncMqqrxvwvo0O0vLDtKnLhSRl84JeOi
      7wRuSnhGJljTytbWBOe6wYDoC4akSuC6iMlzgBreZ5rt2aFjy890gl2tpRmEo3Md
      tKB7qTPo2ojF0TadmunkoYJExFjCGtJ8Tilr83fiPP13CP/IhUdwN6NN5dBI+agC
      Jfcv2dIyRQWuJcYeWXW76LJY9A51EXRXPnBZ+4XMkFOQW8spGXlkmzYRQUhkKSzB
      OtH6HPYwO+snQiwzZTflSIWxm7saPuTzN0pscdjuETzThdGkaAmUBThhnxLvNUoj
      N6yODdOQY8LAzHtVyl8MPtAueXRfIDYnNjAtBzXle+y99Vtz0p47rkyLoCg3cq+y
      mMcGDGwl5w8iFZWm2xM0xogWQoVgiY67ORSHu0vAEH3SkfiWKimoUn0OSyDiAtbT
      XUP8XM8T6mAhMB5b425eybJpcGFBUehBlWAeUh3hs2NpKDOyhvZFG7f6HoqDP/0f
      7TZHUKLhsfoeZoKWGkNq2VLWN5qp8C57JnQs8odCTqu23Xz6SoJQVWMJGIQJaWrC
      GtnJeXBQBKdN7SQcT4fC7NL1FR4cPTIBCIfps66jBL2jWVBS1iyKlVofuVdMzXBI
      gXfb4qUX3sxqmDJIfHbQMzmyvlwHFXqS1kR13iETHOqo3T8KUCLMr9qRHQGvGcAF
      ovN6mk6cZquzUWH/s8fqSe2B2yxkzx0RYVrttrh2eGPGjUdkXp3V1VY=
      -----END ENCRYPTED PRIVATE KEY-----
      """;

  private static final String SHA_CERT_FINGERPRINT = "64:C4:44:A9:02:F7:F0:02:16:AA:C3:43:0B:BF:ED:44:C8:81:87:CD";

  private static KeyPair TEST_KEY_PAIR;

  @BeforeAll
  public static void generateTestKeyPair() throws Exception {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    TEST_KEY_PAIR = gen.generateKeyPair();
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

  @Test
  public void decodePEMFormattedPrivateKey_pkcs8_returnsPrivateKey() throws Exception {
    String pem = toPkcs8Pem(TEST_KEY_PAIR.getPrivate());

    PrivateKey key = CertificateUtil.decodePEMFormattedPrivateKey(pem);

    assertThat(key, notNullValue());
    assertThat(key.getAlgorithm(), equalTo("RSA"));
  }

  @Test
  public void decodePEMFormattedPrivateKey_pkcs1_returnsPrivateKey() throws Exception {
    String pem = toPkcs1Pem(TEST_KEY_PAIR);

    PrivateKey key = CertificateUtil.decodePEMFormattedPrivateKey(pem);

    assertThat(key, notNullValue());
    assertThat(key.getAlgorithm(), equalTo("RSA"));
  }

  @Test
  void testDecodePEMFormattedPrivateKey() throws Exception {
    PrivateKey privateKey = CertificateUtil.decodePEMFormattedPrivateKey(privateKeyPem, null);
    assertNotNull(privateKey);

    privateKey = CertificateUtil.decodePEMFormattedPrivateKey(privateKeyEncrypted, "foobar");
    assertNotNull(privateKey);

    // Test valid PEM but not expected type (e.g. certificate)
    assertThrows(InvalidKeyException.class, () -> CertificateUtil.decodePEMFormattedPrivateKey(CERT_IN_PEM, null));

    // Test not in PEM format nonsense
    assertThrows(InvalidKeyException.class, () -> CertificateUtil.decodePEMFormattedPrivateKey("asdfasdfasdf", null));

    // Test null
    assertThrows(InvalidKeyException.class, () -> CertificateUtil.decodePEMFormattedPrivateKey(null, null));

    // Test wrong password
    assertThrows(InvalidKeyException.class,
        () -> CertificateUtil.decodePEMFormattedPrivateKey(privateKeyEncrypted, "invalid"));
  }

  @Test
  void testIsPEMFormattedPrivateKey() throws Exception {
    assertTrue(CertificateUtil.isPEMFormattedPrivateKey(privateKeyPem));
    assertTrue(CertificateUtil.isPEMFormattedPrivateKey(privateKeyEncrypted));
    assertFalse(CertificateUtil.isPEMFormattedPrivateKey(CERT_IN_PEM));
  }

  @Test
  void testEncodePEM() throws InvalidKeyException {
    PrivateKey original = CertificateUtil.decodePEMFormattedPrivateKey(privateKeyEncrypted, "foobar");

    String pemPasswordlessString = CertificateUtil.removePemPassphrase(privateKeyEncrypted, "foobar");

    // Decode it again to verify this cycle did not change the key
    PrivateKey second = CertificateUtil.decodePEMFormattedPrivateKey(pemPasswordlessString, null);

    assertThat(original, is(second));
  }

  private static String toPkcs8Pem(final PrivateKey privateKey) throws Exception {
    StringWriter sw = new StringWriter();
    try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
      writer.writeObject(privateKey);
    }
    return sw.toString();
  }

  private static String toPkcs1Pem(final KeyPair keyPair) throws Exception {
    StringWriter sw = new StringWriter();
    try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
      writer.writeObject(keyPair);
    }
    return sw.toString();
  }
}
