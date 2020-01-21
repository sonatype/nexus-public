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

import java.util.Optional;

import org.sonatype.nexus.common.entity.HasStringId;

/**
 * Generic CRUD {@link DataAccess} that accesses zero to many externally identified entities of the same type.
 *
 * @since 3.21
 */
public interface IdentifiedDataAccess<E extends HasStringId>
    extends DataAccess
{
  /**
   * Browse existing entities.
   */
  Iterable<E> browse();

  /**
   * Create a new entity.
   */
  void create(E entity);

  /**
   * Retrieve the entity with the given id.
   */
  Optional<E> read(String id);

  /**
   * Update an existing entity.
   */
  boolean update(E entity);

  /**
   * Delete the entity with the given id.
   */
  boolean delete(String id);
}
