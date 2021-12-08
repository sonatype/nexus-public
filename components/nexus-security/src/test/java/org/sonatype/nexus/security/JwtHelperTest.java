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
package org.sonatype.nexus.security;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.Cookie;

import org.sonatype.goodies.testsupport.TestSupport;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.JwtHelper.ISSUER;
import static org.sonatype.nexus.security.JwtHelper.REALM;
import static org.sonatype.nexus.security.JwtHelper.USER;
import static org.sonatype.nexus.security.JwtHelper.USER_SESSION_ID;

public class JwtHelperTest
    extends TestSupport
{
  @Mock
  private Subject subject;

  @Mock
  private PrincipalCollection principals;

  private JwtHelper underTest;

  @Before
  public void setup() {
    underTest = new JwtHelper(300,"/","secret");
    when(subject.getPrincipal()).thenReturn("admin");
    when(subject.getPrincipals()).thenReturn(principals);
    when(principals.getRealmNames()).thenReturn(Collections.singleton("NexusAuthorizingRealm"));
  }

  @Test
  public void testCreateJwtCookie() {
    Cookie jwtCookie = underTest.createJwtCookie(subject);
    assertNotNull(jwtCookie);
    String jwt = jwtCookie.getValue();

    assertJwt(jwt);
    assertCookie(jwtCookie);
  }

  @Test
  public void testVerifyJwt_success() {
    String jwt = makeValidJwt();
    Optional<DecodedJWT> decodedJWT = underTest.verifyJwt(jwt);
    assertTrue(decodedJWT.isPresent());

    DecodedJWT decoded = decodedJWT.get();
    assertEquals(ISSUER, decoded.getClaim("iss").asString());
  }

  @Test
  public void testVerifyJwt_tokenExpired() {
    String jwt = makeInvalidJwt();
    Optional<DecodedJWT> decodedJWT = underTest.verifyJwt(jwt);
    assertFalse(decodedJWT.isPresent());
  }

  @Test
  public void testVerifyAndRefresh_success() {
    String jwt = makeValidJwt();
    DecodedJWT decodedJWT = decodeJwt(jwt);

    Optional<Cookie> refreshed = underTest.verifyAndRefreshJwtCookie(jwt);
    assertTrue(refreshed.isPresent());

    Cookie refreshedCookie = refreshed.get();
    assertCookie(refreshedCookie);

    DecodedJWT refreshedJwt = decodeJwt(refreshedCookie.getValue());

    Claim userSessionId = decodedJWT.getClaim(USER_SESSION_ID);
    assertEquals(userSessionId.asString(), refreshedJwt.getClaim(USER_SESSION_ID).asString());
    assertJwt(refreshedCookie.getValue());
  }

  @Test
  public void testVerifyAndRefresh_invalidJwt() {
    String jwt = makeInvalidJwt();
    Optional<Cookie> refreshed = underTest.verifyAndRefreshJwtCookie(jwt);
    assertFalse(refreshed.isPresent());
  }

  private String makeValidJwt() {
    Date expiresAt = new Date(new Date().getTime() + 100000);
    String userSessionId = UUID.randomUUID().toString();
    return JWT.create()
        .withIssuer(ISSUER)
        .withExpiresAt(expiresAt)
        .withClaim(USER_SESSION_ID, userSessionId)
        .withClaim(USER, "admin")
        .withClaim(REALM, "NexusAuthorizingRealm")
        .sign(Algorithm.HMAC256("secret"));
  }

  private String makeInvalidJwt() {
    Date expiresAt = new Date(new Date().getTime() - 100000);
    return JWT.create()
        .withIssuer(ISSUER)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("secret"));
  }

  private void assertCookie(final Cookie jwtCookie) {
    assertEquals(JwtHelper.JWT_COOKIE_NAME, jwtCookie.getName());
    assertNotNull(jwtCookie.getValue());
    assertEquals(300, jwtCookie.getMaxAge());
    assertEquals("/", jwtCookie.getPath());
    assertTrue(jwtCookie.isHttpOnly());
  }

  private void assertJwt(final String jwt) {
    DecodedJWT decode = decodeJwt(jwt);

    Claim user = decode.getClaim(USER);
    Claim userId = decode.getClaim(USER_SESSION_ID);
    Claim issuer = decode.getClaim("iss");
    Claim realm = decode.getClaim(REALM);

    assertEquals("admin", user.asString());
    assertNotNull(userId.asString());
    assertEquals(ISSUER, issuer.asString());
    assertEquals("NexusAuthorizingRealm", realm.asString());
  }

  private DecodedJWT decodeJwt(final String jwt) {
    try {
      return JWT.decode(jwt);
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Invalid token");
    }
  }
}
