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
package org.sonatype.nexus.upgrade.datastore.internal.steps;

import java.sql.Connection;
import java.util.Optional;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Drops all cached metadata in pypi group repositories, will be regenerated on request and stored in proper location
 */
@Named
public class PypiAssetMigrationStep_1_35
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String DROP_ASSETS = "DELETE FROM pypi_asset WHERE repository_id IN(" +
      "SELECT cr.repository_id FROM " +
      "pypi_content_repository cr " +
      "LEFT JOIN repository AS r ON cr.config_repository_id = r.id " +
      "WHERE r.recipe_name = 'pypi-group')";

  @Override
  public Optional<String> version() {
    return Optional.of("1.35");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, "pypi_asset") && tableExists(connection, "pypi_content_repository")) {
      log.info("Removing cached assets in pypi group repositories, regenerated with proper storage path on request.");
      runStatement(connection, DROP_ASSETS);
    }
  }
}
