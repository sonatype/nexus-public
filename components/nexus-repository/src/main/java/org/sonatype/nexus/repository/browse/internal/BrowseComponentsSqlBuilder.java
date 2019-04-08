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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that encapsulates building the SQL queries for browsing components in the {@link BrowseServiceImpl}.
 *
 * @since 3.1
 */
@Named
@Singleton
public class BrowseComponentsSqlBuilder
    extends BrowseMetadataNodeSqlBuilderSupport
{
  private final MetadataNodeEntityAdapter<Component> componentEntityAdapter;

  @Inject
  BrowseComponentsSqlBuilder(final ComponentEntityAdapter componentEntityAdapter)
  {
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
  }

  @Override
  protected MetadataNodeEntityAdapter<?> getEntityAdapter() {
    return componentEntityAdapter;
  }

  @Override
  protected String getBrowseIndex() {
    return ComponentEntityAdapter.I_GROUP_NAME_VERSION_INSENSITIVE;
  }

  @Override
  Map<String, Object> buildSqlParams(final String repositoryName, final QueryOptions queryOptions) {
    Map<String, Object> params = new HashMap<>();
    params.put("browsedRepository", repositoryName);

    String filter = queryOptions.getFilter();
    if (filter != null) {
      String filterValue = "%" + filter + "%";
      params.put("nameFilter", filterValue);
      params.put("groupFilter", filterValue);
      params.put("versionFilter", filterValue);
    }

    String lastId = queryOptions.getLastId();
    if (lastId != null) {
      params.put("rid", lastId);
    }

    return params;
  }

  @Override
  protected String buildWhereClause(final List<String> bucketIds, final QueryOptions queryOptions) {
    List<String> whereClauses = new ArrayList<>();
    whereClauses.add(bucketIds.stream()
        .map((bucket) -> MetadataNodeEntityAdapter.P_BUCKET + " = " + bucket)
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
    if (queryOptions.getLastId() != null) {
      whereClauses.add("@rid > :rid");
    }
    return whereClauses.stream().map(clause -> "(" + clause + ")").collect(Collectors.joining(" AND "));
  }
}
