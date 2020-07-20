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

import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;

import com.google.common.base.Joiner;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.internal.AssetWhereClauseBuilder.whereClause;

/**
 * Class that encapsulates building the SQL queries for browsing assets in the {@link BrowseServiceImpl}.
 *
 * @since 3.1
 */
@Named
@Singleton
public class BrowseAssetsSqlBuilder
    extends BrowseMetadataNodeSqlBuilderSupport
{
  private final AssetEntityAdapter assetEntityAdapter;

  @Inject
  BrowseAssetsSqlBuilder(final AssetEntityAdapter assetEntityAdapter)
  {
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
  }

  @Override
  protected MetadataNodeEntityAdapter<?> getEntityAdapter() {
    return assetEntityAdapter;
  }

  @Override
  protected String getBrowseIndex() {
    return AssetEntityAdapter.I_NAME_CASEINSENSITIVE;
  }

  @Override
  protected String buildWhereClause(final List<String> bucketIds, final QueryOptions queryOptions) {
    List<String> whereClauses = new ArrayList<>();
    if (!bucketIds.isEmpty()) {
      whereClauses.add("(" + bucketIds.stream()
          .map((bucket) -> MetadataNodeEntityAdapter.P_BUCKET + " = " + bucket)
          .collect(Collectors.joining(" OR ")) + ")");
    }
    if (queryOptions.getContentAuth()) {
      whereClauses.add("contentAuth(@this.name, @this.format, :browsedRepository) == true");
    }

    return whereClause(Joiner.on(" AND ").join(whereClauses), queryOptions.getFilter() != null,
        queryOptions.getLastId() != null);
  }

  @Override
  Map<String, Object> buildSqlParams(final String repositoryName, final QueryOptions queryOptions) {
    Map<String, Object> params = new HashMap<>();
    if (queryOptions.getContentAuth()) {
      params.put("browsedRepository", repositoryName);
    }
    String filter = queryOptions.getFilter();
    if (filter != null) {
      params.put("nameFilter", "%" + filter + "%");
    }

    String lastId = queryOptions.getLastId();
    if (lastId != null) {
      params.put("rid", lastId);
    }
    return params;
  }
}
