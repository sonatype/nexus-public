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
package org.sonatype.nexus.common.app.softdelete;

/**
 * Single source of truth for the schema versions at which the soft-delete {@code deleted_on}/{@code deleted_by}
 * columns exist.
 *
 * <p>
 * These constants live in {@code nexus-configuration} (the lowest module all soft-delete callers share) precisely
 * because the migration steps that introduce the columns live in {@code nexus-repository-content}, which sits
 * <em>above</em> {@code nexus-repository-services}. Callers in the lower module (e.g.
 * {@code RepositoryDeletionService})
 * therefore cannot reference the migration steps' own {@code VERSION} constants without a circular dependency, so a
 * literal was duplicated. Anchoring both the migration steps and every consumer on these constants keeps the version
 * strings in one place across the module boundary.
 * </p>
 */
public final class SoftDeleteSchemaVersions
{
  private SoftDeleteSchemaVersions() {
    // constants holder
  }

  /**
   * Schema version at which the {@code repository} config table has its {@code deleted_on} column
   * (added by {@code ConfigurationSoftDeleteMigrationStep_2_171}).
   */
  public static final String REPOSITORY = "2.171";

  /**
   * Schema version at which every {@code {format}_asset}/{@code {format}_component} table has its
   * {@code deleted_on}/{@code deleted_by} columns (added by {@code SoftDeleteColumnsMigrationStep_2_172}).
   */
  public static final String CONTENT = "2.172";
}
