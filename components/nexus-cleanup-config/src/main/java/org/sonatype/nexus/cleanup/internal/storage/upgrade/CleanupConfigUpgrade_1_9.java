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
package org.sonatype.nexus.cleanup.internal.storage.upgrade;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

@Named
@Singleton
public class CleanupConfigUpgrade_1_9
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  @Override
  public Optional<String> version() {
    return Optional.of("1.9");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {

    try (Statement stmt = connection.createStatement()) {
      int updates = stmt.executeUpdate("UPDATE CLEANUP_POLICY " +
          " SET FORMAT='ALL_FORMATS' WHERE FORMAT='*'");
      log.info(
          "Updated {} misconfigured cleanup policies: set format='ALL_FORMATS'",
          updates);
    }
  }
}
