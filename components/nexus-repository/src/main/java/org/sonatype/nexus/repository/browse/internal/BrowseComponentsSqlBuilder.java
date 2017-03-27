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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that encapsulates building the SQL queries for browsing components in the {@link BrowseServiceImpl}.
 *
 * @since 3.1
 */
public class BrowseComponentsSqlBuilder
{
  private final String repositoryName;

  private final List<Bucket> buckets;

  private final QueryOptions queryOptions;

  public BrowseComponentsSqlBuilder(
      final String repositoryName,
      final List<Bucket> buckets,
      final QueryOptions queryOptions)
  {
    this.repositoryName = checkNotNull(repositoryName);
    this.buckets = checkNotNull(buckets);
    this.queryOptions = checkNotNull(queryOptions);
  }

  /**
   * Returns the SQL for performing the build query.
   */
  public String buildBrowseSql() {
    if (buckets.isEmpty()) {
      return "";
    }

    String querySuffix = buildQuerySuffix();
    String whereClause = buildWhereClause();
    return String.format("SELECT FROM %s WHERE %s %s", AssetEntityAdapter.P_COMPONENT, whereClause, querySuffix);
  }

  /**
   * Returns the SQL parameters for performing the browse query.
   */
  public Map<String, Object> buildSqlParams() {
    Map<String, Object> params = new HashMap<>();
    params.put("browsedRepository", repositoryName);

    String filter = queryOptions.getFilter();
    if (filter != null) {
      String filterValue = "%" + filter + "%";
      params.put("nameFilter", filterValue);
      params.put("groupFilter", filterValue);
      params.put("versionFilter", filterValue);
    }

    return params;
  }

  private String buildWhereClause() {
    List<String> whereClauses = new ArrayList<>();
    whereClauses.add(buckets.stream()
        .map((bucket) -> MetadataNodeEntityAdapter.P_BUCKET + " = " + AttachedEntityHelper.id(bucket))
        .collect(Collectors.joining(" OR ")));
    if (queryOptions.getContentAuth()) {
      whereClauses.add("contentAuth(@this, :browsedRepository) == true");
    }
    if (queryOptions.getFilter() != null) {
      whereClauses.add(
          MetadataNodeEntityAdapter.P_NAME + " LIKE :nameFilter OR " +
          ComponentEntityAdapter.P_GROUP + " LIKE :groupFilter OR " +
          ComponentEntityAdapter.P_VERSION + " LIKE :versionFilter");
    }
    return whereClauses.stream().map(clause -> "(" + clause + ")").collect(Collectors.joining(" AND "));
  }

  private String buildQuerySuffix() {
    String sortDirection = queryOptions.getSortDirection();
    Integer start = queryOptions.getStart();
    Integer limit = queryOptions.getLimit();
    StringBuilder sb = new StringBuilder();
    if (sortDirection != null) {
      sb.append(String.format(" ORDER BY group %1$s, name %1$s, version %1$s", sortDirection));
    }
    if (start != null) {
      sb.append(" SKIP ");
      sb.append(start);
    }
    if (limit != null) {
      sb.append(" LIMIT ");
      sb.append(limit);
    }
    return sb.toString();
  }
}
