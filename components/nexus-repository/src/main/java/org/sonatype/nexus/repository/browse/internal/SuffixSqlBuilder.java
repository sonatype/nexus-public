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
package org.sonatype.nexus.repository.browse.internal;

import org.sonatype.nexus.repository.browse.QueryOptions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builds SQL for sorting, limiting and setting the start record.
 *
 * @since 3.3
 */
public class SuffixSqlBuilder
{
  private final QueryOptions queryOptions;

  private SuffixSqlBuilder(final QueryOptions queryOptions) {
    this.queryOptions = checkNotNull(queryOptions);
  }

  public static String buildSuffix(final QueryOptions queryOptions) {
    return new SuffixSqlBuilder(queryOptions).build();
  }

  private String build() {
    return sort() + start() + limit();
  }

  private String sort() {
    String sortProperty = queryOptions.getSortProperty();
    String sortDirection = queryOptions.getSortDirection();
    if (sortProperty != null && sortDirection != null && "id".equals(sortProperty)) {
      return " ORDER BY @rid " + sortDirection;
    }
    return "";
  }

  private String start() {
    Integer start = queryOptions.getStart();
    if (start != null) {
      return " SKIP " + start;
    }
    return "";
  }

  private String limit() {
    Integer limit = queryOptions.getLimit();
    if (limit != null) {
      return " LIMIT " + limit;
    }
    return "";
  }
}
