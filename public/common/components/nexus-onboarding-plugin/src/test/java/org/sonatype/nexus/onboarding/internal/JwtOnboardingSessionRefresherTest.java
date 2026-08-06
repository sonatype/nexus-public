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
package org.sonatype.nexus.onboarding.internal;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.security.JwtHelper;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtOnboardingSessionRefresherTest
{
  @Mock
  private JwtHelper jwtHelper;

  @Mock
  private Subject subject;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private MockedStatic<SecurityUtils> securityUtilsStatic;

  private JwtOnboardingSessionRefresher underTest;

  @BeforeEach
  void setUp() {
    underTest = new JwtOnboardingSessionRefresher(jwtHelper);
    securityUtilsStatic = mockStatic(SecurityUtils.class);
    securityUtilsStatic.when(SecurityUtils::getSubject).thenReturn(subject);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsStatic != null) {
      securityUtilsStatic.close();
    }
  }

  @Test
  void shouldMintFreshJwtCookieWhenCallerIsTargetUser() {
    Cookie fresh = new Cookie("NXSESSIONID", "fresh-token");
    when(subject.getPrincipal()).thenReturn("admin");
    when(request.isSecure()).thenReturn(false);
    when(jwtHelper.createJwtCookie(subject, false)).thenReturn(fresh);

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(jwtHelper).createJwtCookie(subject, false);
    verify(response).addCookie(fresh);
  }

  @Test
  void shouldPassSecureFlagThroughOnHttpsRequest() {
    Cookie fresh = new Cookie("NXSESSIONID", "fresh-token");
    when(subject.getPrincipal()).thenReturn("admin");
    when(request.isSecure()).thenReturn(true);
    when(jwtHelper.createJwtCookie(subject, true)).thenReturn(fresh);

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(jwtHelper).createJwtCookie(subject, true);
    verify(response).addCookie(fresh);
  }

  @Test
  void shouldNoOpWhenCallerIsDifferentUser() {
    when(subject.getPrincipal()).thenReturn("bob");

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(jwtHelper, never()).createJwtCookie(any(Subject.class), anyBoolean());
    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  void shouldNoOpWhenSubjectIsAnonymous() {
    securityUtilsStatic.when(SecurityUtils::getSubject).thenReturn(null);

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(jwtHelper, never()).createJwtCookie(any(Subject.class), anyBoolean());
    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  void shouldNoOpWhenSubjectPrincipalIsNull() {
    when(subject.getPrincipal()).thenReturn(null);

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(jwtHelper, never()).createJwtCookie(any(Subject.class), anyBoolean());
    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  void shouldSwallowExceptionAndLogWarning() {
    when(subject.getPrincipal()).thenReturn("admin");
    when(request.isSecure()).thenReturn(false);
    doThrow(new RuntimeException("boom")).when(jwtHelper).createJwtCookie(any(Subject.class), anyBoolean());

    // Should not throw
    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(response, never()).addCookie(any(Cookie.class));
  }
}
