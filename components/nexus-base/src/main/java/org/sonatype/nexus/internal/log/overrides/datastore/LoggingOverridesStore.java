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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

import static org.sonatype.nexus.internal.log.overrides.datastore.LoggerOverridesEvent.Action.CHANGE;
import static org.sonatype.nexus.internal.log.overrides.datastore.LoggerOverridesEvent.Action.RESET;
import static org.sonatype.nexus.internal.log.overrides.datastore.LoggerOverridesEvent.Action.RESET_ALL;

/**
 * Store for accessing the logging-overrides related data
 */
@Named("mybatis")
@Singleton
public class LoggingOverridesStore
    extends ConfigStoreSupport<LoggingOverridesDAO>
{
  @Inject
  protected LoggingOverridesStore(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Transactional
  public void create(final LoggingOverridesData data) {
    dao().createRecord(data);
    postCommitEvent(() -> new LoggerOverridesEvent(data.getName(), data.getLevel(), CHANGE));
  }

  @Transactional
  public Continuation<LoggingOverridesData> readRecords() {
    return dao().readRecords(null);
  }

  @Transactional
  public boolean exists(final String name) {
    return readRecords().stream()
        .anyMatch(level -> level.getName().equals(name));
  }

  @Transactional
  public void update(final LoggingOverridesData data) {
    dao().updateRecord(data);
    postCommitEvent(() -> new LoggerOverridesEvent(data.getName(), data.getLevel(), CHANGE));
  }

  @Transactional
  public void deleteByName(final String name) {
    dao().deleteRecord(name);
    postCommitEvent(() -> new LoggerOverridesEvent(name, null, RESET));
  }

  /**
   * Delete all records
   */
  @Transactional
  public void deleteAllRecords() {
    dao().deleteAllRecords();
    postCommitEvent(() -> new LoggerOverridesEvent(null, null, RESET_ALL));
  }
}
