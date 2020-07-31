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
package org.sonatype.nexus.repository.content.search;

import java.util.Collection;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;

/**
 * Search {@link Facet}, that indexes/de-indexes component metadata.
 *
 * @since 3.25
 */
@Facet.Exposed
public interface SearchFacet
    extends SearchIndexFacet
{
  /**
   * Adds the given components to the search index.
   *
   * @param componentIds the components to index
   */
  void index(Collection<EntityId> componentIds);

  /**
   * Removes the given components from the search index.
   *
   * @param componentIds the components to purge
   */
  void purge(Collection<EntityId> componentIds);
}
