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
package org.sonatype.nexus.internal.upgrade;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.common.app.FeatureFlags.ZERO_DOWNTIME_FUTURE_MIGRATION_ENABLED;

@Component
@ConditionalOnProperty(name = ZERO_DOWNTIME_FUTURE_MIGRATION_ENABLED, havingValue = "true")
public class NexusFutureMigrationStep_99_99
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String FAIL_MIGRATION_FLAG = "nexus.zdu.baseline.fail";

  private final boolean shouldFail;

  @Autowired
  public NexusFutureMigrationStep_99_99(
      @Value("${" + FAIL_MIGRATION_FLAG + ":false}") final boolean shouldFail)
  {
    this.shouldFail = shouldFail;
  }

  @Override
  public Optional<String> version() {
    return Optional.of("99.99");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    log.warn("Started step 99.99 test");
    if (shouldFail) {
      if (log.isDebugEnabled()) {
        log.warn("simulating migration failure due to feature flag '{}'", FAIL_MIGRATION_FLAG);
      }
      throw new IllegalStateException("Unable to migrate");
    }
  }
}
