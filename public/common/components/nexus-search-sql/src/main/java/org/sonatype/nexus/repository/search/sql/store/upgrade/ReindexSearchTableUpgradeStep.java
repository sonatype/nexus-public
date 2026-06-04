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

import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.repository.content.search.upgrade.SearchIndexUpgrade;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Triggers the creation of the search index for single node instances after the removal of ElasticSearch.
 */
@Component
public class ReindexSearchTableUpgradeStep
    extends SearchIndexUpgrade
    implements RepeatableDatabaseMigrationStep
{
  private final boolean haEnabled;

  @Autowired
  public ReindexSearchTableUpgradeStep(
      @Value(FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE) final boolean haEnabled)
  {
    this.haEnabled = haEnabled;
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (haEnabled) {
      log.debug("HA enabled no reindex required");
    }
    else {
      super.migrate(connection);
    }
  }

  @Override
  public Integer getChecksum() {
    return 0;
  }
}
