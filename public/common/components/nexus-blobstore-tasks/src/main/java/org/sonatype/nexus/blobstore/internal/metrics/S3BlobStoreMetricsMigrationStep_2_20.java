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
package org.sonatype.nexus.blobstore.internal.metrics;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Dummy no-op migration step to maintain database migration history consistency.
 *
 * This step was temporarily created as a versioned migration step and executed in some environments
 * before being reverted. To prevent "Missing migrations" errors in those environments, this dummy
 * step is kept in the codebase but performs no operations.
 *
 * Original functionality was moved to the repeatable S3BlobStoreMetricsMigrationStep.
 */
@Component
public class S3BlobStoreMetricsMigrationStep_2_20
    implements DatabaseMigrationStep
{
  @Override
  public Optional<String> version() {
    return Optional.of("2.20");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // No-op: This is a placeholder step to maintain migration history consistency
  }
}
