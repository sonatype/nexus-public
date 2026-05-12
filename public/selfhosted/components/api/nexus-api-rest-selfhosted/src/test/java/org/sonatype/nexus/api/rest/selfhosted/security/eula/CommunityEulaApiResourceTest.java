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
package org.sonatype.nexus.api.rest.selfhosted.security.eula;

import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.api.rest.selfhosted.security.eula.model.EulaStatus;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class CommunityEulaApiResourceTest
{
  @Mock
  private GlobalKeyValueStore mockGlobalKeyValueStore;

  @InjectMocks
  private CommunityEulaApiResource underTest;

  @Test
  void testGetCommunityEulaStatusNotSet() {
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.empty());
    EulaStatus eulaStatus = underTest.getCommunityEulaStatus();
    assertFalse(eulaStatus.isAccepted());
  }

  @Test
  void testGetCommunityEulaStatusSet() {
    NexusKeyValue keyValue = new NexusKeyValue();
    keyValue.setValue(Map.of("accepted", true));
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.of(keyValue));
    EulaStatus eulaStatus = underTest.getCommunityEulaStatus();
    assertTrue(eulaStatus.isAccepted());
  }

  @Test
  void testSetEulaAccepted() {
    EulaStatus eulaStatus = new EulaStatus();
    eulaStatus.setAccepted(true);
    eulaStatus.setDisclaimer(EulaStatus.EXPECTED_DISCLAIMER);
    underTest.setEulaAcceptedCE(eulaStatus);
    verify(mockGlobalKeyValueStore, times(1)).setKey(any(NexusKeyValue.class));
  }

  @Test
  void testsAttemptToUnacceptEula() {
    NexusKeyValue keyValue = new NexusKeyValue();
    keyValue.setValue(Map.of("accepted", true));
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.of(keyValue));
    // Attempt to set EULA as not accepted
    EulaStatus eulaStatus = new EulaStatus();
    eulaStatus.setAccepted(false);
    eulaStatus.setDisclaimer(EulaStatus.EXPECTED_DISCLAIMER);
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
      underTest.setEulaAcceptedCE(eulaStatus);
    });

    assertEquals("EULA must be accepted", thrown.getMessage());
    verify(mockGlobalKeyValueStore, times(0)).setKey(any(NexusKeyValue.class));
  }

  @Test
  void testReacceptEula() {
    NexusKeyValue keyValue = new NexusKeyValue();
    keyValue.setValue(Map.of("accepted", true));
    when(mockGlobalKeyValueStore.getKey(anyString())).thenReturn(Optional.of(keyValue));
    // Accept again while previously in an accepted state
    EulaStatus eulaStatus = new EulaStatus();
    eulaStatus.setAccepted(true);
    eulaStatus.setDisclaimer(EulaStatus.EXPECTED_DISCLAIMER);
    underTest.setEulaAcceptedCE(eulaStatus);
    verify(mockGlobalKeyValueStore, times(0)).setKey(any(NexusKeyValue.class));
  }

  @Test
  void testInvalidDisclaimer() {
    EulaStatus eulaStatus = new EulaStatus();
    eulaStatus.setAccepted(true);
    eulaStatus.setDisclaimer("foo");
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
      underTest.setEulaAcceptedCE(eulaStatus);
    });

    assertEquals("Invalid EULA disclaimer", thrown.getMessage());
    verify(mockGlobalKeyValueStore, times(0)).setKey(any(NexusKeyValue.class));
  }
}
