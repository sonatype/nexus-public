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
package org.sonatype.nexus.internal.log.overrides.datastore;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataAccess;

import org.apache.ibatis.annotations.Param;

/**
 * {@link LoggingOverridesData} DataAccess
 */
public interface LoggingOverridesDAO
    extends DataAccess
{
  /**
   * Create new record
   *
   * @param data represents logger configuration
   */
  void createRecord(LoggingOverridesData data);

  /**
   * Return all records stored in DB, the continuationToken to be used when amount more than single page (>1000 rows)
   *
   * @param continuationToken the record id used for pagination
   * @return all records
   */
  Continuation<LoggingOverridesData> readRecords(@Nullable @Param("continuationToken") String continuationToken);

  /**
   * Delete single record by provided 'NAME'
   *
   * @param name the record ID
   */
  void deleteRecord(@Param("name") String name);

  /**
   * Delete all records
   */
  void deleteAllRecords();

  /**
   * Update record by name
   *
   * @param data represents logger configuration
   */
  void updateRecord(LoggingOverridesData data);
}
