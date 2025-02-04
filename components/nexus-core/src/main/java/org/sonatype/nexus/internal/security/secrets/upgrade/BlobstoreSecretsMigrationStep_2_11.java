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
package org.sonatype.nexus.internal.security.secrets.upgrade;

import java.sql.Connection;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.internal.security.secrets.task.SecretsMigrationTaskDescriptor;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Migration step to migrate session token from existing blobstore configurations
 */
@Named
@Singleton
public class BlobstoreSecretsMigrationStep_2_11
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private final UpgradeTaskScheduler startupScheduler;

  @Inject
  public BlobstoreSecretsMigrationStep_2_11(final UpgradeTaskScheduler startupScheduler) {
    this.startupScheduler = checkNotNull(startupScheduler);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.11");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, "secrets")) {
      log.debug("starting secrets migration task");
      startupScheduler.schedule(
          startupScheduler.createTaskConfigurationInstance(SecretsMigrationTaskDescriptor.TYPE_ID));
    }
  }
}
