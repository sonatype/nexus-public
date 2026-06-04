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
package org.sonatype.nexus.repository.search.sql.store.upgrade;

import java.sql.Connection;

import org.sonatype.nexus.repository.content.search.upgrade.SearchIndexUpgrade;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Triggers the recreation of the search index for H2 instances due to a table schema change introduced by
 * {@link SearchTablePathsColumnJsonMigrationStep_2_102}.
 */
@Component
public class ReindexH2SearchTableUpgradeStep
    extends SearchIndexUpgrade
    implements RepeatableDatabaseMigrationStep
{
  @Override
  public void migrate(final Connection connection) throws Exception {
    if (isH2(connection)) {
      log.info("Reindexing H2 search tables");
      super.migrate(connection);
    }
    else {
      log.debug("Skipping re-index because instance does not use H2");
    }
  }

  @Override
  public Integer getChecksum() {
    return 0;
  }
}
