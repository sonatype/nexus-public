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
package org.sonatype.nexus.rapture.internal.security;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.JwtHelper;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;
import static org.sonatype.nexus.servlet.XFrameOptions.DENY;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class JwtServletTest
{
  @Mock
  private HttpServletRequest httpServletRequest;

  @Mock
  private HttpServletResponse httpServletResponse;

  @Mock
  private Subject subject;

  @Mock
  private EventManager eventManager;

  @Mock
  private JwtHelper jwtHelper;

  @Mock
  private JwtSessionRevocationService jwtSessionRevocationService;

  private JwtServlet underTest;

  @Before
  public void setup() {
    underTest = new JwtServlet("/", eventManager, false, jwtHelper, jwtSessionRevocationService);
    PrincipalCollection principalCollection = mock(PrincipalCollection.class);
    when(subject.getPrincipals()).thenReturn(principalCollection);
    when(principalCollection.getRealmNames()).thenReturn(new HashSet<>(Arrays.asList("realm")));
    when(subject.getPrincipal()).thenReturn("someuser");

    when(subject.isAuthenticated()).thenReturn(true);
    ThreadContext.bind(subject);
  }

  @After
  public void cleanup() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void testDoPost() throws Exception {
    underTest.doPost(httpServletRequest, httpServletResponse);

    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, "DENY");
  }

  @Test
  public void testDoDelete() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
    cookie.setPath("/");
    cookie.setMaxAge(0);

    verify(httpServletResponse).addCookie(any(Cookie.class));
    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, "DENY");
  }

  @Test
  public void testDoDeleteRevokesJwtSession() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    // Create a mock JWT token
    String userSessionId = "test-session-id";
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000); // 30 minutes from now
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", "someuser")
        .withClaim("realm", "realm")
        .withClaim("userSessionId", userSessionId)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    // Mock JWT helper to return decoded JWT
    com.auth0.jwt.interfaces.DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that the session was revoked
    verify(jwtSessionRevocationService).revokeSession(
        eq(userSessionId),
        eq("someuser"),
        eq("realm"),
        any(OffsetDateTime.class));
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithoutCookieDoesNotRevoke() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);
    when(httpServletRequest.getCookies()).thenReturn(null);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that revocation was not attempted
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithInvalidJwtContinuesLogout() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, "invalid-jwt-token");
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    // Mock JWT helper to throw exception
    when(jwtHelper.verifyJwt(anyString())).thenThrow(new RuntimeException("Invalid JWT"));

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that logout still completes even if JWT verification fails
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithEmptyCookiesArrayDoesNotRevoke() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[0]);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that revocation was not attempted
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithNonJwtCookiesOnlyDoesNotRevoke() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    Cookie otherCookie1 = new Cookie("SESSIONID", "session-value");
    Cookie otherCookie2 = new Cookie("OTHER", "other-value");
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{otherCookie1, otherCookie2});

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that revocation was not attempted
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithEmptyJwtCookieValueDoesNotRevoke() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, "");
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that revocation was not attempted (empty value is filtered out)
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(jwtHelper, never()).verifyJwt(anyString());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithNullStringJwtCookieValueDoesNotRevoke() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, "null");
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    // "null" string is a valid JWT format but will fail verification
    when(jwtHelper.verifyJwt("null")).thenThrow(new RuntimeException("Invalid JWT"));

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that logout continues even with "null" string value
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithMissingUserSessionIdClaimDoesNotRevoke() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    // Create JWT without userSessionId claim
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000);
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", "someuser")
        .withClaim("realm", "realm")
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    com.auth0.jwt.interfaces.DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that revocation was not attempted due to missing claim
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithMissingRealmClaimDoesNotRevoke() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    // Create JWT without realm claim
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000);
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", "someuser")
        .withClaim("userSessionId", "test-session-id")
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    com.auth0.jwt.interfaces.DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that revocation was not attempted due to missing claim
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithMultipleCookiesIncludingJwt() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    // Create a valid JWT token
    String userSessionId = "test-session-id";
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000);
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", "someuser")
        .withClaim("realm", "realm")
        .withClaim("userSessionId", userSessionId)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));

    // Multiple cookies including JWT
    Cookie otherCookie = new Cookie("SESSIONID", "session-value");
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    Cookie anotherCookie = new Cookie("OTHER", "other-value");
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{otherCookie, jwtCookie, anotherCookie});

    com.auth0.jwt.interfaces.DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that the JWT was found and session was revoked despite other cookies
    verify(jwtSessionRevocationService).revokeSession(
        eq(userSessionId),
        eq("someuser"),
        eq("realm"),
        any(OffsetDateTime.class));
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithNullPrincipalStillRevokesJwtSession() throws Exception {
    // Given: subject with null principal (OIDC logout scenario)
    when(subject.getPrincipal()).thenReturn(null);
    when(httpServletRequest.isSecure()).thenReturn(true);

    // Create a valid JWT token with all required claims
    String userSessionId = "test-session-id";
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000);
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", "oidcuser")
        .withClaim("realm", "OAuth2Realm")
        .withClaim("userSessionId", userSessionId)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    com.auth0.jwt.interfaces.DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    // When: doDelete is called
    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Then: should return NO_CONTENT without throwing exception
    verify(httpServletResponse).setStatus(SC_NO_CONTENT);
    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, DENY);

    // And: should still revoke session using data from JWT cookie
    verify(jwtSessionRevocationService).revokeSession(
        eq(userSessionId),
        eq("oidcuser"),
        eq("OAuth2Realm"),
        any(OffsetDateTime.class));

    // And: should still clear the cookie
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithNullPrincipalAndNoCookieReturnsNoContent() throws Exception {
    // Given: subject with null principal and no cookies
    when(subject.getPrincipal()).thenReturn(null);
    when(httpServletRequest.isSecure()).thenReturn(true);
    when(httpServletRequest.getCookies()).thenReturn(null);

    // When: doDelete is called
    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Then: should return NO_CONTENT gracefully
    verify(httpServletResponse).setStatus(SC_NO_CONTENT);
    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, DENY);

    // And: should not attempt to revoke session
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());

    // But: should still clear the cookie
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithNullPrincipalAndInvalidJwtStillClearsCookie() throws Exception {
    // Given: subject with null principal and invalid JWT
    when(subject.getPrincipal()).thenReturn(null);
    when(httpServletRequest.isSecure()).thenReturn(true);

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, "invalid-jwt-token");
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    // Mock JWT helper to throw exception
    when(jwtHelper.verifyJwt(anyString())).thenThrow(new RuntimeException("Invalid JWT"));

    // When: doDelete is called
    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Then: should return NO_CONTENT gracefully
    verify(httpServletResponse).setStatus(SC_NO_CONTENT);
    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, DENY);

    // And: should not revoke session due to invalid JWT
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());

    // But: should still clear the cookie (most important part!)
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithNullPrincipalAndNullExpiresAtDoesNotRevoke() throws Exception {
    // Given: subject with null principal and JWT without expiresAt
    when(subject.getPrincipal()).thenReturn(null);
    when(httpServletRequest.isSecure()).thenReturn(true);

    // Create JWT without expiresAt claim
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", "oidcuser")
        .withClaim("realm", "OAuth2Realm")
        .withClaim("userSessionId", "test-session-id")
        .sign(Algorithm.HMAC256("test-secret"));

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    com.auth0.jwt.interfaces.DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    // When: doDelete is called
    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Then: should return NO_CONTENT
    verify(httpServletResponse).setStatus(SC_NO_CONTENT);
    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, DENY);

    // And: should not revoke session due to null expiresAt
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());

    // But: should still clear the cookie
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithNullPrincipalAndMissingUserClaimDoesNotRevoke() throws Exception {
    // Given: subject with null principal and JWT without user claim
    when(subject.getPrincipal()).thenReturn(null);
    when(httpServletRequest.isSecure()).thenReturn(true);

    // Create JWT without user claim
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000);
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("realm", "OAuth2Realm")
        .withClaim("userSessionId", "test-session-id")
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    com.auth0.jwt.interfaces.DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    // When: doDelete is called
    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Then: should return NO_CONTENT
    verify(httpServletResponse).setStatus(SC_NO_CONTENT);
    verify(httpServletResponse).setHeader(X_FRAME_OPTIONS, DENY);

    // And: should not revoke session due to missing user claim
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());

    // But: should still clear the cookie
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }

  @Test
  public void testDoDeleteWithNullExpiresAtDoesNotRevoke() throws Exception {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.isRemembered()).thenReturn(false);

    // Create JWT without expiresAt claim (will return null)
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", "someuser")
        .withClaim("realm", "realm")
        .withClaim("userSessionId", "test-session-id")
        .sign(Algorithm.HMAC256("test-secret"));

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    com.auth0.jwt.interfaces.DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    underTest.doDelete(httpServletRequest, httpServletResponse);

    // Verify that revocation was not attempted due to null expiresAt
    verify(jwtSessionRevocationService, never()).revokeSession(anyString(), anyString(), anyString(), any());
    verify(httpServletResponse).addCookie(any(Cookie.class));
  }
}
