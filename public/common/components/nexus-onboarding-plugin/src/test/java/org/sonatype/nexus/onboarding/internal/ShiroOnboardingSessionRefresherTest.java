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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShiroOnboardingSessionRefresherTest
{
  @Mock
  private Subject subject;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private MockedStatic<SecurityUtils> securityUtilsStatic;

  private ShiroOnboardingSessionRefresher underTest;

  @BeforeEach
  void setUp() {
    underTest = new ShiroOnboardingSessionRefresher();
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
  void shouldReAuthenticateSubjectWhenCallerIsTargetUser() {
    when(subject.getPrincipal()).thenReturn("admin");

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    ArgumentCaptor<UsernamePasswordToken> tokenCaptor = ArgumentCaptor.forClass(UsernamePasswordToken.class);
    verify(subject).login(tokenCaptor.capture());
    UsernamePasswordToken token = tokenCaptor.getValue();
    assertThat(token.getUsername(), is("admin"));
    assertThat(new String(token.getPassword()), is("newpass"));
  }

  @Test
  void shouldNoOpWhenCallerIsDifferentUser() {
    when(subject.getPrincipal()).thenReturn("bob");

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(subject, never()).login(any(UsernamePasswordToken.class));
  }

  @Test
  void shouldNoOpWhenSubjectIsAnonymous() {
    securityUtilsStatic.when(SecurityUtils::getSubject).thenReturn(null);

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(subject, never()).login(any(UsernamePasswordToken.class));
  }

  @Test
  void shouldNoOpWhenSubjectPrincipalIsNull() {
    when(subject.getPrincipal()).thenReturn(null);

    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(subject, never()).login(any(UsernamePasswordToken.class));
  }

  @Test
  void shouldSwallowAuthenticationExceptionAndLogWarning() {
    when(subject.getPrincipal()).thenReturn("admin");
    doThrow(new AuthenticationException("bad creds")).when(subject).login(any(UsernamePasswordToken.class));

    // Should not throw
    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(subject).login(any(UsernamePasswordToken.class));
  }

  @Test
  void shouldSwallowUnexpectedRuntimeExceptionAndLogWarning() {
    when(subject.getPrincipal()).thenReturn("admin");
    doThrow(new IllegalStateException("shiro internal failure")).when(subject).login(any(UsernamePasswordToken.class));

    // Password change and admin.password removal already succeeded upstream — the refresher
    // must not propagate an unexpected runtime exception and turn the 204 into a 500.
    underTest.refreshIfSelfChange("admin", "newpass", request, response);

    verify(subject).login(any(UsernamePasswordToken.class));
  }
}
