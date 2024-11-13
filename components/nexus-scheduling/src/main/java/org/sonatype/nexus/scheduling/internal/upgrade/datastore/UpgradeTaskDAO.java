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
package org.sonatype.nexus.scheduling.internal.upgrade.datastore;

import java.util.Optional;

import org.sonatype.nexus.datastore.api.DataAccess;

import org.apache.ibatis.annotations.Param;

/**
 * Data access for a queue of tasks triggered by migration steps which Nexus needs to complete.
 */
public interface UpgradeTaskDAO
  extends DataAccess
{
  int deleteByTaskId(String taskId);

  /**
   * Browse existing entities.
   */
  Iterable<UpgradeTaskData> browse();

  /**
   * Create a new entity.
   */
  int create(UpgradeTaskData entity);

  /**
   * Retrieve the entity with the given id.
   */
  Optional<UpgradeTaskData> read(int id);

  /**
   * Update an existing entity.
   *
   */
  boolean update(UpgradeTaskData entity);

  /**
   * Updates the status of an upgrade task
   *
   * @param taskId the id of the task
   * @param status the status message
   * @return the number of rows modified
   */
  int setStatus(@Param("taskId") String taskId, @Param("status") String status);

  /**
   * Find the next upgrade task as ordered
   */
  Optional<UpgradeTaskData> next();
}
