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
import java.util.Map;

import org.sonatype.nexus.common.entity.EntityId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Requests indexing of one or more components in a particular repository.
 *
 * @since 3.4
 */
class IndexRequest
{
  private static final Logger log = LoggerFactory.getLogger(IndexRequest.class);

  private enum RequestKind
  {
    UPDATE, DELETE
  }

  private final Map<EntityId, RequestKind> componentRequests = new HashMap<>();

  /**
   * Requests an update of the given component's index.
   */
  void update(final EntityId componentId) {
    componentRequests.putIfAbsent(componentId, RequestKind.UPDATE);
  }

  /**
   * Requests deletion of the given component's index.
   */
  void delete(final EntityId componentId) {
    componentRequests.put(componentId, RequestKind.DELETE); // delete wins over update
  }

  /**
   * Applies the index request to the repository's {@link SearchFacet}.
   */
  void apply(final SearchFacet searchFacet) {
    componentRequests.forEach((id, kind) -> {
      switch (kind) {
        case UPDATE:
          searchFacet.put(id);
          break;
        case DELETE:
          searchFacet.delete(id);
          break;
        default:
          log.warn("Unexpected index request {} {}", kind, id);
          break;
      }
    });
  }
}
