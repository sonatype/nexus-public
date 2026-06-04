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
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RsaUtils}.
 */
public class RsaUtilsTest
{
  // A fixed PKCS#8 PEM-encoded RSA private key for deterministic test results
  private static final String TEST_PRIVATE_KEY_PEM =
      "-----BEGIN PRIVATE KEY-----\n" +
          "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC7Nw0rnRTZbOUX\n" +
          "gQSG6OHQaKdHfn4I9Zb0A+uFo+m1AsxnIjYzjhu8yLx9gm0MWBpI1cfYiF31jW1C\n" +
          "wC3tK+6z5uobXGfesBiA6NSfJvpzReftrhLBVOz2wdhkKKwAQhu53n5r2P+7VyV3\n" +
          "yA6KLJl4/sXyw1JwQU+Mmuqsad9mJSHJD4AsrvspOozmQEtKJbghoHPIgnMQZmVl\n" +
          "SIrIGXLlGYn9J+Ahu0oLrNay5c2uoTRRDFQ2F0P0L078TUPb/Dk+y7QrQmsNIG1C\n" +
          "gkEe3oEhRmIwGZQGzfaNfKwDeMFS+p/RaULS+mvwyDj49BgBrvpvekAUREwo1sR0\n" +
          "o3omi0VlAgMBAAECggEAVyfj27//KtGFPZH0t5HHPqzZ43DB4A3lPqhshwSuEnTB\n" +
          "D+pfbVTlGNgi2BNWDP8fDXVo2idyBpTWntK1Dsr7D4maxy4XtRYp8ilygr00r2GF\n" +
          "/aKiXJrazm2j/czpdh1QpxErra2SMHb7nG3oRu9Ia4nAQaoowNXG4OBmC+ol9Hjc\n" +
          "jaKZYicOZUd0pOvzZFjG8/fxMZYllIwI4sUrmPJ2VhfEjtP7Rg/Pk2BRI/wDb0ET\n" +
          "Xv7GT2LguVW484juprSodEykGLzIO54GEHRWoRmX3IHop85KxZ45jmniAU3Gjk9p\n" +
          "nPuA7n+RAdzJ/3gxZcMrdt0tPXyO249oq/spUc4TaQKBgQDrC6+z1RX2UZctU98D\n" +
          "Gisx2VqyLOXtMtJLKYY9JEozloZmrMEDkdcyBHZylwoIxdLlIiBv2kahraWd9bbg\n" +
          "v50jzoSPR/jPnh4L5L3e4qkOZCppmgj82K3qNfL3eOiwKvtgVR23teKeQOd/Xa9d\n" +
          "vP5vJJZXa+Yfl9Q7SywhOeuyVwKBgQDL58Ea2yBjxZUo/t3dRU5rqSkiX45u/JgA\n" +
          "IbJxGRPOJTl8gIsGTAjmAnsj2bJS9AL2y47C3JXZCApNN181Wd08h/sWaSUu9JZC\n" +
          "s+LEBmfijV4/bDzWlLwn0xIEU6fqHzzJNP+XcqyB6Dby7fcsbrSYTD6CuNUEVymI\n" +
          "pZl4Q4oIowKBgBIsLiJ+SBtqRYD4qhZoUIyjHHMIK6LCOiYbiMhzZNVGPw/zLV0k\n" +
          "SnoQhEPpz5nMCbkzgUSEoM9hSJvE4qXPyst47SDS1Lbgp7wNrGxuI9n7/pd5lFZQ\n" +
          "PmoMT8O4cm0kdZkGG60Xf/TyWbOsP6HEuftH3ePWcM1ihMMs1bWf78wzAoGBAJde\n" +
          "+NADFEx9BXplmjcFmG38KnlGDur9wal3WozzXOyQXdi7ZHnMQF7gQKIgnm1OkFS+\n" +
          "UMEAGI4BlgQ0sw1cJQ0mtZOxgtUU5eemuxVi3AQnhmv24kM6L2QxIRLtN55qiimk\n" +
          "monHq6DUztYRKolltdPJ5i4NILYULtUuenv5R6OZAoGBAK3DhBPOVMaii2NhJNIk\n" +
          "rnZ18n3A9kDtE7NbHtGVU3dG+iNjTZpbnMDkSixxTfDSgo1qI8Dgi/xdQjm++4eu\n" +
          "PbgqolpalgC5kGeUL9yGQaI/ObD30wiuDvrdiy7ivu/EKYWqIhTU4c2LEqowGsOz\n" +
          "2U12pnAIhx3c8Ssh/kMbKH+E\n" +
          "-----END PRIVATE KEY-----";

  // The expected key name derived from the above key (last 8 hex chars of SHA-256)
  private static final String EXPECTED_KEY_NAME = "key-35e319cd";

  // -------------------------------------------------------------------------
  // generateKeyName()
  // -------------------------------------------------------------------------

  @Test
  public void generateKeyName_returnsExpectedFormat() {
    String keyName = RsaUtils.generateKeyName(TEST_PRIVATE_KEY_PEM);

    assertThat(keyName).isEqualTo(EXPECTED_KEY_NAME);
  }

  @Test
  public void generateKeyName_isStableAcrossMultipleCalls() {
    String first = RsaUtils.generateKeyName(TEST_PRIVATE_KEY_PEM);
    String second = RsaUtils.generateKeyName(TEST_PRIVATE_KEY_PEM);

    assertThat(first).isEqualTo(second);
  }

  @Test
  public void generateKeyName_ignoresTextBeforeAndAfterPemBlock() {
    String pemWithSurroundingText = "Some UI validation prefix\n" + TEST_PRIVATE_KEY_PEM + "\nsome trailing text";

    assertThat(RsaUtils.generateKeyName(pemWithSurroundingText)).isEqualTo(EXPECTED_KEY_NAME);
  }

  @Test
  public void generateKeyName_ignoresWhitespaceDifferencesInsidePemBlock() {
    String pemWithExtraWhitespace = "\n\n" + TEST_PRIVATE_KEY_PEM + "\n\n";

    assertThat(RsaUtils.generateKeyName(pemWithExtraWhitespace)).isEqualTo(EXPECTED_KEY_NAME);
  }

  @Test
  public void generateKeyName_throwsIllegalArgumentExceptionWhenNoPemBlockPresent() {
    assertThatThrownBy(() -> RsaUtils.generateKeyName("this is not a PEM string"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void generateKeyName_throwsForNullInput() {
    assertThatThrownBy(() -> RsaUtils.generateKeyName(null))
        .isInstanceOf(RuntimeException.class);
  }

  // -------------------------------------------------------------------------
  // sign()
  // -------------------------------------------------------------------------

  @Test
  public void sign_producesNonEmptyByteArray() throws IOException {
    byte[] content = "APKINDEX content".getBytes(StandardCharsets.UTF_8);

    byte[] signature = RsaUtils.sign(content, TEST_PRIVATE_KEY_PEM, null);

    assertThat(signature).isNotEmpty();
  }

  @Test
  public void sign_isConsistentForSameContentAndKey() throws IOException {
    byte[] content = "stable content".getBytes(StandardCharsets.UTF_8);

    byte[] sig1 = RsaUtils.sign(content, TEST_PRIVATE_KEY_PEM, null);
    byte[] sig2 = RsaUtils.sign(content, TEST_PRIVATE_KEY_PEM, null);

    assertThat(sig1).isEqualTo(sig2);
  }

  @Test
  public void sign_producesDifferentSignaturesForDifferentContent() throws IOException {
    byte[] content1 = "content one".getBytes(StandardCharsets.UTF_8);
    byte[] content2 = "content two".getBytes(StandardCharsets.UTF_8);

    byte[] sig1 = RsaUtils.sign(content1, TEST_PRIVATE_KEY_PEM, null);
    byte[] sig2 = RsaUtils.sign(content2, TEST_PRIVATE_KEY_PEM, null);

    assertThat(sig1).isNotEqualTo(sig2);
  }

  @Test
  public void sign_worksWithEmptyContent() throws IOException {
    byte[] empty = new byte[0];

    byte[] signature = RsaUtils.sign(empty, TEST_PRIVATE_KEY_PEM, null);

    assertThat(signature).isNotEmpty();
  }

  @Test
  public void sign_throwsIoExceptionWhenPemIsInvalid() {
    byte[] content = "content".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> RsaUtils.sign(content, "not a pem", null))
        .isInstanceOf(IOException.class);
  }

  @Test
  public void sign_worksWithNullPassphrase() throws IOException {
    byte[] content = "content".getBytes(StandardCharsets.UTF_8);

    byte[] signature = RsaUtils.sign(content, TEST_PRIVATE_KEY_PEM, null);

    assertThat(signature).isNotEmpty();
  }

  @Test
  public void sign_worksWithEmptyPassphrase() throws IOException {
    byte[] content = "content".getBytes(StandardCharsets.UTF_8);

    byte[] signature = RsaUtils.sign(content, TEST_PRIVATE_KEY_PEM, "");

    assertThat(signature).isNotEmpty();
  }

  // -------------------------------------------------------------------------
  // getPublicKey()
  // -------------------------------------------------------------------------

  @Test
  public void getPublicKey_returnsPemEncodedPublicKey() throws IOException {
    byte[] publicKeyBytes = RsaUtils.getPublicKey(TEST_PRIVATE_KEY_PEM, null);
    String publicKeyPem = new String(publicKeyBytes, StandardCharsets.UTF_8);

    assertThat(publicKeyPem).contains("-----BEGIN PUBLIC KEY-----");
    assertThat(publicKeyPem).contains("-----END PUBLIC KEY-----");
  }

  @Test
  public void getPublicKey_returnsNonEmptyBytes() throws IOException {
    byte[] publicKeyBytes = RsaUtils.getPublicKey(TEST_PRIVATE_KEY_PEM, null);

    assertThat(publicKeyBytes).isNotEmpty();
  }

  @Test
  public void getPublicKey_throwsIoExceptionWhenPemIsInvalid() {
    assertThatThrownBy(() -> RsaUtils.getPublicKey("not a pem", null))
        .isInstanceOf(IOException.class);
  }
}
