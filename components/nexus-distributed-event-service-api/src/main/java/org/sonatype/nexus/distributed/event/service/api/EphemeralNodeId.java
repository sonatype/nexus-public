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
package org.sonatype.nexus.distributed.event.service.api;

import java.util.UUID;

import org.sonatype.nexus.distributed.event.service.api.common.PublisherEvent;

/**
 * Initial/temporary implementation of ephemeral node id.
 *
 * Creators of {@link PublisherEvent} sent to 'DistributedEventPublisher' should obtain the node's id from
 * this class.
 *
 * The Distributed Event Service event loop should use this class to obtain the id for that node.
 *
 * See {@code DistributedEventPublisher}
 * @see PublisherEvent
 * @since 3.38
 */
public final class EphemeralNodeId
{
  public static final String NODE_ID = UUID.randomUUID().toString();

  private EphemeralNodeId() {
    //noop
  }
}
