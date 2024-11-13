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
package org.sonatype.nexus.upgrade.internal;

import java.sql.Connection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

/**
 * An upgrade that manages the events table
 *
 * @since 3.38
 */
@Named
@Singleton
public class DistributedEventsUpgrade
    extends ComponentSupport
    implements RepeatableDatabaseMigrationStep
{
  private final boolean clusterEnabled;

  @Inject
  public DistributedEventsUpgrade(@Named("${nexus.datastore.clustered.enabled:-false}") final boolean clusterEnabled) {
    this.clusterEnabled = clusterEnabled;
  }

  @Override
  public Integer getChecksum() {
    return clusterEnabled ? 1 : 0;
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (clusterEnabled) {
      log.debug("Cluster enabled, skipping");
      return;
    }

    log.debug("Dropping distributed event table");
    runStatement(connection, "DROP TABLE IF EXISTS distributed_events");
  }
}
