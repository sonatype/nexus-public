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

import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that encapsulates building the SQL queries for browsing assets in the {@link BrowseServiceImpl}.
 *
 * @since 3.1
 */
public class BrowseAssetsSqlBuilder
{
  private final QueryOptions queryOptions;

  public BrowseAssetsSqlBuilder(final QueryOptions queryOptions) {
    this.queryOptions = checkNotNull(queryOptions);
  }

  public String buildWhereClause() {
    List<String> whereClauses = new ArrayList<>();
    whereClauses.add("contentAuth(@this) == true");
    if (queryOptions.getFilter() != null) {
      whereClauses.add(MetadataNodeEntityAdapter.P_NAME + " LIKE :nameFilter");
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
    if (filter == null) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap("nameFilter", "%" + filter + "%");
  }
}
