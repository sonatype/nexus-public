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
package org.sonatype.nexus.coreui.internal.blobstore;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;

import static org.sonatype.nexus.blobstore.s3.internal.upgrade.S3FailoverMigrationStep_2_6.S3_FAILOVER_MIGRATION_VERSION;
import static org.sonatype.nexus.common.app.FeatureFlags.CLUSTERED_ZERO_DOWNTIME_ENABLED_NAMED;

/**
 * State contributor to enable regional failover configuration for S3 blob stores.
 * The failover configuration will be available on ZDU if schema version is at least on version 2.6.
 */
@Named
@Singleton
public class S3FailoverStateContributor
    extends ComponentSupport
    implements StateContributor
{
  private final DatabaseCheck databaseCheck;

  private final boolean zduEnabled;

  @Inject
  public S3FailoverStateContributor(
      final DatabaseCheck databaseCheck,
      @Named(CLUSTERED_ZERO_DOWNTIME_ENABLED_NAMED) final boolean zduEnabled)
  {
    this.databaseCheck = databaseCheck;
    this.zduEnabled = zduEnabled;
  }

  @Nullable
  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of("S3FailoverEnabled", isAvailable());
  }

  private boolean isAvailable() {
    return !zduEnabled || databaseCheck.isAtLeast(S3_FAILOVER_MIGRATION_VERSION);
  }
}
