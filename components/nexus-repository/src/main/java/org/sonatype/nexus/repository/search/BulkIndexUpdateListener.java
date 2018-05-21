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
package org.sonatype.nexus.repository.search;

import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.goodies.common.ComponentSupport;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;

/**
 * {@link BulkProcessor.Listener} that logs.
 */
class BulkIndexUpdateListener
    extends ComponentSupport
    implements BulkProcessor.Listener
{
  private final AtomicInteger inflightRequestCount = new AtomicInteger();

  int inflightRequestCount() {
    return inflightRequestCount.get();
  }

  @Override
  public void beforeBulk(final long executionId, final BulkRequest request) {
    inflightRequestCount.getAndAdd(request.numberOfActions());

    if (log.isDebugEnabled()) {
      log.debug("index update starting, executionId: {}, request count: {}, request size (bytes): {}",
          executionId,
          request.numberOfActions(),
          request.estimatedSizeInBytes()
      );
    }
  }

  @Override
  public void afterBulk(final long executionId, final BulkRequest request, final BulkResponse response) {
    inflightRequestCount.getAndAdd(-request.numberOfActions());

    if (log.isDebugEnabled()) {
      log.debug("index update success, executionId: {}, request count: {}, request size (bytes): {}; " +
              "response took: {}, response hasFailures: {}",
          executionId,
          request.numberOfActions(),
          request.estimatedSizeInBytes(),
          response.getTook(),
          response.hasFailures());
    }
  }

  @Override
  public void afterBulk(final long executionId, final BulkRequest request, final Throwable failure) {
    inflightRequestCount.getAndAdd(-request.numberOfActions());

    log.error("index update failure, executionId: {}, request count: {}, request size (bytes): {}; " +
            "this may indicate that not enough CPU is available to effectively index repository content",
        executionId,
        request.numberOfActions(),
        request.estimatedSizeInBytes(),
        failure);
  }
}
