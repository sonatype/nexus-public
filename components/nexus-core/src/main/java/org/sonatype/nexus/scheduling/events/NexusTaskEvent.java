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
package org.sonatype.nexus.scheduling.events;

import org.sonatype.nexus.events.AbstractEvent;
import org.sonatype.nexus.scheduling.NexusTask;

/**
 * Abstract super class for task related events.
 *
 * @author cstamas
 * @since 2.0
 */
public abstract class NexusTaskEvent<T>
    extends AbstractEvent<NexusTask<T>>
{
  public NexusTaskEvent(final NexusTask<T> task) {
    super(task);
  }

  /**
   * Returns the newxus task that failed.
   *
   * @return failing nexus task
   */
  public NexusTask<T> getNexusTask() {
    return getEventSender();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "nexusTask=" + getNexusTask() +
        '}';
  }
}
