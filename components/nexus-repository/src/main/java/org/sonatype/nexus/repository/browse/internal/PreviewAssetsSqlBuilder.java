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
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.RepositorySelector;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.internal.AssetWhereClauseBuilder.whereClause;
import static org.sonatype.nexus.repository.browse.internal.SuffixSqlBuilder.buildSuffix;

/**
 * Class that encapsulates building the SQL queries for previewing assets in the {@link BrowseServiceImpl}.
 *
 * @since 3.1
 */
public class PreviewAssetsSqlBuilder
{
  private final RepositorySelector repositorySelector;

  private final String jexlExpression;

  private final QueryOptions queryOptions;

  private final Map<String, List<String>> repoToContainedGroupMap;

  public PreviewAssetsSqlBuilder(final RepositorySelector repositorySelector,
                                 final String jexlExpression,
                                 final QueryOptions queryOptions,
                                 final Map<String, List<String>> repoToContainedGroupMap) {
    this.repositorySelector = checkNotNull(repositorySelector);
    this.jexlExpression = checkNotNull(jexlExpression);
    this.queryOptions = checkNotNull(queryOptions);
    this.repoToContainedGroupMap = checkNotNull(repoToContainedGroupMap);
  }

  public String buildWhereClause() {
    return whereClause("contentExpression(@this, :jexlExpression, :repositorySelector, " +
        ":repoToContainedGroupMap) == true", queryOptions.getFilter() != null);
  }

  public String buildQuerySuffix() {
    return buildSuffix(queryOptions);
  }

  public Map<String, Object> buildSqlParams() {
    Map<String, Object> params = new HashMap<>();
    params.put("repositorySelector", repositorySelector.toSelector());
    params.put("jexlExpression", buildJexlExpression());
    params.put("repoToContainedGroupMap", repoToContainedGroupMap);

    String filter = queryOptions.getFilter();
    if (filter != null) {
      params.put("nameFilter", "%" + filter + "%");
    }
    return params;
  }

  private String buildJexlExpression() {
    //posted question here, http://www.prjhub.com/#/issues/7476 as why we can't just have orients bulit in escaping for double quotes
    return jexlExpression.replace("\"", "'").replaceAll("\\s", " ");
  }
}
