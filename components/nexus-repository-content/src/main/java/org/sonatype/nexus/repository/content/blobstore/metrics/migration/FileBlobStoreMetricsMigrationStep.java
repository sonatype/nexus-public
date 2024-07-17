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
package org.sonatype.nexus.repository.content.blobstore.metrics.migration;

import java.sql.Connection;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

/**
 * Blob store metrics migration now live in {@link BlobStoreMetricsMigrationService}
 */
@Named
@Singleton
public class FileBlobStoreMetricsMigrationStep
    extends RepeatableDatabaseMigrationStep
{
  @Override
  public void migrate(final Connection connection) throws Exception {
    // no-op
  }

  @Override
  public Integer getChecksum() {
    return 0;
  }
}
