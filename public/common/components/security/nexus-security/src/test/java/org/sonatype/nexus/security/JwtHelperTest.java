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
import jakarta.servlet.http.Cookie;

import org.sonatype.nexus.security.jwt.JwtVerificationException;
import org.sonatype.nexus.security.jwt.SecretStore;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Provider;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.JwtHelper.ID_TOKEN;
import static org.sonatype.nexus.security.JwtHelper.ISSUER;
import static org.sonatype.nexus.security.JwtHelper.REALM;
import static org.sonatype.nexus.security.JwtHelper.USER;
import static org.sonatype.nexus.security.JwtHelper.USER_SESSION_ID;

@RunWith(MockitoJUnitRunner.Silent.class)
public class JwtHelperTest
{
  private static final String VALID_JWT_TOKEN =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";

  @Mock
  private Subject subject;

  @Mock
  private PrincipalCollection principals;

  @Mock
  private SecretStore secretStore;

  @Mock
  private Provider<SecretStore> storeProvider;

  private JwtHelper underTest;

  @Before
  public void setup() throws Exception {
    when(secretStore.getSecret()).thenReturn(Optional.of("secret"));
    when(storeProvider.get()).thenReturn(secretStore);
    underTest = new JwtHelper(300, "/", storeProvider);
    underTest.doStart();
    when(subject.getPrincipal()).thenReturn("admin");
    when(subject.getPrincipals()).thenReturn(principals);
    when(principals.getRealmNames()).thenReturn(Collections.singleton("NexusAuthorizingRealm"));
  }

  @Test
  public void testCreateJwtCookie() {
    Session session = mock(Session.class);
    doReturn(session).when(subject).getSession();
    Cookie jwtCookie = underTest.createJwtCookie(subject, false);
    assertNotNull(jwtCookie);
    String jwt = jwtCookie.getValue();

    assertJwt(jwt);
    assertCookie(jwtCookie);
  }

  @Test
  public void testCreateJwtCookie_withIdToken() {
    Session session = mock(Session.class);
    doReturn(session).when(subject).getSession();
    doReturn(VALID_JWT_TOKEN).when(session).getAttribute(ID_TOKEN);
    Cookie jwtCookie = underTest.createJwtCookie(subject, false);
    assertNotNull(jwtCookie);
    String jwt = jwtCookie.getValue();

    assertJwt(jwt);
    assertCookie(jwtCookie);
  }

  @Test
  public void testCreateJwtCookie_withSecure() {
    Session session = mock(Session.class);
    doReturn(session).when(subject).getSession();
    Cookie jwtCookie = underTest.createJwtCookie(subject, true);
    assertNotNull(jwtCookie);
    String jwt = jwtCookie.getValue();

    assertJwt(jwt);
    assertCookie(jwtCookie, true);
  }

  @Test
  public void testVerifyJwt_success() throws Exception {
    String jwt = makeValidJwt();
    DecodedJWT decodedJWT = underTest.verifyJwt(jwt);

    assertEquals(ISSUER, decodedJWT.getClaim("iss").asString());
  }

  @Test(expected = JwtVerificationException.class)
  public void testVerifyJwt_tokenExpired() throws Exception {
    String jwt = makeInvalidJwt();
    underTest.verifyJwt(jwt);
  }

  @Test
  public void testRefreshJwtCookie_success() throws Exception {
    String jwt = makeValidJwt();
    DecodedJWT decodedJWT = decodeJwt(jwt);

    Cookie refreshed = underTest.refreshJwtCookie(underTest.verifyJwt(jwt), false);
    assertCookie(refreshed);

    DecodedJWT refreshedJwt = decodeJwt(refreshed.getValue());

    Claim userSessionId = decodedJWT.getClaim(USER_SESSION_ID);
    assertEquals(userSessionId.asString(), refreshedJwt.getClaim(USER_SESSION_ID).asString());
    assertJwt(refreshed.getValue());
  }

  @Test
  public void testRefreshJwtCookie_secureRequest_success() throws Exception {
    String jwt = makeValidJwt();
    DecodedJWT decodedJWT = decodeJwt(jwt);

    Cookie refreshed = underTest.refreshJwtCookie(underTest.verifyJwt(jwt), true);
    assertCookie(refreshed, true);

    DecodedJWT refreshedJwt = decodeJwt(refreshed.getValue());

    Claim userSessionId = decodedJWT.getClaim(USER_SESSION_ID);
    assertEquals(userSessionId.asString(), refreshedJwt.getClaim(USER_SESSION_ID).asString());
    assertJwt(refreshed.getValue());
  }

  /**
   * Verify behavior when nexus.jwt.cookieSecure is set to false and the request occurs in an HTTPS environment.
   * This combination should result in the JWT returning false for {@link Cookie#getSecure()}, as the feature flag
   * takes precedence.
   *
   * @throws Exception
   */
  @Test
  public void testRefreshJwtCookie_secureRequest_cookieSecure_false() throws Exception {
    String jwt = makeValidJwt();
    DecodedJWT decodedJWT = decodeJwt(jwt);

    JwtHelper cookieSecureFalse = new JwtHelper(300, "/", storeProvider, false);
    cookieSecureFalse.doStart();

    Cookie refreshed = cookieSecureFalse.refreshJwtCookie(cookieSecureFalse.verifyJwt(jwt), true);
    assertCookie(refreshed, false);

    DecodedJWT refreshedJwt = decodeJwt(refreshed.getValue());

    Claim userSessionId = decodedJWT.getClaim(USER_SESSION_ID);
    assertEquals(userSessionId.asString(), refreshedJwt.getClaim(USER_SESSION_ID).asString());
    assertJwt(refreshed.getValue());
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
    assertCookie(jwtCookie, false);
  }

  private void assertCookie(final Cookie jwtCookie, final boolean secure) {
    assertEquals(JwtHelper.JWT_COOKIE_NAME, jwtCookie.getName());
    assertNotNull(jwtCookie.getValue());
    assertEquals(300, jwtCookie.getMaxAge());
    assertEquals("/", jwtCookie.getPath());
    assertTrue(jwtCookie.isHttpOnly());
    assertEquals(secure, jwtCookie.getSecure());
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

  @Test
  public void testSetExpirySeconds_updatesExpiry() {
    assertEquals(300, underTest.getExpirySeconds());

    underTest.setExpirySeconds(600);

    assertEquals(600, underTest.getExpirySeconds());
  }

  @Test
  public void testSetExpirySeconds_createsTokenWithNewExpiry() {
    underTest.setExpirySeconds(600);

    Session session = mock(Session.class);
    doReturn(session).when(subject).getSession();
    Cookie jwtCookie = underTest.createJwtCookie(subject, false);

    assertEquals(600, jwtCookie.getMaxAge());

    // Verify JWT token also has correct expiry claim
    DecodedJWT decoded = decodeJwt(jwtCookie.getValue());
    Date expiresAt = decoded.getExpiresAt();
    Date issuedAt = decoded.getIssuedAt();
    long expirySeconds = (expiresAt.getTime() - issuedAt.getTime()) / 1000;
    assertTrue("Expiry should be approximately 600 seconds", Math.abs(expirySeconds - 600) <= 1);
  }

  @Test(expected = IllegalStateException.class)
  public void testSetExpirySeconds_rejectsZero() {
    underTest.setExpirySeconds(0);
  }

  @Test(expected = IllegalStateException.class)
  public void testSetExpirySeconds_rejectsNegative() {
    underTest.setExpirySeconds(-1);
  }

  @Test
  public void testSetExpirySeconds_threadSafety() throws Exception {
    // Test concurrent updates don't cause race conditions
    Thread t1 = new Thread(() -> {
      for (int i = 0; i < 100; i++) {
        underTest.setExpirySeconds(300 + i);
      }
    });

    Thread t2 = new Thread(() -> {
      for (int i = 0; i < 100; i++) {
        underTest.setExpirySeconds(600 + i);
      }
    });

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    // Final value should be valid (either from t1 or t2)
    int finalExpiry = underTest.getExpirySeconds();
    assertTrue("Final expiry should be in valid range",
        (finalExpiry >= 300 && finalExpiry < 400) || (finalExpiry >= 600 && finalExpiry < 700));
  }
}
