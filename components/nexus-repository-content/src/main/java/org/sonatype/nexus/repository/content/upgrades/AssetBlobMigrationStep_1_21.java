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
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static java.util.Objects.requireNonNull;

/**
 * Adds column added_to_repository on each asset_blob table.
 * Creates index for added_to_repository on each asset_blob table.
 * Drops index for blob_created on asset table.
 */
@Named
public class AssetBlobMigrationStep_1_21
    implements DatabaseMigrationStep
{
  private static final String ADD_COLUMN = "ALTER TABLE %s_asset_blob ADD COLUMN IF NOT EXISTS added_to_repository  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP";

  private static final String CREATE_INDEX = "CREATE INDEX IF NOT EXISTS idx_%s_asset_blob_added_to_repository ON %s_asset_blob (added_to_repository)";

  private static final String DROP_INDEX = "DROP INDEX IF EXISTS idx_%s_asset_blob_blob_created";

  private final List<Format> formats;

  @Inject
  public AssetBlobMigrationStep_1_21(final List<Format> formats)
  {
    this.formats = requireNonNull(formats);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.21");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    try (Statement st = connection.createStatement()) {
      for (Format format : formats) {
        String formatValue = format.getValue();
        st.execute(String.format(ADD_COLUMN, formatValue));
        st.execute(String.format(CREATE_INDEX, formatValue, formatValue));
        st.execute(String.format(DROP_INDEX, formatValue));
      }
    }
  }
}
