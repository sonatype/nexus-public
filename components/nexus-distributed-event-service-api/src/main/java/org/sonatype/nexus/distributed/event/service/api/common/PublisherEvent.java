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
package org.sonatype.nexus.distributed.event.service.api.common;

import org.sonatype.nexus.distributed.event.service.api.DistributedEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulates a {@link DistributedEvent} to be published by the DistributedEventPublisher.
 *
 * A PublisherEvent is a notification to the DistributedEventPublisher to publish the enclosed {@link
 * DistributedEvent}.
 *
 * @since 3.38
 */
public final class PublisherEvent
{
  private final String sourceNodeId;

  private final DistributedEvent distributedEvent;

  public PublisherEvent(final String sourceNodeId, final DistributedEvent distributedEvent) {
    this.sourceNodeId = checkNotNull(sourceNodeId);
    this.distributedEvent = checkNotNull(distributedEvent);
  }

  public String getSourceNodeId() {
    return sourceNodeId;
  }

  public DistributedEvent getDistributedEvent() {
    return distributedEvent;
  }
}
