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
package org.sonatype.nexus.timeline;

import org.sonatype.timeline.TimelineFilter;
import org.sonatype.timeline.TimelineRecord;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * Wrapping predicate into TimelineFilter.
 *
 * @author: cstamas
 * @since 2.0
 */
public class PredicateTimelineFilter
    implements TimelineFilter
{

  private final Predicate<Entry> predicate;

  public PredicateTimelineFilter(final Predicate<Entry> predicate) {
    this.predicate = Preconditions.checkNotNull(predicate);
  }

  @Override
  public boolean accept(final TimelineRecord timelineRecord) {
    return predicate.apply(new TimelineRecordWrapper(timelineRecord));
  }
}
