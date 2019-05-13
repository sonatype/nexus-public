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

import java.util.Arrays;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.onboarding.OnboardingConfiguration;
import org.sonatype.nexus.onboarding.OnboardingItem;
import org.sonatype.nexus.onboarding.OnboardingManager;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

public class OnboardingStateContributorTest
    extends TestSupport
{
  @Mock
  private OnboardingConfiguration onboardingConfiguration;

  @Mock
  private OnboardingManager onboardingManager;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private OnboardingItem onboardingItem1;

  @Mock
  private OnboardingItem onboardingItem2;

  @Mock
  private Subject subject;

  private OnboardingStateContributor underTest;

  @Before
  public void setup() {
    when(onboardingConfiguration.isEnabled()).thenReturn(true);
    when(onboardingManager.getOnboardingItems()).thenReturn(Arrays.asList(onboardingItem1, onboardingItem2));
    when(onboardingManager.needsOnboarding()).thenReturn(true);
    when(securityHelper.subject()).thenReturn(subject);
    when(subject.isPermitted("nexus:*")).thenReturn(true);
    when(subject.getPrincipal()).thenReturn(new Object());

    underTest = new OnboardingStateContributor(onboardingConfiguration, onboardingManager, securityHelper);
  }

  @Test
  public void testGetState() {
    Map<String, Object> state = underTest.getState();
    assertThat(state.size(), is(1));
    assertThat(state.get("onboarding.required"), is(true));
  }

  @Test
  public void testGetState_noItems() {
    when(onboardingManager.needsOnboarding()).thenReturn(false);

    Map<String, Object> state = underTest.getState();
    assertThat(state, nullValue());
  }

  @Test
  public void testGetState_noAuthz() {
    when(subject.isPermitted("nexus:*")).thenReturn(false);

    Map<String, Object> state = underTest.getState();
    assertThat(state, nullValue());
  }

  @Test
  public void testGetState_noPrincipal() {
    when(subject.getPrincipal()).thenReturn(null);

    Map<String, Object> state = underTest.getState();
    assertThat(state, nullValue());
  }

  @Test
  public void testGetState_cacheOnboardingState() {
    Map<String, Object> state = underTest.getState();
    assertThat(state.get("onboarding.required"), is(true));

    when(onboardingManager.needsOnboarding()).thenReturn(false);

    state = underTest.getState();
    assertThat(state, nullValue());

    //set to true to validate that cache kicks in and still doesn't add data to the map
    when(onboardingManager.needsOnboarding()).thenReturn(true);

    state = underTest.getState();
    assertThat(state, nullValue());
  }
}
