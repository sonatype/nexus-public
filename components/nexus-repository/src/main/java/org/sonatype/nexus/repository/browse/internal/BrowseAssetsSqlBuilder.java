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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.repository.browse.QueryOptions;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.internal.AssetWhereClauseBuilder.whereClause;
import static org.sonatype.nexus.repository.browse.internal.SuffixSqlBuilder.buildSuffix;

/**
 * Class that encapsulates building the SQL queries for browsing assets in the {@link BrowseServiceImpl}.
 *
 * @since 3.1
 */
public class BrowseAssetsSqlBuilder
{
  private final QueryOptions queryOptions;
  private final String repositoryName;

  public BrowseAssetsSqlBuilder(final String repositoryName, final QueryOptions queryOptions) {
    this.repositoryName = checkNotNull(repositoryName);
    this.queryOptions = checkNotNull(queryOptions);
  }

  public String buildWhereClause() {
    return whereClause("contentAuth(@this, :browsedRepository) == true", queryOptions.getFilter() != null);
  }

  public String buildQuerySuffix() {
    return buildSuffix(queryOptions);
  }

  public Map<String, Object> buildSqlParams() {
    Map<String, Object> params = new HashMap<>();
    params.put("browsedRepository", repositoryName);
    String filter = queryOptions.getFilter();
    if (filter != null) {
      params.put("nameFilter", "%" + filter + "%");
    }
    return params;
  }
}
