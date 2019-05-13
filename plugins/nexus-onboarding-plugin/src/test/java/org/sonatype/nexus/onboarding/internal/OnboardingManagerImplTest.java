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

import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.onboarding.OnboardingConfiguration;
import org.sonatype.nexus.onboarding.OnboardingItem;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class OnboardingManagerImplTest
    extends TestSupport
{
  @Mock
  private OnboardingConfiguration onboardingConfiguration;

  @Mock
  private OnboardingItem onboardingItem1;

  @Mock
  private OnboardingItem onboardingItem2;

  @Mock
  private OnboardingItem onboardingItem3;

  private OnboardingManagerImpl underTest;

  @Before
  public void setup() {
    when(onboardingConfiguration.isEnabled()).thenReturn(true);
    when(onboardingItem1.applies()).thenReturn(true);
    when(onboardingItem1.getType()).thenReturn("type1");
    when(onboardingItem1.getPriority()).thenReturn(2);
    when(onboardingItem2.applies()).thenReturn(true);
    when(onboardingItem2.getType()).thenReturn("type2");
    when(onboardingItem2.getPriority()).thenReturn(1);
    when(onboardingItem3.applies()).thenReturn(true);
    when(onboardingItem3.getType()).thenReturn("type3");
    when(onboardingItem3.getPriority()).thenReturn(0);

    underTest = new OnboardingManagerImpl(ImmutableSet.of(onboardingItem1, onboardingItem2, onboardingItem3),
        onboardingConfiguration);
  }

  @Test
  public void testNeedsOnboarding() {
    assertThat(underTest.needsOnboarding(), is(true));
  }

  @Test
  public void testNeedsOnboarding_notAllItems() {
    when(onboardingItem1.applies()).thenReturn(false);
    when(onboardingItem2.applies()).thenReturn(false);
    when(onboardingItem3.applies()).thenReturn(true);

    assertThat(underTest.needsOnboarding(), is(true));
  }

  @Test
  public void testGetOnboardingItems() {
    List<OnboardingItem> items = underTest.getOnboardingItems();
    assertThat(items.size(), is(3));
    assertThat(items.get(0).getType(), is("type3"));
    assertThat(items.get(1).getType(), is("type2"));
    assertThat(items.get(2).getType(), is("type1"));
  }

  @Test
  public void testGetOnboardingItems_noItems() {
    underTest = new OnboardingManagerImpl(Collections.emptySet(), onboardingConfiguration);

    assertThat(underTest.getOnboardingItems().size(), is(0));
  }
}
