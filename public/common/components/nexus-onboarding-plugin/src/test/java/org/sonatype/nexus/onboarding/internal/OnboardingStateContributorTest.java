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

import org.sonatype.nexus.onboarding.OnboardingConfiguration;
import org.sonatype.nexus.onboarding.OnboardingItem;
import org.sonatype.nexus.onboarding.OnboardingManager;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.app.FeatureFlags.REACT_ONBOARDING_ENABLED;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class OnboardingStateContributorTest
{
  @Mock
  private OnboardingConfiguration onboardingConfiguration;

  @Mock
  private OnboardingManager onboardingManager;

  @Mock
  private AdminPasswordFileManager adminPasswordFileManager;

  @Mock
  private OnboardingItem onboardingItem1;

  @Mock
  private OnboardingItem onboardingItem2;

  private OnboardingStateContributor underTest;

  @Before
  public void setup() {
    when(onboardingConfiguration.isEnabled()).thenReturn(true);
    when(onboardingManager.getOnboardingItems()).thenReturn(Arrays.asList(onboardingItem1, onboardingItem2));
    when(onboardingManager.needsOnboarding()).thenReturn(true);
    when(adminPasswordFileManager.exists()).thenReturn(true);
    when(adminPasswordFileManager.getPath()).thenReturn("path/to/file");

    underTest =
        new OnboardingStateContributor(onboardingConfiguration, onboardingManager, adminPasswordFileManager, false);
  }

  @Test
  public void testGetState() {
    Map<String, Object> state = underTest.getState();
    assertThat(state.size(), is(3));
    assertThat(state.get("onboarding.required"), is(true));
    assertThat(state.get("admin.password.file"), is("path/to/file"));
    assertThat(state.get(REACT_ONBOARDING_ENABLED), is(false));
  }

  @Test
  public void testGetState_noItems() {
    when(onboardingManager.needsOnboarding()).thenReturn(false);
    when(adminPasswordFileManager.exists()).thenReturn(false);

    Map<String, Object> state = underTest.getState();

    // The React onboarding flag is always published so the frontend can read it via NX.State, regardless of whether
    // the ExtJS wizard has anything else to contribute.
    assertThat(state.size(), is(1));
    assertThat(state.get("onboarding.required"), nullValue());
    assertThat(state.get("admin.password.file"), nullValue());
    assertThat(state.get(REACT_ONBOARDING_ENABLED), is(false));
  }

  @Test
  public void testGetState_cacheOnboardingState() {
    Map<String, Object> state = underTest.getState();
    assertThat(state.get("onboarding.required"), is(true));

    when(onboardingManager.needsOnboarding()).thenReturn(false);

    state = underTest.getState();
    assertThat(state.get("onboarding.required"), nullValue());

    // set to true to validate that cache kicks in and still doesn't add data to the map
    when(onboardingManager.needsOnboarding()).thenReturn(true);

    state = underTest.getState();
    assertThat(state.get("onboarding.required"), nullValue());
  }

  @Test
  public void testGetState_noAdminPasswordFile() {
    when(adminPasswordFileManager.exists()).thenReturn(false);

    Map<String, Object> state = underTest.getState();
    assertThat(state.get("admin.password.file"), nullValue());
  }

  @Test
  public void testGetState_reactOnboardingFlagPublishedWhenEnabled() {
    underTest =
        new OnboardingStateContributor(onboardingConfiguration, onboardingManager, adminPasswordFileManager, true);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(REACT_ONBOARDING_ENABLED), is(true));
  }

  @Test
  public void testGetState_reactOnboardingFlagPublishedWhenDisabled() {
    underTest =
        new OnboardingStateContributor(onboardingConfiguration, onboardingManager, adminPasswordFileManager, false);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(REACT_ONBOARDING_ENABLED), is(false));
  }
}
