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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class ProStarterInformationPageOnboardingItemTest
    extends TestSupport
{
  @Mock
  private InstanceStatus instanceStatus;

  @Mock
  private OnboardingCapabilityHelper onboardingCapabilityHelper;

  @Mock
  private OnboardingCapability onboardingCapability;

  private ProStarterInformationPageOnboardingItem underTest;

  @Before
  public void setup() {
    when(onboardingCapabilityHelper.getOnboardingCapability()).thenReturn(onboardingCapability);
    underTest = new ProStarterInformationPageOnboardingItem(instanceStatus, onboardingCapabilityHelper);
  }

  @Test
  public void testAppliesForUpgradedInstanceAndProStarterCompleted() {
    when(instanceStatus.isUpgraded()).thenReturn(true);
    when(onboardingCapability.isProStarterInfoPageCompleted()).thenReturn(true);

    assertThat(underTest.applies(), is(false));
  }

  @Test
  public void testAppliesForUpgradedInstanceAndProStarterNotCompleted() {
    when(instanceStatus.isUpgraded()).thenReturn(true);
    when(onboardingCapability.isProStarterInfoPageCompleted()).thenReturn(false);

    assertThat(underTest.applies(), is(true));
  }

  @Test
  public void testAppliesForNotUpgradedInstanceAndProStarterCompleted() {
    when(instanceStatus.isUpgraded()).thenReturn(false);
    when(onboardingCapability.isProStarterInfoPageCompleted()).thenReturn(true);

    assertThat(underTest.applies(), is(false));
  }

  @Test
  public void testAppliesForNotUpgradedInstanceAndProStarterNotCompleted() {
    when(instanceStatus.isUpgraded()).thenReturn(false);
    when(onboardingCapability.isProStarterInfoPageCompleted()).thenReturn(false);

    assertThat(underTest.applies(), is(false));
  }

  @Test
  public void testGetType() {
    assertThat(underTest.getType(), is("ProStarterInformationPage"));
  }

  @Test
  public void testGetPriority() {
    assertThat(underTest.getPriority(), is(0));
  }
}
