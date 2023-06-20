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
package org.sonatype.nexus.repository.search;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

/**
 * @since 3.38
 */
@Named
@Singleton
public class SearchUtils
    extends ComponentSupport
{
  public static final String CONTINUATION_TOKEN = "continuationToken";

  public static final String SORT_FIELD = "sort";

  public static final String SORT_DIRECTION = "direction";

  private static final String ASSET_PREFIX = "assets.";

  private final RepositoryManagerRESTAdapter repoAdapter;

  private final Map<String, String> searchParams;

  private final Map<String, String> assetSearchParams;

  @Inject
  public SearchUtils(
      final RepositoryManagerRESTAdapter repoAdapter,
      final Map<String, SearchMappings> searchMappings)
  {
    this.repoAdapter = checkNotNull(repoAdapter);
    this.searchParams = checkNotNull(searchMappings).entrySet().stream()
        .flatMap(e -> stream(e.getValue().get().spliterator(), true))
        .collect(toMap(SearchMapping::getAlias, SearchMapping::getAttribute));
    this.assetSearchParams = searchParams.entrySet().stream()
        .filter(e -> e.getValue().startsWith(ASSET_PREFIX))
        .collect(toMap(Entry::getKey, Entry::getValue));
  }

  public Map<String, String> getSearchParameters() {
    return searchParams;
  }

  public Map<String, String> getAssetSearchParameters() {
    return assetSearchParams;
  }

  public Repository getRepository(final String repository) {
    return repoAdapter.getReadableRepository(repository);
  }

  /**
   * Builds a collection of {@link SearchFilter} based on configured search parameters.
   *
   * @param uriInfo {@link UriInfo} to extract query parameters from
   * @return
   */
  public List<SearchFilter> getSearchFilters(final UriInfo uriInfo) {
    return convertParameters(uriInfo, Arrays.asList(CONTINUATION_TOKEN, SORT_FIELD, SORT_DIRECTION));
  }

  private List<SearchFilter> convertParameters(final UriInfo uriInfo, final List<String> keys) {
    return uriInfo.getQueryParameters().entrySet().stream()
        .filter(entry -> !keys.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream()
            .map(value -> {
                String key = searchParams.getOrDefault(entry.getKey(), entry.getKey());
                return new SearchFilter(key, value);
            }))
        .collect(toList());
  }

  public boolean isAssetSearchParam(final String assetSearchParam) {
    return assetSearchParams.containsKey(assetSearchParam) || isFullAssetAttributeName(assetSearchParam);
  }

  public boolean isFullAssetAttributeName(final String assetSearchParam) {
    return assetSearchParam.startsWith(ASSET_PREFIX);
  }

  public String getFullAssetAttributeName(final String key) {
    return isFullAssetAttributeName(key) ? key : getAssetSearchParameters().get(key);
  }
}
