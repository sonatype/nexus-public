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

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

/**
 * Search {@link Facet}, that index/de-index component metadata.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface ElasticSearchFacet
    extends SearchIndexFacet
{
  /**
   * Indexes the metadata of the given component, requires an active {@link UnitOfWork}.
   */
  void put(EntityId componentId);

  /**
   * Indexes the metadata of the given components, requires an active {@link UnitOfWork}.
   *
   * @since 3.4
   */
  void bulkPut(Iterable<EntityId> componentIds);

  /**
   * De-indexes the metadata of the given component.
   */
  void delete(EntityId componentId);

  /**
   * De-indexes the metadata of the given components.
   *
   * @since 3.4
   */
  void bulkDelete(Iterable<EntityId> componentIds);
}
