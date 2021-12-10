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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sonatype.nexus.common.entity.EntityId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Requests indexing of one or more components in a particular repository.
 *
 * @since 3.4
 */
final class IndexRequest
{
  /**
   * All pending deletes as tracked by {@link IndexBatchRequest}.
   */
  private final Set<EntityId> pendingDeletes;

  /**
   * Components that need their index either updated or possibly removed.
   */
  private final Set<EntityId> updatedIds = new HashSet<>();

  IndexRequest(final Set<EntityId> pendingDeletes) {
    this.pendingDeletes = checkNotNull(pendingDeletes);
  }

  /**
   * Marks the given component as needing their index updated or possibly removed.
   */
  void update(final EntityId componentId) {
    updatedIds.add(componentId);
  }

  /**
   * Applies the index request to the repository's {@link ElasticSearchFacet} one-by-one.
   *
   * Has side-effect of removing local deletions from {@link #pendingDeletes}.
   */
  void apply(final ElasticSearchFacet elasticSearchFacet) {
    updatedIds.forEach(id -> {
      if (pendingDeletes.remove(id)) {
        elasticSearchFacet.delete(id);
      }
      else {
        elasticSearchFacet.put(id);
      }
    });
  }

  /**
   * Applies the index request to the repository's {@link ElasticSearchFacet} in bulk.
   *
   * Has side-effect of removing local deletions from {@link #pendingDeletes}.
   */
  void bulkApply(final ElasticSearchFacet elasticSearchFacet) {

    if (!pendingDeletes.isEmpty()) {
      Set<EntityId> deletedIds = new HashSet<>();

      // move ids over from bulk-update to bulk-delete as appropriate
      for (Iterator<EntityId> itr = updatedIds.iterator(); itr.hasNext();) {
        EntityId id = itr.next();
        if (pendingDeletes.remove(id)) {
          deletedIds.add(id);
          itr.remove();
        }
      }

      if (!deletedIds.isEmpty()) {
        elasticSearchFacet.bulkDelete(deletedIds);
      }
    }

    if (!updatedIds.isEmpty()) {
      elasticSearchFacet.bulkPut(updatedIds);
    }
  }
}
