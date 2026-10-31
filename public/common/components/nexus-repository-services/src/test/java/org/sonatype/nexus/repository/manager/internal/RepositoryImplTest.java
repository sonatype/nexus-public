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
package org.sonatype.nexus.repository.manager.internal;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.DELETED;
import static org.sonatype.nexus.repository.manager.internal.RepositoryImpl.State.STOPPED;

/**
 * Lifecycle robustness tests for {@link RepositoryImpl} under HA immediate consistency (NEXUS-54229).
 *
 * A repository create/update/delete issued by a client can be load-balanced across HA nodes, so the node
 * that handles an originating {@code DELETE} may still be draining the peer node's replicated create/update
 * events for the same repository. A replicated update runs {@code stop() -> update() -> start()}, leaving the
 * repository transiently STOPPED (or freshly re-STARTED). The delete path must tolerate that instead of
 * throwing {@link org.sonatype.nexus.common.stateguard.InvalidStateException} -- which previously surfaced as an
 * HTTP 500 during test teardown. Mirrors the guard already used by {@code destroy()} and the NEXUS-53816 fix.
 */
class RepositoryImplTest
{
  private RepositoryImpl underTest;

  @BeforeEach
  void setUp() throws Exception {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRepositoryName()).thenReturn("test-repo");

    underTest = new RepositoryImpl(mock(EventManager.class), new HostedType(), new Format("test-format")
    {
    });
    underTest.init(configuration);
    underTest.start();
  }

  /**
   * {@code stopSafe()} must be idempotent: calling it on an already-STOPPED repository is a no-op, not an
   * {@code InvalidStateException} from {@code stop()}'s {@code @Transitions(from = STARTED)}. Before the fix
   * the second call threw "Invalid state: STOPPED; allowed: [STARTED]" -- exactly the error seen when a
   * load-balanced REST delete raced a peer node's in-flight replicated update.
   */
  @Test
  void stopSafeIsIdempotentWhenAlreadyStopped() throws Exception {
    underTest.stopSafe();
    assertEquals(STOPPED, underTest.getStateGuard().getCurrent());

    // Second stop must not blow up now that the repository is already STOPPED.
    assertDoesNotThrow(() -> underTest.stopSafe());
    assertEquals(STOPPED, underTest.getStateGuard().getCurrent());
  }

  /**
   * {@code delete()} must stop a still-STARTED repository itself, atomically under its own write lock, rather
   * than requiring the caller to have already stopped it. A concurrent replicated update can flip the
   * repository back to STARTED between the manager's {@code stopSafe()} and {@code delete()}; before the fix
   * {@code delete()}'s {@code @Transitions(from = STOPPED)} then threw and failed the delete.
   */
  @Test
  void deleteStopsAStillStartedRepository() throws Exception {
    // Intentionally NOT calling stopSafe() first: the repository is STARTED, as it would be after a racing
    // replicated update re-started it just before this delete ran.
    assertDoesNotThrow(() -> underTest.delete());
    assertEquals(DELETED, underTest.getStateGuard().getCurrent());
  }
}
