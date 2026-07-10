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

import org.sonatype.nexus.security.jwt.JwtSessionRevocationService;
import org.sonatype.nexus.security.jwt.JwtVerificationException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.JwtHelper.JWT_COOKIE_NAME;

/**
 * Test for {@link JwtFilter}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class JwtFilterTest
{
  private static final String NEW_JWT = "new-jwt";

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @Mock
  private JwtHelper jwtHelper;

  @Mock
  private JwtSessionRevocationService jwtSessionRevocationService;

  private JwtFilter jwtFilter;

  @Before
  public void setupFilter() {
    this.jwtFilter = new JwtFilter(jwtHelper, jwtSessionRevocationService);
    when(request.getRequestURI()).thenReturn("/somepath");
  }

  @Test
  public void testPreHandle_successfulRefresh() throws Exception {
    String jwt = signedJwt("alice", "default", "session-1", new Date());
    Cookie oldCookie = makeCookie(jwt);
    Cookie newCookie = makeCookie(NEW_JWT);

    when(request.getCookies()).thenReturn(new Cookie[]{oldCookie});
    DecodedJWT decodedJwt = JWT.decode(jwt);
    when(jwtHelper.verifyJwt(jwt)).thenReturn(decodedJwt);
    when(jwtSessionRevocationService.isRevoked("session-1")).thenReturn(false);
    when(jwtSessionRevocationService.isUserInvalidatedAfter(anyString(), any())).thenReturn(false);
    when(jwtHelper.refreshJwtCookie(decodedJwt, false)).thenReturn(newCookie);

    jwtFilter.preHandle(request, response);

    verify(response).addCookie(newCookie);
  }

  @Test
  public void testPreHandle_invalidJwt_expiresCookie() throws Exception {
    Cookie oldCookie = makeCookie("bad-jwt");
    when(request.getCookies()).thenReturn(new Cookie[]{oldCookie});
    when(jwtHelper.verifyJwt("bad-jwt")).thenThrow(new JwtVerificationException("Invalid JWT"));

    jwtFilter.preHandle(request, response);

    oldCookie.setValue("");
    oldCookie.setMaxAge(0);
    verify(response).addCookie(oldCookie);
    verify(jwtHelper, never()).refreshJwtCookie(any(DecodedJWT.class), any(Boolean.class));
  }

  @Test
  public void testPreHandle_revokedSession_expiresCookie_andDoesNotRefresh() throws Exception {
    String jwt = signedJwt("alice", "default", "session-1", new Date());
    Cookie oldCookie = makeCookie(jwt);

    when(request.getCookies()).thenReturn(new Cookie[]{oldCookie});
    when(jwtHelper.verifyJwt(jwt)).thenReturn(JWT.decode(jwt));
    when(jwtSessionRevocationService.isRevoked("session-1")).thenReturn(true);

    jwtFilter.preHandle(request, response);

    // Cookie expired, no refresh call
    oldCookie.setValue("");
    oldCookie.setMaxAge(0);
    verify(response).addCookie(oldCookie);
    verify(jwtHelper, never()).refreshJwtCookie(any(DecodedJWT.class), any(Boolean.class));
  }

  @Test
  public void testPreHandle_userInvalidatedAfterIat_expiresCookie_andDoesNotRefresh() throws Exception {
    Date iat = new Date(System.currentTimeMillis() - 5000);
    String jwt = signedJwt("alice", "default", "session-1", iat);
    Cookie oldCookie = makeCookie(jwt);

    when(request.getCookies()).thenReturn(new Cookie[]{oldCookie});
    when(jwtHelper.verifyJwt(jwt)).thenReturn(JWT.decode(jwt));
    when(jwtSessionRevocationService.isRevoked("session-1")).thenReturn(false);
    when(jwtSessionRevocationService.isUserInvalidatedAfter(anyString(), any())).thenReturn(true);

    jwtFilter.preHandle(request, response);

    oldCookie.setValue("");
    oldCookie.setMaxAge(0);
    verify(response).addCookie(oldCookie);
    verify(jwtHelper, never()).refreshJwtCookie(any(DecodedJWT.class), any(Boolean.class));
  }

  @Test
  public void testPreHandle_noJwtCookie() throws Exception {
    when(request.getCookies()).thenReturn(new Cookie[]{});

    jwtFilter.preHandle(request, response);

    verifyNoInteractions(response);
  }

  private static String signedJwt(final String user, final String realm, final String sessionId, final Date iat) {
    return JWT.create()
        .withIssuer("sonatype")
        .withClaim("user", user)
        .withClaim("realm", realm)
        .withClaim("userSessionId", sessionId)
        .withIssuedAt(iat)
        .withExpiresAt(new Date(iat.getTime() + 1_800_000))
        .sign(Algorithm.HMAC256("test-secret"));
  }

  private Cookie makeCookie(final String jwt) {
    Cookie cookie = new Cookie(JWT_COOKIE_NAME, jwt);
    cookie.setMaxAge(300);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    return cookie;
  }
}
