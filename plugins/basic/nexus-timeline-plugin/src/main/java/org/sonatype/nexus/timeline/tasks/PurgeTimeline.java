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
package org.sonatype.nexus.timeline.tasks;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.scheduling.AbstractNexusTask;
import org.sonatype.nexus.timeline.NexusTimeline;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Purge timeline.
 */
@Named(PurgeTimelineTaskDescriptor.ID)
public class PurgeTimeline
    extends AbstractNexusTask<Object>
{
  /**
   * System event action: timeline purge
   */
  public static final String ACTION = "TL_PURGE";

  private final NexusTimeline timeline;

  @Inject
  public PurgeTimeline(final NexusTimeline timeline) {
    this.timeline = checkNotNull(timeline);
  }

  public int getPurgeOlderThan() {
    return Integer.parseInt(getParameters().get(PurgeTimelineTaskDescriptor.OLDER_THAN_FIELD_ID));
  }

  @Override
  protected Object doRun()
      throws Exception
  {
    timeline.purgeOlderThan(getPurgeOlderThan());
    return null;
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    return "Purging Timeline records.";
  }

}
