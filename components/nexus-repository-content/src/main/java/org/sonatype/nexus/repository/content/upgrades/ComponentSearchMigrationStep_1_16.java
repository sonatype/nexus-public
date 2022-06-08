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
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Creates 3 custom fields to store format specific attributes and their indexes.
 */
@Named
public class ComponentSearchMigrationStep_1_16
    implements DatabaseMigrationStep
{
  private static final String MIGRATE = "" +
      "ALTER TABLE component_search ADD COLUMN IF NOT EXISTS format_field_1 VARCHAR;" +
      "ALTER TABLE component_search ADD COLUMN IF NOT EXISTS format_field_2 VARCHAR;" +
      "ALTER TABLE component_search ADD COLUMN IF NOT EXISTS format_field_3 VARCHAR;" +
      "CREATE INDEX IF NOT EXISTS idx_component_search_format_field_1 ON component_search (format_field_1);" +
      "CREATE INDEX IF NOT EXISTS idx_component_search_format_field_2 ON component_search (format_field_2);" +
      "CREATE INDEX IF NOT EXISTS idx_component_search_format_field_3 ON component_search (format_field_3);";

  @Override
  public Optional<String> version() {
    return Optional.of("1.16");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    try (Statement st = connection.createStatement()) {
      st.execute(MIGRATE);
    }
  }
}
