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
package org.sonatype.nexus.repository.search.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.query.SearchResultComponentGenerator;
import org.sonatype.nexus.repository.search.query.SearchResultsGenerator;
import org.springframework.stereotype.Component;

/**
 * Generates search results consumable by the UI
 *
 * @since 3.14
 */
@Component
public class SearchResultsGeneratorImpl
    implements SearchResultsGenerator
{

  private final Map<String, SearchResultComponentGenerator> searchResultComponentGeneratorMap;

  private final SearchResultComponentGenerator defaultSearchResultComponentGenerator;

  @Autowired
  SearchResultsGeneratorImpl(
      final List<SearchResultComponentGenerator> searchResultComponentGeneratorList)
  {
    this.searchResultComponentGeneratorMap = QualifierUtil.buildQualifierBeanMap(searchResultComponentGeneratorList);
    this.defaultSearchResultComponentGenerator = this.searchResultComponentGeneratorMap
        .get(DefaultSearchResultComponentGenerator.DEFAULT_SEARCH_RESULT_COMPONENT_GENERATOR_KEY);
  }

  @Override
  public List<ComponentSearchResult> getSearchResultList(final SearchResponse response) {
    Set<String> componentIdSet = new HashSet<>();

    return response.getSearchResults()
        .stream()
        .map(component -> {
          String format = component.getFormat();

          SearchResultComponentGenerator generator = searchResultComponentGeneratorMap
              .getOrDefault(format, defaultSearchResultComponentGenerator);

          ComponentSearchResult hit = generator.from(component, componentIdSet);
          if (null != hit) {
            componentIdSet.add(hit.getId());
          }
          return hit;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
