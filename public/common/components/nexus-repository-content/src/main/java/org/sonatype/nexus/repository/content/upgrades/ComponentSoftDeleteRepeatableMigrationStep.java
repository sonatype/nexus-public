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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Disabled placeholder (NEXUS-54969).
 *
 * <p>
 * This step originally added soft-delete columns ({@code deleted_on}, {@code deleted_by}) and partial
 * indexes to every {@code {format}_component} table (Logical Deletion Recovery, NEXUS-49147/NEXUS-51733).
 * Because the content DAOs read those tables with {@code SELECT *}, running the live, non-transactional
 * {@code ALTER TABLE} while pgjdbc had already server-side-prepared those statements caused
 * {@code cached plan must not change result type} (SQLState {@code 0A000}) failures — a spike of
 * customer-facing 500s during rollout.
 * </p>
 *
 * <p>
 * The body is now a no-op: no columns or indexes are added. This is safe for both database states —
 * an already-migrated database keeps its (now unused, all-NULL) columns, and a never-migrated database
 * never gets them. The store layer ({@code ComponentStore#softDeleteAvailable()}) also returns
 * {@code false}, so no read/write references those columns. The class stays registered and keeps its
 * checksum so Flyway continues to track it. Re-enabling is <em>not</em> merely reverting this class: it also
 * requires re-auditing every {@code SELECT *} / {@code C.*} read on {@code {format}_component} tables —
 * including format-specific DAOs with custom layouts — for result-shape safety.
 * </p>
 */
@Component
public class ComponentSoftDeleteRepeatableMigrationStep
    implements RepeatableDatabaseMigrationStep
{
  private static final Logger log = LoggerFactory.getLogger(ComponentSoftDeleteRepeatableMigrationStep.class);

  /**
   * Bump when the migration body changes in a way that must re-run on already-migrated databases.
   * Left intact so the recorded Flyway checksum is unchanged by this disabling change.
   */
  private static final int LOGIC_VERSION = 2;

  private final List<Format> formats;

  @Autowired
  public ComponentSoftDeleteRepeatableMigrationStep(final List<Format> formats) {
    this.formats = formats;
  }

  @Override
  public Integer getChecksum() {
    int formatsHash = Arrays.hashCode(formats.stream()
        .map(Format::getValue)
        .sorted()
        .toArray());
    return Objects.hash(LOGIC_VERSION, formatsHash);
  }

  @Override
  public void migrate(final Connection connection) {
    // NEXUS-54969: disabled. Do not add deleted_on/deleted_by columns or indexes to {format}_component
    // tables. Idempotent no-op: already-migrated databases keep their (unused, NULL) columns; fresh
    // databases never get them. Prevents the live ALTER TABLE that triggered "cached plan must not change
    // result type" (SQLState 0A000) 500s.
    log.debug("Soft-delete component column migration disabled (NEXUS-54969); skipping");
  }
}
