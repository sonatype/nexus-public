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
package org.sonatype.nexus.proxy.maven.routing.internal.task.executor;

import java.util.Set;

import org.sonatype.nexus.proxy.maven.routing.internal.task.CancelableRunnable;

/**
 * Simple statistics for {@link ConstrainedExecutor}.
 *
 * @author cstamas
 * @since 2.4
 */
public class Statistics
{
  private final Set<String> currentlyRunningJobKeys;

  /**
   * Constructor.
   */
  public Statistics(final Set<String> currentlyRunningJobKeys) {
    this.currentlyRunningJobKeys = currentlyRunningJobKeys;
  }

  /**
   * THe set of currently executing (scheduled or running, but not canceled and still running)
   * {@link CancelableRunnable} command keys.
   *
   * @return the job keys that are scheduled or running (without canceled ones).
   */
  public Set<String> getCurrentlyRunningJobKeys() {
    return currentlyRunningJobKeys;
  }
}
