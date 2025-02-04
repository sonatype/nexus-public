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

import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class CommunityDiscoverOnboardingItemTest
    extends TestSupport
{
  @Mock
  private ApplicationVersion mockApplicationVersion;

  @Mock
  private GlobalKeyValueStore mockGlobalKeyValueStore;

  @InjectMocks
  private CommunityDiscoverOnboardingItem underTest;

  @Before
  public void setUp() {
    when(mockApplicationVersion.getEdition()).thenReturn("COMMUNITY");
  }

  @Test
  public void testAppliesWhenCommunityAndEulaNotAccepted() {
    when(mockGlobalKeyValueStore.getKey("nexus.community.eula.accepted")).thenReturn(Optional.empty());
    assertTrue(underTest.applies());
  }

  @Test
  public void testAppliesWhenCommunityAndEulaAccepted() {
    NexusKeyValue eulaStatus = new NexusKeyValue();
    eulaStatus.setValue(Map.of("accepted", true));
    when(mockGlobalKeyValueStore.getKey("nexus.community.eula.accepted")).thenReturn(Optional.of(eulaStatus));
    assertFalse(underTest.applies());
  }

  @Test
  public void testAppliesWhenNotCommunity() {
    when(mockApplicationVersion.getEdition()).thenReturn("PRO");
    assertFalse(underTest.applies());
  }
}
