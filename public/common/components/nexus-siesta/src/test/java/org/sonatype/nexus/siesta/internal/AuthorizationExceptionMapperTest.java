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
package org.sonatype.nexus.siesta.internal;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.security.anonymous.AnonymousPrincipalCollection;

import jakarta.inject.Provider;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AuthorizationExceptionMapper}.
 * <p>
 * Uses WARN strictness because code intentionally calls getAttribute() with different keys,
 * and short-circuit evaluation of boolean expressions makes some stubs appear unused.
 */
@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class AuthorizationExceptionMapperTest
{
  @Mock
  private HttpServletRequest httpRequest;

  @Mock
  private Provider<HttpServletRequest> httpRequestProvider;

  @Mock
  private Subject subject;

  @Mock
  private AnonymousPrincipalCollection anonymousPrincipals;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  private AuthorizationExceptionMapper underTest;

  @BeforeEach
  void setup() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getSubject).thenReturn(subject);

    underTest = new AuthorizationExceptionMapper(httpRequestProvider);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Test
  void shouldReturn401WithAuthChallenge_whenUserIsNotAuthenticated() {
    // Given: user is not authenticated (no principal)
    when(subject.getPrincipal()).thenReturn(null);
    when(subject.isAuthenticated()).thenReturn(false);

    // Stub httpRequest and BOTH attributes using doReturn (Mockito recommendation for strict mode)
    doReturn(httpRequest).when(httpRequestProvider).get();
    doReturn("Bearer").when(httpRequest).getAttribute("auth.scheme");
    doReturn("Test Realm").when(httpRequest).getAttribute("auth.realm");

    // When: converting authorization exception
    try (Response response = underTest.convert(new AuthorizationException(), "test-id")) {
      // Then: should return 401 Unauthorized with WWW-Authenticate header
      assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
      assertThat(response.getHeaderString("WWW-Authenticate"), is("Bearer realm=\"Test Realm\""));
    }
  }

  @Test
  void shouldReturn403WithoutAuthChallenge_whenUserIsAuthenticatedButLacksPermission() {
    // Given: user is authenticated (has principal) but lacks required permission
    when(subject.getPrincipal()).thenReturn("testuser");
    when(subject.isAuthenticated()).thenReturn(true);

    // Do NOT stub httpRequestProvider - it's never called when authenticated=true

    // When: converting authorization exception
    try (Response response = underTest.convert(new AuthorizationException(), "test-id")) {
      // Then: should return 403 Forbidden without WWW-Authenticate header
      assertThat(response.getStatus(), is(Status.FORBIDDEN.getStatusCode()));
      assertThat(response.getHeaderString("WWW-Authenticate"), is(nullValue()));
    }
  }

  @Test
  void shouldReturn403_whenAnonymousUserIsAuthenticatedButLacksPermission() {
    // Given: anonymous user (has principal="anonymous")
    // Note: Anonymous has isAuthenticated()=false in Shiro but is treated as authenticated
    when(subject.getPrincipal()).thenReturn("anonymous");
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.getPrincipals()).thenReturn(anonymousPrincipals); // AnonymousPrincipalCollection

    // Do NOT stub httpRequestProvider - it's never called when authenticated=true

    // When: converting authorization exception
    try (Response response = underTest.convert(new AuthorizationException(), "test-id")) {
      // Then: should return 403 Forbidden (anonymous is technically authenticated)
      assertThat(response.getStatus(), is(Status.FORBIDDEN.getStatusCode()));
      assertThat(response.getHeaderString("WWW-Authenticate"), is(nullValue()));
    }
  }

  @Test
  void shouldReturn401_whenPrincipalExistsButUserNotAuthenticated() {
    // Given: subject has principal but isAuthenticated() returns false
    // (edge case: remembered user but not authenticated in current session)
    when(subject.getPrincipal()).thenReturn("remembereduser");
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.getPrincipals()).thenReturn(null); // Not AnonymousPrincipalCollection

    // Stub httpRequest and BOTH attributes using doReturn (Mockito recommendation for strict mode)
    doReturn(httpRequest).when(httpRequestProvider).get();
    doReturn("Basic").when(httpRequest).getAttribute("auth.scheme");
    doReturn("Nexus").when(httpRequest).getAttribute("auth.realm");

    // When: converting authorization exception
    try (Response response = underTest.convert(new AuthorizationException(), "test-id")) {
      // Then: should return 401 because user is not authenticated
      // (both conditions must be true: has principal AND isAuthenticated)
      assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
      assertThat(response.getHeaderString("WWW-Authenticate"), is("Basic realm=\"Nexus\""));
    }
  }

  @Test
  void shouldReturn403WithoutAuthChallenge_whenUnauthenticatedRequestFromUi() {
    // Given: user is not authenticated (no principal) but request is from the UI
    when(subject.getPrincipal()).thenReturn(null);
    when(subject.isAuthenticated()).thenReturn(false);

    doReturn(httpRequest).when(httpRequestProvider).get();
    doReturn("true").when(httpRequest).getHeader("X-Nexus-UI");

    // When: converting authorization exception
    try (Response response = underTest.convert(new AuthorizationException(), "test-id")) {
      // Then: should return 403 Forbidden without WWW-Authenticate header
      // to avoid triggering the browser's native auth dialog
      assertThat(response.getStatus(), is(Status.FORBIDDEN.getStatusCode()));
      assertThat(response.getHeaderString("WWW-Authenticate"), is(nullValue()));
    }
  }
}
