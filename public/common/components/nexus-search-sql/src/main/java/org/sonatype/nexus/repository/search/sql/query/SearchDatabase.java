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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.Optional;

import org.sonatype.nexus.repository.rest.sql.SearchField;

/**
 * Abstraction for details specific to the underlying database used for search.
 */
public interface SearchDatabase
{
  String COMPONENT_PREFIX_ID = "cs.";

  String ASSET_PREFIX_ID = "ap.";

  String ATTRIBUTES = COMPONENT_PREFIX_ID + "attributes";

  /**
   * Return the column to use for sorting by the user specified {@link SearchField} if one exists.
   */
  Optional<String> getSortColumn(final SearchField field);

  /**
   * Format a JSON path expression for sorting on nested JSON attributes.
   * Different databases may have different functions or operators to build the query.
   *
   * @param columnName the column containing JSON (e.g., "cs.attributes")
   * @param sortAlias the sort alias which represents the JSON path inside the column (e.g., "maven2.extension")
   * @return database-specific JSON path expression for use in ORDER BY clauses
   */
  String formatJsonPath(String columnName, String sortAlias);
}
