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
package org.sonatype.nexus.blobstore.s3.internal.upgrade;

import java.sql.Connection;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

@Named
@Singleton
public class S3FailoverMigrationStep_2_6
    implements DatabaseMigrationStep
{
  public static final String S3_FAILOVER_MIGRATION_VERSION = "2.6";

  @Override
  public Optional<String> version() {
    return Optional.of(S3_FAILOVER_MIGRATION_VERSION);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // No-op, this makes the S3 failover region available
  }
}
