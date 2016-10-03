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
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that encapsulates building the SQL queries for previewing assets in the {@link BrowseServiceImpl}.
 *
 * @since 3.1
 */
public class PreviewAssetsSqlBuilder
{
  private final String repositoryName;

  private final String jexlExpression;

  private final List<String> previewRepositories;

  private final QueryOptions queryOptions;

  public PreviewAssetsSqlBuilder(final String repositoryName,
                                 final String jexlExpression,
                                 final List<String> previewRepositories,
                                 final QueryOptions queryOptions) {
    this.repositoryName = checkNotNull(repositoryName);
    this.jexlExpression = checkNotNull(jexlExpression);
    this.previewRepositories = checkNotNull(previewRepositories);
    this.queryOptions = checkNotNull(queryOptions);
  }

  public String buildWhereClause() {
    List<String> whereClauses = new ArrayList<>();
    whereClauses.add("contentAuth(@this) == true");
    whereClauses.add("contentExpression(@this, :jexlExpression, :repositoryName, :repositoriesAsString) == true");
    if (queryOptions.getFilter() != null) {
      whereClauses.add(String.format("%s LIKE :nameFilter", MetadataNodeEntityAdapter.P_NAME));
    }
    return String.join(" AND ", whereClauses);
  }

  public String buildQuerySuffix() {
    String sortProperty = queryOptions.getSortProperty();
    String sortDirection = queryOptions.getSortDirection();
    Integer start = queryOptions.getStart();
    Integer limit = queryOptions.getLimit();
    StringBuilder sb = new StringBuilder();
    if (sortProperty != null && sortDirection != null) {
      sb.append(" ORDER BY ");
      sb.append(sortProperty);
      sb.append(' ');
      sb.append(sortDirection);
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

  public Map<String, Object> buildSqlParams() {
    String filter = queryOptions.getFilter();
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder();
    builder.put("repositoryName", repositoryName)
        .put("jexlExpression", buildJexlExpression())
        .put("repositoriesAsString", buildRepositoriesAsString());
    if (filter != null) {
      builder.put("nameFilter", "%" + filter + "%");
    }
    return builder.build();
  }

  private String buildJexlExpression() {
    //posted question here, http://www.prjhub.com/#/issues/7476 as why we can't just have orients bulit in escaping for double quotes
    return jexlExpression.replaceAll("\"", "'").replaceAll("\\s", " ");
  }

  private String buildRepositoriesAsString() {
    if (previewRepositories.isEmpty()) {
      return "";
    }
    return String.join(",", previewRepositories);
  }
}
