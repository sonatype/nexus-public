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
package org.sonatype.timeline.internal;

import java.io.IOException;

import org.sonatype.timeline.TimelineCallback;
import org.sonatype.timeline.TimelineRecord;

public class RepairBatch
    implements TimelineCallback
{
  private final DefaultTimelineIndexer indexer;

  public RepairBatch(final DefaultTimelineIndexer indexer) {
    this.indexer = indexer;
  }

  @Override
  public boolean processNext(TimelineRecord rec)
      throws IOException
  {
    indexer.addBatch(rec);
    return true;
  }

  public void finish()
      throws IOException
  {
    indexer.finishBatch();
  }
}
