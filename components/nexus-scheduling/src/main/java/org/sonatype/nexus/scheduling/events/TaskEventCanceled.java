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

import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskInfo;

/**
 * Event fired when {@link Cancelable} NX Task is running and has been canceled. Fired in the moment cancellation was
 * applied, the task is probably still running and will stop when it detects request for cancellation. If this event
 * was emitted, the last event sent in this run for the task is {@link TaskEventStoppedCanceled}.
 *
 * @since 2.0
 */
public class TaskEventCanceled
    extends TaskEvent
{
  public TaskEventCanceled(final TaskInfo taskInfo) {
    super(taskInfo);
  }
}
