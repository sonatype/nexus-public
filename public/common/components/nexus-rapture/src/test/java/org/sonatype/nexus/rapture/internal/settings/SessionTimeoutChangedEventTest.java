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
package org.sonatype.nexus.rapture.internal.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SessionTimeoutChangedEvent}.
 */
class SessionTimeoutChangedEventTest
{
  @Test
  void testIsLocal_whenRemoteNodeIdIsNull() {
    SessionTimeoutChangedEvent event = new SessionTimeoutChangedEvent(120);
    assertTrue(event.isLocal(), "Event should be local when remoteNodeId is null");
  }

  @Test
  void testIsLocal_whenRemoteNodeIdIsSet() {
    SessionTimeoutChangedEvent event = new SessionTimeoutChangedEvent(120);
    event.setRemoteNodeId("node-2");
    assertFalse(event.isLocal(), "Event should not be local when remoteNodeId is set");
  }
}
