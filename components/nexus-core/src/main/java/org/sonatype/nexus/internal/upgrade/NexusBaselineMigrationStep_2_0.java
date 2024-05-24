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
package org.sonatype.nexus.internal.upgrade;

import java.sql.Connection;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * No-op baseline version for Zero Downtime Upgrades
 */
@Named
public class NexusBaselineMigrationStep_2_0
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String FAIL_MIGRATION_FLAG = "nexus.zdu.baseline.fail";

  private final boolean shouldFail;

  @Inject
  public NexusBaselineMigrationStep_2_0(@Named("${" + FAIL_MIGRATION_FLAG + ":-false}") final boolean shouldFail) {
    this.shouldFail = shouldFail;
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.0");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (shouldFail) {
      if (log.isDebugEnabled()) {
        log.debug("simulating migration failure due to feature flag '{}'", FAIL_MIGRATION_FLAG);
      }

      throw new IllegalStateException("Unable to migrate");
    }
  }
}
