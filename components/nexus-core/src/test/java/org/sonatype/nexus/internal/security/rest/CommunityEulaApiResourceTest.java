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
package org.sonatype.nexus.internal.security.rest;

import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommunityEulaApiResourceTest
    extends TestSupport
{
  @Mock
  private GlobalKeyValueStore mockGlobalKeyValueStore;

  @InjectMocks
  private CommunityEulaApiResource underTest;

  @Test
  public void testGetCommunityEulaStatusNotSet() {
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.empty());
    EulaStatus eulaStatus = underTest.getCommunityEulaStatus();
    assertFalse(eulaStatus.isAccepted());
  }

  @Test
  public void testGetCommunityEulaStatusSet() {
    NexusKeyValue keyValue = new NexusKeyValue();
    keyValue.setValue(Map.of("accepted", true));
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.of(keyValue));
    EulaStatus eulaStatus = underTest.getCommunityEulaStatus();
    assertTrue(eulaStatus.isAccepted());
  }

  @Test
  public void testSetEulaAccepted() {
    EulaStatus eulaStatus = new EulaStatus();
    eulaStatus.setAccepted(true);
    eulaStatus.setDisclaimer(EulaStatus.EXPECTED_DISCLAIMER);
    underTest.setEulaAcceptedCE(eulaStatus);
    verify(mockGlobalKeyValueStore, times(1)).setKey(any(NexusKeyValue.class));
  }
}
