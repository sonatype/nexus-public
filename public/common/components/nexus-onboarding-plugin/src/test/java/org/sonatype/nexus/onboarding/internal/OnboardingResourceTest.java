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
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.ws.rs.WebApplicationException;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.onboarding.OnboardingManager;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.onboarding.internal.OnboardingResource.PASSWORD_REQUIRED;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class OnboardingResourceTest
{
  @ValidationExecutor
  private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Mock
  private OnboardingManager onboardingManager;

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private AdminPasswordFileManager adminPasswordFileManager;

  @Mock
  private OnboardingSessionRefresher sessionRefresher;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @InjectMocks
  private OnboardingResource underTest;

  @Test
  void testChangeAdminPassword() throws Exception {
    when(onboardingManager.needsOnboarding()).thenReturn(true);

    underTest.changeAdminPassword("newpass", request, response);

    verify(securitySystem).changePassword("admin", "newpass", true);
    verify(adminPasswordFileManager).removeFile();
  }

  @Test
  void shouldInvokeSessionRefresherAfterChangingAdminPassword() throws Exception {
    when(onboardingManager.needsOnboarding()).thenReturn(true);

    underTest.changeAdminPassword("newpass", request, response);

    InOrder inOrder = inOrder(securitySystem, adminPasswordFileManager, sessionRefresher);
    inOrder.verify(securitySystem).changePassword("admin", "newpass", true);
    inOrder.verify(adminPasswordFileManager).removeFile();
    inOrder.verify(sessionRefresher).refreshIfSelfChange("admin", "newpass", request, response);
  }

  @Test
  void shouldNotInvokeSessionRefresherWhenNeedsOnboardingReturnsFalse() {
    when(onboardingManager.needsOnboarding()).thenReturn(false);

    assertThrows(WebApplicationException.class,
        () -> underTest.changeAdminPassword("newpass", request, response));

    verifyNoInteractions(sessionRefresher);
  }

  @Test
  void shouldNotInvokeSessionRefresherWhenUserNotFound() throws Exception {
    when(onboardingManager.needsOnboarding()).thenReturn(true);
    doThrow(new UserNotFoundException("admin")).when(securitySystem)
        .changePassword("admin", "newpass", true);

    WebApplicationException e = assertThrows(WebApplicationException.class,
        () -> underTest.changeAdminPassword("newpass", request, response));
    assertThat(e.getResponse().getStatus(), is(NOT_FOUND.getStatusCode()));

    verify(adminPasswordFileManager, never()).removeFile();
    verifyNoInteractions(sessionRefresher);
  }

  @Test
  void testChangeAdminPassword_blockedWhenOnboardingComplete() {
    when(onboardingManager.needsOnboarding()).thenReturn(false);

    WebApplicationException e =
        assertThrows(WebApplicationException.class,
            () -> underTest.changeAdminPassword("newpass", request, response));
    assertThat(e.getResponse().getStatus(), is(FORBIDDEN.getStatusCode()));
    verifyNoInteractions(securitySystem, adminPasswordFileManager);
  }

  // needsOnboarding() is not stubbed in these tests because @Validate (via ValidationExtension)
  // intercepts the call and throws ConstraintViolationException before the method body executes,
  // so the lifecycle gate is never reached.
  @Test
  void testChangeAdminPassword_empty() {
    ConstraintViolationException e =
        assertThrows(ConstraintViolationException.class,
            () -> underTest.changeAdminPassword("", request, response));
    assertThat(e.getConstraintViolations().iterator().next().getMessage(), is(PASSWORD_REQUIRED));
  }

  @Test
  void testChangeAdminPassword_null() {
    ConstraintViolationException e =
        assertThrows(ConstraintViolationException.class,
            () -> underTest.changeAdminPassword(null, request, response));
    assertThat(e.getConstraintViolations().iterator().next().getMessage(), is(PASSWORD_REQUIRED));
  }
}
