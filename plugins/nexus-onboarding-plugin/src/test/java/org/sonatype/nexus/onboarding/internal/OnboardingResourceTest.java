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

import javax.validation.ConstraintViolationException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.onboarding.OnboardingManager;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.validation.ValidationModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.onboarding.internal.OnboardingResource.PASSWORD_REQUIRED;

public class OnboardingResourceTest
    extends TestSupport
{
  @Mock
  private OnboardingManager onboardingManager;

  @Mock
  private SecuritySystem securitySystem;

  private OnboardingResource underTest;

  @Before
  public void setup() {
    underTest = Guice.createInjector(new ValidationModule(), new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(OnboardingManager.class).toInstance(onboardingManager);
        bind(SecuritySystem.class).toInstance(securitySystem);
      }
    }).getInstance(OnboardingResource.class);
  }

  @Test
  public void testChangeAdminPassword() throws Exception {
    underTest.changeAdminPassword("newpass");

    verify(securitySystem).changePassword("admin", "newpass", false);
  }

  @Test
  public void testChangeAdminPassword_empty() {
    try {
      underTest.changeAdminPassword("");
      fail("empty password should have failed validation");
    }
    catch (ConstraintViolationException e) {
      assertThat(e.getConstraintViolations().iterator().next().getMessage(), is(PASSWORD_REQUIRED));
    }
  }

  @Test
  public void testChangeAdminPassword_null() {
    try {
      underTest.changeAdminPassword(null);
      fail("null password should have failed validation");
    }
    catch (ConstraintViolationException e) {
      assertThat(e.getConstraintViolations().iterator().next().getMessage(), is(PASSWORD_REQUIRED));
    }
  }
}
