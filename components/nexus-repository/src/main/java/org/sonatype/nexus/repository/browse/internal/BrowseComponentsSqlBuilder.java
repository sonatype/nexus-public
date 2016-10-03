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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that encapsulates building the SQL queries for browsing components in the {@link BrowseServiceImpl}.
 *
 * @since 3.1
 */
public class BrowseComponentsSqlBuilder
{
  private final boolean group;

  private final List<Bucket> buckets;

  private final QueryOptions queryOptions;

  public BrowseComponentsSqlBuilder(
      final boolean group,
      final List<Bucket> buckets,
      final QueryOptions queryOptions)
  {
    this.group = group;
    this.buckets = checkNotNull(buckets);
    this.queryOptions = checkNotNull(queryOptions);
  }

  /**
   * Returns the SQL for performing the count query.
   */
  public String buildCountSql() {
    String whereClause = buildWhereClause();
    return String.format("SELECT COUNT(DISTINCT(%s)) FROM asset WHERE %s", AssetEntityAdapter.P_COMPONENT, whereClause);
  }

  /**
   * Returns the SQL for performing the build query.
   */
  public String buildBrowseSql() {
    String querySuffix = buildQuerySuffix();
    String whereClause = buildWhereClause();
    return String.format("SELECT DISTINCT(%s) AS %s FROM asset WHERE %s %s", AssetEntityAdapter.P_COMPONENT,
        AssetEntityAdapter.P_COMPONENT, whereClause, querySuffix);
  }

  /**
   * Returns the SQL parameters for performing the browse query.
   */
  public Map<String, Object> buildSqlParams() {
    String filter = queryOptions.getFilter();
    if (filter == null) {
      return Collections.emptyMap();
    }
    String filterValue = "%" + filter + "%";
    return ImmutableMap.<String, Object>builder()
        .put("nameFilter", filterValue)
        .put("groupFilter", filterValue)
        .put("versionFilter", filterValue)
        .build();
  }

  private String buildWhereClause() {
    List<String> whereClauses = new ArrayList<>();
    whereClauses.add("contentAuth(@this) == true");
    whereClauses.add(buckets.stream()
        .map((bucket) -> MetadataNodeEntityAdapter.P_BUCKET + " = " + AttachedEntityHelper.id(bucket))
        .collect(Collectors.joining(" OR ")));
    whereClauses.add(AssetEntityAdapter.P_COMPONENT + " IS NOT NULL");
    if (queryOptions.getFilter() != null) {
      whereClauses.add(
          AssetEntityAdapter.P_COMPONENT + "." + MetadataNodeEntityAdapter.P_NAME + " LIKE :nameFilter OR " +
              AssetEntityAdapter.P_COMPONENT + "." + ComponentEntityAdapter.P_GROUP + " LIKE :groupFilter OR " +
              AssetEntityAdapter.P_COMPONENT + "." + ComponentEntityAdapter.P_VERSION + " LIKE :versionFilter");
    }
    return whereClauses.stream().map(clause -> "(" + clause + ")").collect(Collectors.joining(" AND "));
  }

  private String buildQuerySuffix() {
    String sortProperty = queryOptions.getSortProperty();
    String sortDirection = queryOptions.getSortDirection();
    Integer start = queryOptions.getStart();
    Integer limit = queryOptions.getLimit();
    StringBuilder sb = new StringBuilder();
    if (sortProperty != null && sortDirection != null) {
      if (group) {
        sb.append(String.format(" GROUP BY %s.%s, %s.%s, %s.%s",
            AssetEntityAdapter.P_COMPONENT,
            ComponentEntityAdapter.P_GROUP,
            AssetEntityAdapter.P_COMPONENT,
            MetadataNodeEntityAdapter.P_NAME,
            AssetEntityAdapter.P_COMPONENT,
            ComponentEntityAdapter.P_VERSION));
      }
      sb.append(" ORDER BY ");
      if (group) {
        sb.append(String.format(" %s %s,", MetadataNodeEntityAdapter.P_BUCKET, sortDirection));
      }
      sb.append(String.format("%s.%s %s", AssetEntityAdapter.P_COMPONENT, sortProperty, sortDirection));
      if (ComponentEntityAdapter.P_GROUP.equals(sortProperty)) {
        sb.append(String.format(", %s.%s ASC, %s.%s ASC",
            AssetEntityAdapter.P_COMPONENT,
            MetadataNodeEntityAdapter.P_NAME,
            AssetEntityAdapter.P_COMPONENT,
            ComponentEntityAdapter.P_VERSION));
      }
      else if (MetadataNodeEntityAdapter.P_NAME.equals(sortProperty)) {
        sb.append(String.format(", %s.%s ASC, %s.%s ASC",
            AssetEntityAdapter.P_COMPONENT,
            ComponentEntityAdapter.P_VERSION,
            AssetEntityAdapter.P_COMPONENT,
            ComponentEntityAdapter.P_GROUP));
      }
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
