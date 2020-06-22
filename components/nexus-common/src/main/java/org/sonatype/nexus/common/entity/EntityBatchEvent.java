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
package org.sonatype.nexus.common.entity;

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.common.event.HasAffinity;

/**
 * Batched sequence of {@link EntityEvent}s.
 *
 * @since 3.1
 */
public class EntityBatchEvent
    implements HasAffinity
{
  /**
   * Marker for {@link EntityEvent}s which can be batched together in a {@link EntityBatchEvent} as well
   * as being sent individually. Useful if subscribers want to receive a group of events at the same time.
   */
  public interface Batchable
  {
    // empty
  }

  private final List<EntityEvent> events;

  public EntityBatchEvent(final List<EntityEvent> events) {
    this.events = Collections.unmodifiableList(events);
  }

  public List<EntityEvent> getEvents() {
    return events;
  }

  @Override
  public String getAffinity() {
    return events.get(0).getAffinity(); // first event in the batch declares the affinity for the rest
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "events=" + events +
        '}';
  }
}
