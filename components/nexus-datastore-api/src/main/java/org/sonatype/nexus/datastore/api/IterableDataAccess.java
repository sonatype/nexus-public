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
package org.sonatype.nexus.datastore.api;

import org.sonatype.nexus.common.entity.EntityId;

/**
 * Generic CRUD {@link DataAccess} that accesses zero to many entities of the same type.
 *
 * @since 3.next
 */
public interface IterableDataAccess<E, ID extends EntityId>
    extends DataAccess, Iterable<E>
{
  /**
   * Creates a new entity.
   */
  E create(E entity);

  /**
   * Retrieves the entity with the given id.
   */
  E read(ID id);

  /**
   * Updates an existing entity.
   */
  E update(E entity);

  /**
   * Deletes the entity with the given id.
   */
  void delete(ID id);
}
