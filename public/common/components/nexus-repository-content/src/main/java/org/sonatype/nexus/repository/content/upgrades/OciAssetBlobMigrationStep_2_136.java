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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * OCI-specific migration step: Creates index for blob_created on OCI asset_blob table.
 * Drops index for last_updated on OCI asset table.
 *
 * This migration step is specific to OCI format repository support (NEXUS-42721).
 * Adds necessary indexes for efficient OCI asset queries.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OciAssetBlobMigrationStep_2_136
    implements DatabaseMigrationStep
{
  private static final String OCI_FORMAT = "oci";

  private static final String OCI_ASSET_BLOB_TABLE = OCI_FORMAT + "_asset_blob";

  private static final String OCI_ASSET_TABLE = OCI_FORMAT + "_asset";

  private static final String CREATE_INDEX =
      "CREATE INDEX IF NOT EXISTS idx_%s_asset_blob_blob_created ON %s_asset_blob (blob_created)";

  private static final String DROP_INDEX = "DROP INDEX IF EXISTS idx_%s_asset_last_updated";

  @Override
  public Optional<String> version() {
    return Optional.of("2.136");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, OCI_ASSET_BLOB_TABLE)) {
      return;
    }

    try (Statement st = connection.createStatement()) {
      st.execute(String.format(CREATE_INDEX, OCI_FORMAT, OCI_FORMAT));

      if (tableExists(connection, OCI_ASSET_TABLE)) {
        st.execute(String.format(DROP_INDEX, OCI_FORMAT));
      }
    }
  }
}
