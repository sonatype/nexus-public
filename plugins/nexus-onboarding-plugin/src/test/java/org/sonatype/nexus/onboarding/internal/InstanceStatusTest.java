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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.onboarding.capability.OnboardingCapability;
import org.sonatype.nexus.onboarding.capability.OnboardingCapabilityHelper;
import org.sonatype.nexus.security.anonymous.AnonymousManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class InstanceStatusTest
    extends TestSupport
{
  private InstanceStatus underTest;

  @Mock
  private AnonymousManager anonymousManager;

  @Mock
  private OnboardingCapabilityHelper onboardingCapabilityHelper;

  @Mock
  private OnboardingCapability onboardingCapability;

  @Before
  public void setup() {
    underTest = new InstanceStatus(anonymousManager, onboardingCapabilityHelper);
    when(onboardingCapabilityHelper.getOnboardingCapability()).thenReturn(onboardingCapability);
  }

  @Test
  public void test_instanceIsNew_whenAnonymousNotConfigured() {
    when(anonymousManager.isConfigured()).thenReturn(false);

    assertThat(underTest.isNew(), is(true));
    assertThat(underTest.isUpgraded(), is(false));
  }

  @Test
  public void test_instanceIsUpgraded_whenAnonymousConfiguredButRegistrationNotStarted() {
    when(anonymousManager.isConfigured()).thenReturn(true);
    when(onboardingCapability.isRegistrationStarted()).thenReturn(false);

    assertThat(underTest.isNew(), is(false));
    assertThat(underTest.isUpgraded(), is(true));
  }

  @Test
  public void test_instanceIsNew_whenAnonymousConfiguredAndRegistrationStartedButNotCompleted() {
    when(anonymousManager.isConfigured()).thenReturn(true);
    when(onboardingCapability.isRegistrationStarted()).thenReturn(true);
    when(onboardingCapability.isRegistrationCompleted()).thenReturn(false);

    assertThat(underTest.isNew(), is(true));
    assertThat(underTest.isUpgraded(), is(false));
  }
}
