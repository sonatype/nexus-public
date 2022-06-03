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
package org.sonatype.nexus.internal.log.datastore;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

/**
 * Store for accessing the logging-overrides related data
 */
@Named("mybatis")
public class LoggingOverridesStore
    extends ConfigStoreSupport<LoggingOverridesDAO>
{
  @Inject
  protected LoggingOverridesStore(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  /**
   * Create new record
   *
   * @param name  the name of the logger this record is related to
   * @param level log level for this logger, see {@link LoggerLevel}
   */
  @Transactional
  public void createRecord(final String name, final LoggerLevel level) {
    dao().createRecord(name, level.toString());
  }

  /**
   * Return all records stored in DB, the continuationToken to be used when amount more than single page (>1000 rows)
   *
   * @param continuationToken the record id used for pagination
   * @return all records
   */
  @Transactional
  public Continuation<LoggingOverridesData> readRecords(final String continuationToken) {
    return dao().readRecords(continuationToken);
  }

  /**
   * Delete single record by provided 'ID'
   *
   * @param id the record ID
   */
  @Transactional
  public void deleteRecord(final String id) {
    dao().deleteRecord(id);
  }

  /**
   * Delete all records
   */
  @Transactional
  public void deleteAllRecords() {
    dao().deleteAllRecords();
  }
}
