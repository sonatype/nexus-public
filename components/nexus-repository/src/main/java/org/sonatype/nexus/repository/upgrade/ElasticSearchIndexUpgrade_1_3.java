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
package org.sonatype.nexus.repository.upgrade;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.repository.search.ElasticSearchIndexCheckpoint;

/**
 * Updates the $data-dir/elasticsearch/nexus.lsn file with a reindex marker string to
 * trigger the IndexSyncService service to update all indexes.
 *
 * @since 3.16
 */
@Named
@Singleton
@Upgrades(model = ElasticSearchIndexCheckpoint.MODEL, from = "1.2", to = "1.3")
public class ElasticSearchIndexUpgrade_1_3
    extends ElasticSearchIndexUpgradeSupport
{
  @Inject
  public ElasticSearchIndexUpgrade_1_3(final ApplicationDirectories applicationDirectories) {
    super(applicationDirectories);
  }
}
