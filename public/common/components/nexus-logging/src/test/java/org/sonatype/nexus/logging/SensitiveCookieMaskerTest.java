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
package org.sonatype.nexus.logging;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class SensitiveCookieMaskerTest
{
  private final SensitiveCookieMasker underTest = new SensitiveCookieMasker();

  @Test
  void testRealWorldLine_masksJWT_andPreservesAttributes() {
    String input = "2026-08-11 12:33:31,761+0000 DEBUG ... org.apache.http.headers -"
        + " http-outgoing-4963 << Set-Cookie: KEYCLOAK_IDENTITY=eyJhbGciOiJ.abc.def;"
        + " Version=1; Path=/auth; HttpOnly";
    String result = underTest.mask(input);

    assertThat(result.contains("eyJhbGciOiJ.abc.def"), is(false));
    assertThat(result.contains("Set-Cookie: KEYCLOAK_IDENTITY=****"), is(true));
    assertThat(result.contains("; Version=1; Path=/auth; HttpOnly"), is(true));
  }

  @Test
  void testKeycloakSession_isMasked() {
    String input = "Set-Cookie: KEYCLOAK_SESSION=abc123def456; Path=/auth";
    String result = underTest.mask(input);

    assertThat(result.contains("abc123def456"), is(false));
    assertThat(result.contains("KEYCLOAK_SESSION=****"), is(true));
    assertThat(result.contains("; Path=/auth"), is(true));
  }

  @Test
  void testAuthSessionId_isMasked() {
    String input = "Set-Cookie: AUTH_SESSION_ID=xyz789; Version=1";
    String result = underTest.mask(input);

    assertThat(result.contains("xyz789"), is(false));
    assertThat(result.contains("AUTH_SESSION_ID=****"), is(true));
  }

  @Test
  void testLegacyVariants_areMasked() {
    assertThat(
        underTest.mask("Set-Cookie: KEYCLOAK_IDENTITY_LEGACY=token1").contains("KEYCLOAK_IDENTITY_LEGACY=****"),
        is(true));
    assertThat(
        underTest.mask("Set-Cookie: KEYCLOAK_SESSION_LEGACY=token2").contains("KEYCLOAK_SESSION_LEGACY=****"),
        is(true));
    assertThat(
        underTest.mask("Set-Cookie: AUTH_SESSION_ID_LEGACY=token3").contains("AUTH_SESSION_ID_LEGACY=****"),
        is(true));
  }

  @Test
  void testCaseInsensitiveHeaderMatch() {
    String input = "set-cookie: keycloak_identity=eyJhbGciOiJ.abc.def; Path=/auth";
    String result = underTest.mask(input);

    assertThat(result.contains("eyJhbGciOiJ.abc.def"), is(false));
    assertThat(result.contains("****"), is(true));
    assertThat(result.contains("; Path=/auth"), is(true));
  }

  @Test
  void testBenignCookie_notMasked() {
    String input = "Set-Cookie: JSESSIONID=abc123; Path=/";
    assertThat(underTest.mask(input), equalTo(input));
  }

  @Test
  void testExtraCookieViaConstructorCsv_isMasked() {
    SensitiveCookieMasker maskerWithExtra = new SensitiveCookieMasker("MY_TOKEN");
    String input = "Set-Cookie: MY_TOKEN=secret123; Path=/";
    String result = maskerWithExtra.mask(input);

    assertThat(result.contains("secret123"), is(false));
    assertThat(result.contains("MY_TOKEN=****"), is(true));
  }

  @Test
  void testNullMessage_returnsNull() {
    assertThat(underTest.mask(null), equalTo(null));
  }

  @Test
  void testMessageWithoutEquals_returnedUnchanged() {
    String input = "No equals sign here at all";
    assertThat(underTest.mask(input), equalTo(input));
  }

  @Test
  void testMultipleOccurrences_allMasked() {
    String input = "Set-Cookie: KEYCLOAK_IDENTITY=token1; Set-Cookie: KEYCLOAK_SESSION=token2";
    String result = underTest.mask(input);

    assertThat(result.contains("token1"), is(false));
    assertThat(result.contains("token2"), is(false));
    assertThat(result.contains("KEYCLOAK_IDENTITY=****"), is(true));
    assertThat(result.contains("KEYCLOAK_SESSION=****"), is(true));
  }

  @Test
  void testConfigurableCookieViaSystemProperty_isMasked() {
    String previous = System.getProperty("nexus.log.mask.cookies");
    System.setProperty("nexus.log.mask.cookies", "CUSTOM_TOKEN, OTHER_TOKEN");
    try {
      SensitiveCookieMasker masker = new SensitiveCookieMasker();
      String input = "Set-Cookie: CUSTOM_TOKEN=secretValue123; Path=/";
      String result = masker.mask(input);

      assertThat(result.contains("secretValue123"), is(false));
      assertThat(result.contains("CUSTOM_TOKEN=****"), is(true));
    }
    finally {
      if (previous == null) {
        System.clearProperty("nexus.log.mask.cookies");
      }
      else {
        System.setProperty("nexus.log.mask.cookies", previous);
      }
    }
  }

  @Test
  void testJwtWithBase64Padding_isFullyMasked() {
    String input = "Set-Cookie: KEYCLOAK_IDENTITY=eyJhbGciOiJ.payload.sig==; Path=/auth";
    String result = underTest.mask(input);

    assertThat(result.contains("eyJhbGciOiJ.payload.sig"), is(false));
    assertThat(result.contains("=="), is(false));
    assertThat(result.contains("KEYCLOAK_IDENTITY=****"), is(true));
    assertThat(result.contains("; Path=/auth"), is(true));
  }
}
