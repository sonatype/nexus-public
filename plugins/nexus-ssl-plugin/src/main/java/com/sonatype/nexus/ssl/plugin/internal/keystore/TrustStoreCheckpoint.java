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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.Checkpoint;
import org.sonatype.nexus.common.upgrade.Checkpoints;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade checkpoint for the legacy file-based trust store. It exists for the sole purpose of removing the legacy files
 * after their import into the database.
 * 
 * @since 3.1
 */
@Named
@Singleton
@Checkpoints(model = TrustStoreCheckpoint.MODEL, local = true)
public class TrustStoreCheckpoint
    implements Checkpoint
{
  public static final String MODEL = "truststore";

  private final LegacyKeyStoreUpgradeService upgrade;

  @Inject
  public TrustStoreCheckpoint(final LegacyKeyStoreUpgradeService upgrade) {
    this.upgrade = checkNotNull(upgrade);
  }

  @Override
  public void begin(String version) throws Exception {
    // no-op
  }

  @Override
  public void commit() throws Exception {
    // no-op
  }

  @Override
  public void rollback() throws Exception {
    // no-op
  }

  @Override
  public void end() {
    upgrade.deleteKeyStoreFiles();
  }
}
