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
package org.sonatype.nexus.repository.search.index;

import java.util.concurrent.Callable;

import org.sonatype.goodies.common.ComponentSupport;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkProcessor;

/**
 * A Runnable for adding an {@link org.elasticsearch.action.index.IndexRequest}
 * or {@link org.elasticsearch.action.delete.DeleteRequest} to a {@link BulkProcessor}.
 *
 * The intention is for this Runnable to be run on a single dedicated worker thread so that only that thread
 * is contesting for the BulkProcessor's MutEx when {@link BulkProcessor#add(ActionRequest)} is invoked.
 * That worker thread should be the same worker thread running instances of {@link BulkProcessorFlusher}.
 * This is achieved by using a single thread pool in {@link SearchIndexServiceImpl} to run instances of
 * this Runnable which are created in {@link SearchIndexServiceImpl}.
 *
 * @since 3.22
 */
public class BulkProcessorUpdater<T extends ActionRequest<T>>
    extends ComponentSupport
    implements Callable<Void>
{
  private final BulkProcessor bulkProcessor;

  private final T actionRequest;

  public BulkProcessorUpdater(final BulkProcessor bulkProcessor, final T actionRequest) {
    this.bulkProcessor = bulkProcessor;
    this.actionRequest = actionRequest;
  }

  @Override
  public Void call() {
    log.debug("Trying to add index/delete request to batch.");
    this.bulkProcessor.add(actionRequest);
    log.debug("Added index/delete request to batch.");
    return null;
  }
}
