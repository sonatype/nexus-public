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
package org.sonatype.nexus.repository.search.sql.store.upgrade;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Adds a composite index on search_components for paged component-version browsing.
 * <p>
 * The index covers (format, namespace, search_component_name, normalised_version) to support
 * the paginated component-versions query introduced in NEXUS-54219. The first three columns
 * filter to a specific component, and the fourth enables Index Scan Backward with early
 * termination for ORDER BY normalised_version DESC LIMIT N queries.
 * <p>
 * Column order is load-bearing, and normalised_version must come last. The query has equality
 * predicates on format, namespace and search_component_name but none on normalised_version,
 * which appears only in ORDER BY / GROUP BY. A B-tree can only seek a contiguous range defined
 * by predicates on its leading columns, so a variant leading with normalised_version has no
 * range to seek and the planner declines to use it at all.
 * <p>
 * Measured on a 2,015,027-row search_components table with a 5,005-version fixture
 * (PostgreSQL 16, first page, warm cache):
 * <ul>
 * <li>pre-existing indexes only: 7.2ms, BitmapAnd over the namespace and component_name
 * text_pattern_ops indexes, 5,005 rows scanned to return 20</li>
 * <li>(format, namespace, search_component_name): 8.9ms, index is used but still scans all
 * 5,005 rows, because it cannot supply the ordering so nothing terminates early</li>
 * <li>this four-column index: 0.12ms, Index Scan Backward, 33 rows scanned</li>
 * <li>(normalised_version, format, namespace, search_component_name): 6.1ms, index unused,
 * planner falls back to the pre-existing bitmap plan</li>
 * </ul>
 * The trailing normalised_version column is what earns the index: it converts cost proportional
 * to the component's total version count into cost proportional to the page size. Deep OFFSET
 * pages (11.2ms to 7.7ms) and COUNT(DISTINCT version) (10.5ms to 6.3ms) improve as well, so no
 * query shape regresses in exchange.
 * <p>
 * The leftmost (format, namespace) prefix also serves NEXUS-54220 queries.
 * <p>
 * This index is only usable while the {@code format}, {@code group} and {@code name} search
 * mappings stay exact-match, which is what makes their predicates plain-column equality
 * ({@code cs.format = ?}) rather than a tsvector match. Switching any of them to lenient matching
 * in {@code DefaultSearchMappings} silently strands this index: results stay correct, nothing is
 * logged, and the paged browse falls back to a bitmap scan over every row of the component.
 * {@code ComponentVersionsSearchTableDAOTestSupport.componentVersionFilterUsesPlainColumnsNotTsvector} guards the
 * assumption, and
 * {@code private/developer-documentation/architecture/data/sql-search.md} explains it.
 */
@Component
public class SearchComponentsVersionBrowseIndexMigrationStep_2_161
    implements DatabaseMigrationStep
{
  private static final String TABLE_NAME = "search_components";

  private static final String INDEX_NAME = "idx_search_components_format_ns_name_version";

  @Override
  public Optional<String> version() {
    return Optional.of("2.161");
  }

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, TABLE_NAME)) {
      return;
    }
    if (indexExists(connection, TABLE_NAME, INDEX_NAME)) {
      return;
    }
    if (isPostgresql(connection)) {
      runStatement(connection, String.format(
          "CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON %s (format, namespace, search_component_name, normalised_version)",
          INDEX_NAME, TABLE_NAME));
    }
    else if (isH2(connection)) {
      runStatement(connection, String.format(
          "CREATE INDEX IF NOT EXISTS %s ON %s (format, namespace, search_component_name, normalised_version)",
          INDEX_NAME, TABLE_NAME));
    }
  }
}
