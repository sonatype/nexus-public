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
package org.sonatype.nexus.orient;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.concurrent.ConcurrentRunner;

import org.junit.Test;

import static java.lang.Boolean.FALSE;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.orient.ReplicationModeOverrides.clearReplicationModeOverrides;
import static org.sonatype.nexus.orient.ReplicationModeOverrides.dontWaitForReplicationResults;
import static org.sonatype.nexus.orient.ReplicationModeOverrides.shouldWaitForReplicationResults;

public class ReplicationModeOverridesTest
    extends TestSupport
{
  @Test
  public void overrideIsPerThread() throws Exception {
    ConcurrentRunner runner = new ConcurrentRunner(1, 60);

    runner.addTask(10, () -> {
      assertThat(shouldWaitForReplicationResults(), is(empty()));
    });

    runner.addTask(10, () -> {
      dontWaitForReplicationResults();
      try {
        assertThat(shouldWaitForReplicationResults(), is(of(FALSE)));
      }
      finally {
        clearReplicationModeOverrides();
      }
    });

    runner.go();

    assertThat(runner.getRunInvocations(), is(runner.getTaskCount() * runner.getIterations()));
  }
}
