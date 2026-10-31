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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.springframework.stereotype.Component;

/**
 * This is a placeholder for a removed migration step.
 *
 * The original step was introduced by the Logical Deletion Recovery initiative (NEXUS-51733) and shipped
 * to dev/staging tenants, recording a {@code flyway_schema_history} row. After the feature was reverted,
 * the step had to remain present as a no-op so Flyway can still match the recorded row to a Java migration
 * and does not treat the schema as being from a later version of Nexus Repository.
 */
@Component
public class ConfigurationSoftDeleteMigrationStep_2_147
    implements DatabaseMigrationStep
{
  @Override
  public Optional<String> version() {
    return Optional.of("2.147");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // noop
  }
}
