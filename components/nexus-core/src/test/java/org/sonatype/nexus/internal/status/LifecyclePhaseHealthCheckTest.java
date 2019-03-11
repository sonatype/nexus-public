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
package org.sonatype.nexus.internal.status;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class LifecyclePhaseHealthCheckTest
    extends TestSupport
{
  @Mock
  ManagedLifecycleManager lifecycleManager;

  @Test
  public void TasksPhaseYieldsHealthy() {
    when(lifecycleManager.getCurrentPhase()).thenReturn(Phase.TASKS);
    LifecyclePhaseHealthCheck phaseHealthCheck = new LifecyclePhaseHealthCheck(lifecycleManager);

    assertThat(phaseHealthCheck.check().isHealthy(), is(true));
  }

  @Test
  public void StoragePhaseYieldsUnhealthy() {
    when(lifecycleManager.getCurrentPhase()).thenReturn(Phase.STORAGE);
    LifecyclePhaseHealthCheck phaseHealthCheck = new LifecyclePhaseHealthCheck(lifecycleManager);

    assertThat(phaseHealthCheck.check().isHealthy(), is(false));
  }
}
