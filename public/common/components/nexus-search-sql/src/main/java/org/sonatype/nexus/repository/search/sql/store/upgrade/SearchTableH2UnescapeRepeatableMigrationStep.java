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

import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import org.springframework.stereotype.Component;

/**
 * Removes PostgreSQL tsEscape formatting (single quotes) and position markers from H2 search columns.
 * This migration fixes data that was indexed with PostgreSQL TSVECTOR format in H2.
 * Converts tokenized format like '0.1' '0':1 '1':2 to plain format: 0.1
 * 
 * This is a repeatable migration - delete this file after it runs on your environment.
 */
@Component
public class SearchTableH2UnescapeRepeatableMigrationStep
    implements RepeatableDatabaseMigrationStep
{
  private static final String SEARCH_COMPONENTS_TABLE = "search_components";

  @Override
  public Integer getChecksum() {
    return 1; // Constant value - runs once, then delete this file
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (tableExists(connection, SEARCH_COMPONENTS_TABLE) && isH2(connection)) {
      removeQuotesFromFormatFields(connection);
    }
  }

  private void removeQuotesFromFormatFields(final Connection connection) throws Exception {
    // Convert PostgreSQL TSVECTOR format to plain H2 format (single value only, no tokens)
    // PostgreSQL format examples:
    // Simple: 'value'
    // Tokenized: '0.1' '0':1 '1':2
    // We want H2 format (just the first value, no tokens):
    // Simple: value
    // Tokenized: 0.1

    for (int i = 1; i <= 7; i++) {
      String column = "format_field_values_" + i;

      // Extract just the first quoted value (before any space or position marker)
      // Use REGEXP_REPLACE to extract everything up to first space or colon after removing quotes
      runStatement(connection, String.format(
          "UPDATE search_components " +
              "SET %s = LOWER(TRIM(REGEXP_REPLACE(REGEXP_REPLACE(%s, '''([^'']+)''.*', '$1'), '''', ''))) " +
              "WHERE %s IS NOT NULL AND %s != ''",
          column, column, column, column));
    }
  }
}
