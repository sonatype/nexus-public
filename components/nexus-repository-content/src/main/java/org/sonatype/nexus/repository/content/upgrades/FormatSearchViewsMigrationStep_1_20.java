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

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Drop legacy format search views.
 */
@Named
public class FormatSearchViewsMigrationStep_1_20
    implements DatabaseMigrationStep
{
  private static final String DROP_FORMAT_SEARCH_VIEW = "DROP VIEW IF EXISTS %s_component_search CASCADE";

  private final List<Format> formats;

  @Inject
  public FormatSearchViewsMigrationStep_1_20(final List<Format> formats) {
    this.formats = formats;
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.20");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    try (Statement st = connection.createStatement()) {
      for (Format format : formats) {
        st.execute(String.format(DROP_FORMAT_SEARCH_VIEW, format.getValue()));
      }
    }
  }
}
