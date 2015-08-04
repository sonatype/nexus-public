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
package org.sonatype.nexus.client.core.subsystem;

import java.util.Collection;

/**
 * A nexus {@link Entity} repository.
 *
 * @since 2.3
 */
public interface EntityRepository<E extends Entity<E>>
{

  /**
   * Creates a new entity with specified id.
   *
   * @param id of new entity
   * @return created entity (never null)
   */
  E create(String id);

  /**
   * Retrieves an existing entity by id.
   *
   * @param id of entity to be retrieved (cannot be null)
   * @return entity with specified id
   */
  E get(String id);

  /**
   * Retrieves all existing entities.
   *
   * @return all existing entities (never null)
   */
  Collection<E> get();

}
