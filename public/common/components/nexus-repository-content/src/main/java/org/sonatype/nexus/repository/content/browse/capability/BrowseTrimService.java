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
package org.sonatype.nexus.repository.content.browse.capability;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Singleton;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.db.DatabaseCheck;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
@Singleton
public class BrowseTrimService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final AtomicBoolean postgresqlTrimEnabled = new AtomicBoolean(false);

  private final AtomicBoolean batchTrimEnabled = new AtomicBoolean(false);

  private final DatabaseCheck databaseCheck;

  @Autowired
  public BrowseTrimService(final DatabaseCheck databaseCheck) {
    this.databaseCheck = checkNotNull(databaseCheck);
  }

  public boolean isPostgresqlTrimEnabled() {
    return postgresqlTrimEnabled.get();
  }

  public void setPostgresqlTrimEnabled(final boolean enabled) {
    log.info("PostgreSQL browse trim setting changed to: {}", enabled);
    postgresqlTrimEnabled.set(enabled);
  }

  public boolean isBatchTrimEnabled() {
    return batchTrimEnabled.get();
  }

  public void setBatchTrimEnabled(final boolean enabled) {
    log.info("Batch trim processing setting changed to: {}", enabled);
    batchTrimEnabled.set(enabled);
  }

  /**
   * Determines if trim operations should be allowed based on database type and capability settings.
   *
   * @return true if trim operations should proceed, false otherwise
   */
  public boolean shouldAllowTrim() {
    if (databaseCheck.isPostgresql() && !postgresqlTrimEnabled.get()) {
      log.debug("Trim operations disabled for PostgreSQL (capability not enabled)");
      return false;
    }
    return true;
  }
}
