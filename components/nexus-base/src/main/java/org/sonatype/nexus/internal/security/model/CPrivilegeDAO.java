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
package org.sonatype.nexus.internal.security.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.datastore.api.IdentifiedDataAccess;
import org.sonatype.nexus.security.config.CPrivilege;

import org.apache.ibatis.annotations.Param;

/**
 * {@link CPrivilegeData} access.
 *
 * @since 3.21
 */
public interface CPrivilegeDAO
    extends IdentifiedDataAccess<CPrivilegeData>
{

  /**
   * Retrieve the entity with the given name.
   */
  Optional<CPrivilege> readByName(String name);

  /**
   * Update an entity by its name
   */
  boolean updateByName(CPrivilegeData entity);

  /**
   * Delete an entity with the given name.
   */
  boolean deleteByName(String name);

  /**
   * Find privileges by their ids.
   *
   * @param ids a set of privilege ids.
   * @return a list of {@link CPrivilege}
   */
  List<CPrivilege> findByIds(@Param("ids") Set<String> ids);
}
