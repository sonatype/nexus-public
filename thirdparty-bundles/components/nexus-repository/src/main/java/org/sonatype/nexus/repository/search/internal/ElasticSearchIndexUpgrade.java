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
package org.sonatype.nexus.repository.search.internal;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.search.IndexSyncService.INDEX_UPGRADE_MARKER;

/**
 * Updates the $data-dir/elasticsearch/nexus.lsn file with a reindex marker string to
 * trigger the IndexSyncService service to update all indexes.
 *
 * @since 3.6.1
 */
@Named
@Singleton
@Upgrades(model = ElasticSearchIndexCheckpoint.MODEL, from = "1.0", to = "1.1")
public class ElasticSearchIndexUpgrade
    extends ComponentSupport
    implements Upgrade
{
  private File nexusLsnFile;

  @Inject
  public ElasticSearchIndexUpgrade(final ApplicationDirectories applicationDirectories) {
    checkNotNull(applicationDirectories);
    this.nexusLsnFile = new File(applicationDirectories.getWorkDirectory("elasticsearch"), "nexus.lsn");
  }

  @Override
  public void apply() throws Exception {
    writeReindexMarker();
  }

  private void writeReindexMarker() throws IOException {
    log.info("Write reindex marker to Elasticsearch marker file");
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(nexusLsnFile))) {
      out.writeBytes(INDEX_UPGRADE_MARKER);
    }
  }
}
