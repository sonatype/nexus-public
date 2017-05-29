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

import java.util.List;
import java.util.Map;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import static org.sonatype.nexus.repository.browse.internal.SuffixSqlBuilder.buildSuffix;

/**
 * @since 3.4
 */
public abstract class BrowseSqlBuilderSupport
    extends ComponentSupport
{
  /**
   * Returns the SQL for performing the build query.
   */
  String buildBrowseSql(final List<String> bucketIds, final QueryOptions queryOptions) {
    if (bucketIds.isEmpty()) {
      return "";
    }

    StringBuilder queryBuilder = new StringBuilder("SELECT FROM ");

    if ("id".equals(queryOptions.getSortProperty())) {
      queryBuilder.append(getEntityAdapter().getTypeName());
    }
    else {
      queryBuilder.append("INDEXVALUES");
      if (queryOptions.getSortDirection() != null) {
        queryBuilder.append(queryOptions.getSortDirection());
      }
      queryBuilder.append(":").append(getBrowseIndex());
    }

    queryBuilder.append(" WHERE ").append(buildWhereClause(bucketIds, queryOptions)).append(' ')
        .append(buildQuerySuffix(queryOptions));

    return queryBuilder.toString();
  }

  private String buildQuerySuffix(final QueryOptions queryOptions) {
    return buildSuffix(queryOptions);
  }

  protected abstract MetadataNodeEntityAdapter<?> getEntityAdapter();

  protected abstract String getBrowseIndex();

  protected abstract String buildWhereClause(final List<String> bucketIds, final QueryOptions queryOptions);

  /**
   * Returns the SQL parameters for performing the browse query.
   */
  abstract Map<String, Object> buildSqlParams(final String repositoryName, final QueryOptions queryOptions);
}
