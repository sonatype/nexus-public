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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityBatchEvent;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent;
import org.sonatype.nexus.repository.storage.ComponentEvent;

/**
 * Requests indexing of one or more components across one or more repositories.
 *
 * @since 3.4
 */
public final class IndexBatchRequest
{
  /**
   * Repository-scoped index requests.
   */
  private final Map<String, IndexRequest> requests = new HashMap<>();

  /**
   * Tracks components that were deleted; this may include components also tracked under {@link IndexRequest}s.
   */
  private final Set<EntityId> pendingDeletes = new HashSet<>();

  /**
   * Populates index requests based on the given event.
   */
  public IndexBatchRequest(final EntityBatchEvent batchEvent) {
    for (final EntityEvent event : batchEvent.getEvents()) {
      if (event instanceof ComponentEvent) {
        consume((ComponentEvent) event);
      }
      else if (event instanceof AssetEvent) {
        consume((AssetEvent) event);
      }
    }
  }

  /**
   * Creates a blank index request for manual building.
   */
  public IndexBatchRequest() {
    // no-op
  }

  /**
   * Requests an update of the given component's index under the given repository.
   */
  public void update(final String repositoryName, final EntityId componentId) {
    request(repositoryName).update(componentId);
  }

  /**
   * Requests a delete of the given component's index under the given repository (if known).
   *
   * If the repository is unknown then we'll query Elasticsearch later on to find the index.
   */
  public void delete(@Nullable final String repositoryName, final EntityId componentId) {
    if (repositoryName != null) {
      request(repositoryName).update(componentId); // scope delete under specific repository
    }
    pendingDeletes.add(componentId);
  }

  /**
   * Applies the index requests to the given consumer and returns any remaining pending deletes.
   *
   * These deletes aren't directly associated with a repository and require a delete-by-query step.
   */
  Set<EntityId> apply(final BiConsumer<String, IndexRequest> consumer) {
    requests.forEach(consumer);
    return pendingDeletes;
  }

  /**
   * Marks the component's index as needing an update or deletion.
   */
  private void consume(final ComponentEvent event) {
    if (event instanceof ComponentDeletedEvent) {
      delete(event.getRepositoryName(), event.getComponentId());
    }
    else {
      update(event.getRepositoryName(), event.getComponentId());
    }
  }

  /**
   * Marks the owning component's index as needing an update.
   */
  private void consume(final AssetEvent event) {
    if (event.getComponentId() != null) {
      update(event.getRepositoryName(), event.getComponentId());
    }
  }

  /**
   * Returns an {@link IndexRequest} for the given repository.
   */
  private IndexRequest request(final String repositoryName) {
    IndexRequest request = requests.get(repositoryName);
    if (request == null) {
      request = new IndexRequest(pendingDeletes);
      requests.put(repositoryName, request);
    }
    return request;
  }
}
