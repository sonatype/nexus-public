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

import java.util.Date;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;
import org.sonatype.nexus.security.jwt.JwtVerificationException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.subject.WebSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;

/**
 * Tests for {@link JwtSecurityFilter} focusing on JWT session revocation logic.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class JwtSecurityFilterTest
{
  @Mock
  private WebSecurityManager webSecurityManager;

  @Mock
  private FilterChainResolver filterChainResolver;

  @Mock
  private JwtHelper jwtHelper;

  @Mock
  private JwtSessionRevocationService jwtSessionRevocationService;

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private WebSubject fallbackSubject;

  private JwtSecurityFilter underTest;

  @Before
  public void setup() throws Exception {
    underTest = new JwtSecurityFilter(
        webSecurityManager,
        filterChainResolver,
        jwtHelper,
        jwtSessionRevocationService,
        auditRecorder);

    // Setup default request behavior
    when(request.getRemoteHost()).thenReturn("localhost");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    // Mock WebSecurityManager to return a WebSubject when super.createSubject() is called
    when(webSecurityManager.createSubject(any())).thenReturn(fallbackSubject);
  }

  @Test
  public void testCreateSubject_noCookies() throws Exception {
    when(request.getCookies()).thenReturn(null);

    WebSubject subject = underTest.createSubject(request, response);

    // Should fall through to super.createSubject() which creates unauthenticated subject
    assertThat(subject, notNullValue());
    verify(jwtHelper, never()).verifyJwt(anyString());
  }

  @Test
  public void testCreateSubject_noJwtCookie() throws Exception {
    Cookie otherCookie = new Cookie("SESSIONID", "session-value");
    when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());
    verify(jwtHelper, never()).verifyJwt(anyString());
  }

  @Test
  public void testCreateSubject_emptyJwtValue() throws Exception {
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, "");
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());
    verify(jwtHelper, never()).verifyJwt(anyString());
  }

  @Test
  public void testCreateSubject_invalidJwt() throws Exception {
    String invalidJwt = "invalid-jwt-token";
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, invalidJwt);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
    when(jwtHelper.verifyJwt(invalidJwt)).thenThrow(new JwtVerificationException("Invalid JWT"));

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());

    // Verify cookie was expired
    ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookieCaptor.capture());
    Cookie expiredCookie = cookieCaptor.getValue();
    assertThat(expiredCookie.getValue(), is(""));
    assertThat(expiredCookie.getMaxAge(), is(0));
  }

  @Test
  public void testCreateSubject_validJwtWithoutUserSessionId() throws Exception {
    // Create JWT without userSessionId claim (backward compatibility)
    String jwtToken = createJwtWithoutUserSessionId("testuser", "default");
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtHelper.getExpirySeconds()).thenReturn(1800);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());
    assertThat(subject.isAuthenticated(), is(true));

    // Verify revocation was not checked (backward compatibility)
    verify(jwtSessionRevocationService, never()).isRevoked(anyString());
    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  public void testCreateSubject_validJwtWithNonRevokedSession() throws Exception {
    String userSessionId = "valid-session-id";
    String jwtToken = createJwtWithUserSessionId("testuser", "default", userSessionId);
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtHelper.getExpirySeconds()).thenReturn(1800);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(false);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());
    assertThat(subject.isAuthenticated(), is(true));

    // Verify revocation was checked
    verify(jwtSessionRevocationService).isRevoked(userSessionId);
    // No cookie expiration
    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  public void testCreateSubject_validJwtWithRevokedSession() throws Exception {
    String userSessionId = "revoked-session-id";
    String username = "testuser";
    String jwtToken = createJwtWithUserSessionId(username, "default", userSessionId);
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(true);
    when(auditRecorder.isEnabled()).thenReturn(true);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());

    // Verify revocation was checked
    verify(jwtSessionRevocationService).isRevoked(userSessionId);

    // Verify cookie was expired
    ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookieCaptor.capture());
    Cookie expiredCookie = cookieCaptor.getValue();
    assertThat(expiredCookie.getValue(), is(""));
    assertThat(expiredCookie.getMaxAge(), is(0));

    // Verify audit was recorded
    ArgumentCaptor<AuditData> auditCaptor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(auditCaptor.capture());
    AuditData auditData = auditCaptor.getValue();
    assertThat(auditData.getDomain(), is("security.jwt"));
    assertThat(auditData.getType(), is("revoked-token-attempt"));
    assertThat(auditData.getContext(), is(username));
    assertThat(auditData.getAttributes().get("username"), is(username));
    assertThat(auditData.getAttributes().get("sessionId"), is(userSessionId));
    assertThat(auditData.getAttributes().get("remoteAddr"), is("127.0.0.1"));
    assertThat(auditData.getAttributes().get("remoteHost"), is("localhost"));
  }

  @Test
  public void testCreateSubject_revokedSessionWithoutUserClaim() throws Exception {
    String userSessionId = "revoked-session-id";
    // Create JWT without user claim
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000);
    String jwtToken = JWT.create()
        .withIssuer("sonatype")
        .withClaim("realm", "default")
        .withClaim("userSessionId", userSessionId)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));

    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(true);
    when(auditRecorder.isEnabled()).thenReturn(true);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());

    // Verify audit was recorded - username will be null when claim is missing
    // (Note: getClaim() returns a non-null Claim object with null asString() value)
    ArgumentCaptor<AuditData> auditCaptor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(auditCaptor.capture());
    AuditData auditData = auditCaptor.getValue();
    assertThat(auditData.getAttributes().get("username"), is(nullValue()));
  }

  @Test
  public void testCreateSubject_revokedSessionWithAuditDisabled() throws Exception {
    String userSessionId = "revoked-session-id";
    String jwtToken = createJwtWithUserSessionId("testuser", "default", userSessionId);
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(true);
    when(auditRecorder.isEnabled()).thenReturn(false);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());

    // Verify audit was NOT recorded
    verify(auditRecorder, never()).record(any(AuditData.class));

    // But cookie should still be expired
    verify(response).addCookie(any(Cookie.class));
  }

  @Test
  public void testCreateSubject_validJwtWithNullUserSessionIdClaim() throws Exception {
    String jwtToken = createJwtWithUserSessionId("testuser", "default", "test-session");
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = mock(DecodedJWT.class);
    Claim nullClaim = mock(Claim.class);
    Claim userClaim = mock(Claim.class);
    Claim realmClaim = mock(Claim.class);

    when(nullClaim.isNull()).thenReturn(true);
    when(userClaim.asString()).thenReturn("testuser");
    when(realmClaim.asString()).thenReturn("default");

    when(decodedJWT.getClaim("userSessionId")).thenReturn(nullClaim);
    when(decodedJWT.getClaim("user")).thenReturn(userClaim);
    when(decodedJWT.getClaim("realm")).thenReturn(realmClaim);

    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtHelper.getExpirySeconds()).thenReturn(1800);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());
    assertThat(subject.isAuthenticated(), is(true));

    // Verify revocation was NOT checked when claim is null
    verify(jwtSessionRevocationService, never()).isRevoked(anyString());
  }

  @Test
  public void testCreateSubject_withNullUserClaim() throws Exception {
    String jwtToken = createJwtWithUserSessionId("testuser", "default", "test-session");
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = mock(DecodedJWT.class);
    Claim userSessionIdClaim = mock(Claim.class);
    Claim nullUserClaim = mock(Claim.class);
    Claim realmClaim = mock(Claim.class);

    when(userSessionIdClaim.isNull()).thenReturn(false);
    when(userSessionIdClaim.asString()).thenReturn("test-session");
    when(nullUserClaim.asString()).thenReturn(null); // Null user
    when(realmClaim.asString()).thenReturn("default");

    when(decodedJWT.getClaim("userSessionId")).thenReturn(userSessionIdClaim);
    when(decodedJWT.getClaim("user")).thenReturn(nullUserClaim);
    when(decodedJWT.getClaim("realm")).thenReturn(realmClaim);

    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    WebSubject subject = underTest.createSubject(request, response);

    // Should fall back to unauthenticated subject when user is null
    assertThat(subject, notNullValue());
    verify(jwtHelper).verifyJwt(jwtToken);
  }

  @Test
  public void testCreateSubject_withNullRealmClaim() throws Exception {
    String jwtToken = createJwtWithUserSessionId("testuser", "default", "test-session");
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = mock(DecodedJWT.class);
    Claim userSessionIdClaim = mock(Claim.class);
    Claim userClaim = mock(Claim.class);
    Claim nullRealmClaim = mock(Claim.class);

    when(userSessionIdClaim.isNull()).thenReturn(false);
    when(userSessionIdClaim.asString()).thenReturn("test-session");
    when(userClaim.asString()).thenReturn("testuser");
    when(nullRealmClaim.asString()).thenReturn(null); // Null realm

    when(decodedJWT.getClaim("userSessionId")).thenReturn(userSessionIdClaim);
    when(decodedJWT.getClaim("user")).thenReturn(userClaim);
    when(decodedJWT.getClaim("realm")).thenReturn(nullRealmClaim);

    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);

    WebSubject subject = underTest.createSubject(request, response);

    // Should fall back to unauthenticated subject when realm is null
    assertThat(subject, notNullValue());
    verify(jwtHelper).verifyJwt(jwtToken);
  }

  @Test
  public void testCreateSubject_userInvalidatedAfterJwtIat_returns401_andClearsCookie() throws Exception {
    String userSessionId = "valid-session-id";
    String username = "alice";
    Date issuedAt = new Date(System.currentTimeMillis() - 5000);
    String jwtToken = createJwtWithIat(username, "default", userSessionId, issuedAt);
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(false);
    when(jwtSessionRevocationService.isUserInvalidatedAfter(
        org.mockito.ArgumentMatchers.eq(username),
        any())).thenReturn(true);
    when(auditRecorder.isEnabled()).thenReturn(true);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());
    // Not authenticated — fell through to fallback subject
    assertThat(subject, is(fallbackSubject));

    // Cookie expired
    ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookieCaptor.capture());
    Cookie expired = cookieCaptor.getValue();
    assertThat(expired.getValue(), is(""));
    assertThat(expired.getMaxAge(), is(0));

    // Audit recorded under same domain as other revocation events
    ArgumentCaptor<AuditData> auditCaptor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(auditCaptor.capture());
    AuditData data = auditCaptor.getValue();
    assertThat(data.getDomain(), is("security.jwt"));
    assertThat(data.getType(), is("revoked-token-attempt"));
    assertThat(data.getAttributes().get("username"), is(username));
  }

  @Test
  public void testCreateSubject_userInvalidatedBeforeJwtIat_allows() throws Exception {
    String userSessionId = "valid-session-id";
    String username = "alice";
    Date issuedAt = new Date(System.currentTimeMillis() - 1000);
    String jwtToken = createJwtWithIat(username, "default", userSessionId, issuedAt);
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtHelper.getExpirySeconds()).thenReturn(1800);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(false);
    when(jwtSessionRevocationService.isUserInvalidatedAfter(
        anyString(), any())).thenReturn(false);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());
    assertThat(subject.isAuthenticated(), is(true));
    verify(jwtSessionRevocationService).isUserInvalidatedAfter(
        org.mockito.ArgumentMatchers.eq(username),
        any());
    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  public void testCreateSubject_noIat_skipsUserInvalidationCheck() throws Exception {
    // createJwtWithUserSessionId does not set iat — invalidation check is skipped
    String userSessionId = "valid-session-id";
    String jwtToken = createJwtWithUserSessionId("alice", "default", userSessionId);
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtHelper.getExpirySeconds()).thenReturn(1800);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(false);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, notNullValue());
    assertThat(subject.isAuthenticated(), is(true));
    verify(jwtSessionRevocationService, never()).isUserInvalidatedAfter(anyString(), any());
  }

  /**
   * Deleted users are rejected via the same mechanism as password change: DefaultSecuritySystem
   * calls SessionInvalidator.invalidateSessionsForUser on deletion, which records a cutoff that
   * JwtSecurityFilter reads through isUserInvalidatedAfter. No per-request user lookup is used.
   */
  @Test
  public void testCreateSubject_deletedUserInvalidation_returns401AndClearsCookie() throws Exception {
    String username = "deleted-user";
    String userSessionId = "some-session-id";
    Date issuedAt = new Date(System.currentTimeMillis() - 5000);
    String jwtToken = createJwtWithIat(username, "default", userSessionId, issuedAt);
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(false);
    when(jwtSessionRevocationService.isUserInvalidatedAfter(eq(username), any())).thenReturn(true);
    when(auditRecorder.isEnabled()).thenReturn(false);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, is(fallbackSubject));
    ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookieCaptor.capture());
    Cookie expiredCookie = cookieCaptor.getValue();
    assertThat(expiredCookie.getValue(), is(""));
    assertThat(expiredCookie.getMaxAge(), is(0));
  }

  /**
   * Deactivated users are rejected via the same mechanism as deletion: DefaultSecuritySystem
   * calls SessionInvalidator.invalidateSessionsForUser on the active→disabled transition, which
   * records a cutoff that JwtSecurityFilter reads through isUserInvalidatedAfter.
   */
  @Test
  public void testCreateSubject_deactivatedUserInvalidation_returns401AndClearsCookie() throws Exception {
    String username = "disabled-user";
    String userSessionId = "some-session-id";
    Date issuedAt = new Date(System.currentTimeMillis() - 5000);
    String jwtToken = createJwtWithIat(username, "default", userSessionId, issuedAt);
    Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
    when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

    DecodedJWT decodedJWT = JWT.decode(jwtToken);
    when(jwtHelper.verifyJwt(jwtToken)).thenReturn(decodedJWT);
    when(jwtSessionRevocationService.isRevoked(userSessionId)).thenReturn(false);
    when(jwtSessionRevocationService.isUserInvalidatedAfter(eq(username), any())).thenReturn(true);
    when(auditRecorder.isEnabled()).thenReturn(false);

    WebSubject subject = underTest.createSubject(request, response);

    assertThat(subject, is(fallbackSubject));
    ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookieCaptor.capture());
    Cookie expiredCookie = cookieCaptor.getValue();
    assertThat(expiredCookie.getValue(), is(""));
    assertThat(expiredCookie.getMaxAge(), is(0));
  }

  /**
   * Helper to create a JWT with an explicit issuedAt claim.
   */
  private String createJwtWithIat(String username, String realm, String userSessionId, Date issuedAt) {
    Date expiresAt = new Date(issuedAt.getTime() + 1800000);
    return JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", username)
        .withClaim("realm", realm)
        .withClaim("userSessionId", userSessionId)
        .withIssuedAt(issuedAt)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));
  }

  /**
   * Helper to create a JWT without userSessionId claim.
   */
  private String createJwtWithoutUserSessionId(String username, String realm) {
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000);
    return JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", username)
        .withClaim("realm", realm)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));
  }

  /**
   * Helper to create a JWT with userSessionId claim.
   */
  private String createJwtWithUserSessionId(String username, String realm, String userSessionId) {
    Date expiresAt = new Date(System.currentTimeMillis() + 1800000);
    return JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", username)
        .withClaim("realm", realm)
        .withClaim("userSessionId", userSessionId)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("test-secret"));
  }
}
