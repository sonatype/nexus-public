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

import jakarta.inject.Singleton;

import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op upgrade step.
 *
 * Previously dropped distributed_events table when clustering was disabled.
 * Now that distributed_events table is always created (regardless of HA mode),
 * this step is no longer needed.
 *
 * @since 3.38
 */
@Component
@Singleton
public class DistributedEventsUpgrade
    implements RepeatableDatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Integer getChecksum() {
    return 1;
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // No-op: distributed_events table is now always present regardless of HA mode (NEXUS-49804)
  }
}
